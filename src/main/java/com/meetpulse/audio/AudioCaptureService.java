package com.meetpulse.audio;

import com.meetpulse.model.EnergyFrame;
import com.meetpulse.processing.EnergyCalculator;
import com.meetpulse.processing.SilenceDetector;
import com.meetpulse.service.MeetingAnalyzer;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class AudioCaptureService {

    // ── audio config ──────────────────────────────────────────────────────
    private static final float SAMPLE_RATE = 44100f;
    private static final int SAMPLE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final int BUFFER_BYTES = 4096;
    private static final long CALIBRATION_MS = 3500;
    private static final double EMA_ALPHA = 0.24;
    private static final double NOISE_TRACK_ALPHA = 0.015;

    // ── state ─────────────────────────────────────────────────────────────
    public enum Phase { IDLE, CALIBRATING, RECORDING, STOPPED }

    private volatile Phase phase = Phase.IDLE;
    private volatile boolean stopFlag = false;

    private double threshold = 800.0;
    private double noiseFloor = 0.0;
    private double liveRawRms = 0.0;
    private double liveSmoothedRms = 0.0;

    // ── components ────────────────────────────────────────────────────────
    private TargetDataLine line;
    private final EnergyCalculator calc = new EnergyCalculator();
    private SilenceDetector detector = new SilenceDetector(threshold);
    private final MeetingAnalyzer analyzer = new MeetingAnalyzer();

    // ── callbacks (all called from the audio thread) ─────────────────────
    /** Called every frame: (rms, isSilent, phase) */
    private TriConsumer<Double, Boolean, Phase> onFrame;
    /** Called for log messages */
    private Consumer<String> onLog;
    /** Called when phase changes */
    private Consumer<Phase> onPhaseChange;

    @FunctionalInterface
    public interface TriConsumer<A, B, C> { void accept(A a, B b, C c); }

    public void setOnFrame(TriConsumer<Double, Boolean, Phase> cb) { this.onFrame = cb; }
    public void setOnLog(Consumer<String> cb) { this.onLog = cb; }
    public void setOnPhaseChange(Consumer<Phase> cb) { this.onPhaseChange = cb; }

    // ── main entry ────────────────────────────────────────────────────────
    public void start() {
        stopFlag = false;
        analyzer.reset();

        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_BITS, CHANNELS, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                log("ERROR: Microphone not supported on this system.");
                return;
            }

            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format, BUFFER_BYTES * 4);
            line.start();

            runCalibration();
            if (!stopFlag) runRecording();

        } catch (LineUnavailableException e) {
            log("ERROR: Microphone unavailable — " + e.getMessage());
        } catch (Exception e) {
            log("ERROR: " + e.getMessage());
        } finally {
            closeHardware();
            if (phase != Phase.STOPPED) setPhase(Phase.STOPPED);
        }
    }

    // ── calibration ───────────────────────────────────────────────────────
    private void runCalibration() {
        setPhase(Phase.CALIBRATING);
        log("Calibrating — please stay silent for ~3s...");

        byte[] buf = new byte[BUFFER_BYTES];
        List<Double> samples = new ArrayList<>();
        long deadline = System.currentTimeMillis() + CALIBRATION_MS;
        double ema = 0.0;

        while (!stopFlag && System.currentTimeMillis() < deadline) {
            int read = line.read(buf, 0, buf.length);
            if (read <= 0) continue;

            double raw = calc.calculateRms(buf, read);
            if (ema == 0.0) ema = raw;
            ema = (EMA_ALPHA * raw) + ((1.0 - EMA_ALPHA) * ema);

            liveRawRms = raw;
            liveSmoothedRms = ema;
            samples.add(ema);

            if (onFrame != null) onFrame.accept(ema, true, Phase.CALIBRATING);
        }

        if (!samples.isEmpty()) {
            double p20 = percentile(samples, 0.20);
            double p75 = percentile(samples, 0.75);
            double med = median(samples);
            noiseFloor = Math.max(40.0, p20 > 0 ? p20 : med);
            threshold = computeAdaptiveThreshold(noiseFloor, p75);
            detector = new SilenceDetector(threshold, threshold * 0.82);
            detector.reset(true);

            log(String.format("Calibration done — floor %.0f, p75 %.0f, threshold %.0f",
                    noiseFloor, p75, threshold));
        } else {
            // Fallback if calibration yielded nothing.
            noiseFloor = 300.0;
            threshold = 800.0;
            detector = new SilenceDetector(threshold, threshold * 0.82);
            detector.reset(true);
            log("WARN: Calibration data unavailable, using default threshold.");
        }
    }

    // ── recording ─────────────────────────────────────────────────────────
    private void runRecording() {
        setPhase(Phase.RECORDING);
        log("Recording started — signal analysis active.");

        byte[] buf = new byte[BUFFER_BYTES];
        String lastState = "";
        double ema = liveSmoothedRms > 0 ? liveSmoothedRms : 0.0;

        while (!stopFlag) {
            int read = line.read(buf, 0, buf.length);
            if (read <= 0) continue;

            double raw = calc.calculateRms(buf, read);
            if (ema == 0.0) ema = raw;
            ema = (EMA_ALPHA * raw) + ((1.0 - EMA_ALPHA) * ema);

            liveRawRms = raw;
            liveSmoothedRms = ema;

            // Slowly track noise floor only when likely silent to avoid chasing speech.
            if (ema < threshold * 0.9) {
                if (noiseFloor <= 0) noiseFloor = ema;
                noiseFloor = ((1.0 - NOISE_TRACK_ALPHA) * noiseFloor) + (NOISE_TRACK_ALPHA * ema);
                threshold = computeAdaptiveThreshold(noiseFloor, threshold);
                detector.setThresholds(threshold, threshold * 0.82);
            }

            boolean silent = detector.isSilent(ema);
            analyzer.addFrame(new EnergyFrame(System.currentTimeMillis(), ema, silent));

            if (onFrame != null) onFrame.accept(ema, silent, Phase.RECORDING);

            String state = silent ? "SILENT" : "SPEAKING";
            if (!state.equals(lastState)) {
                log(String.format("%s  raw: %.0f  smooth: %.0f  thr: %.0f",
                        silent ? "🔇 Silence" : "🎤 Speaking", raw, ema, threshold));
                lastState = state;
            }
        }
    }

    // ── stop/reset ────────────────────────────────────────────────────────
    public void stop() {
        stopFlag = true;
    }

    public void reset() {
        stopFlag = true;
        closeHardware();
        analyzer.reset();
        threshold = 800.0;
        noiseFloor = 0.0;
        liveRawRms = 0.0;
        liveSmoothedRms = 0.0;
        detector = new SilenceDetector(threshold, threshold * 0.82);
        detector.reset(true);
        setPhase(Phase.IDLE);
    }

    // ── accessors ─────────────────────────────────────────────────────────
    public Phase getPhase() { return phase; }
    public double getThreshold() { return threshold; }
    public double getNoiseFloor() { return noiseFloor; }
    public double getLiveRawRms() { return liveRawRms; }
    public double getLiveSmoothedRms() { return liveSmoothedRms; }
    public MeetingAnalyzer getAnalyzer() { return analyzer; }

    /** Safe live snapshot — never mutates segment state */
    public double getLiveSpeakingPct() { return analyzer.getLiveSpeakingPct(); }
    public int getLiveSegmentCount() { return analyzer.getLiveSegmentCount(); }
    public double getLivePeakRms() { return analyzer.getLivePeakRms(); }
    public int getLiveFrameCount() { return analyzer.getLiveFrameCount(); }

    // ── helpers ───────────────────────────────────────────────────────────
    private void closeHardware() {
        if (line != null && line.isOpen()) {
            line.stop();
            line.close();
        }
    }

    private double median(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if ((n & 1) == 1) return sorted.get(n / 2);
        return (sorted.get((n / 2) - 1) + sorted.get(n / 2)) / 2.0;
    }

    private double percentile(List<Double> values, double q) {
        if (values.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        double pos = q * (sorted.size() - 1);
        int lo = (int) Math.floor(pos);
        int hi = (int) Math.ceil(pos);
        if (lo == hi) return sorted.get(lo);
        double w = pos - lo;
        return sorted.get(lo) * (1.0 - w) + sorted.get(hi) * w;
    }

    private double medianAbsoluteDeviation(List<Double> values, double med) {
        List<Double> dev = new ArrayList<>(values.size());
        for (double v : values) dev.add(Math.abs(v - med));
        return median(dev);
    }

    private double clamp(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    private double computeAdaptiveThreshold(double floor, double anchor) {
        // Keep threshold above floor but avoid exploding under transient spikes.
        double base = floor + 120.0;
        double ratio = floor * 2.25;
        double guided = Math.max(base, ratio);
        if (anchor > 0) guided = Math.max(guided, anchor * 0.92);
        return clamp(guided, 120.0, 12000.0);
    }

    private void setPhase(Phase p) {
        phase = p;
        if (onPhaseChange != null) onPhaseChange.accept(p);
    }

    private void log(String msg) {
        if (onLog != null) onLog.accept(msg);
    }
}
