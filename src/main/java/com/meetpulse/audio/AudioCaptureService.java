package com.meetpulse.audio;

import com.meetpulse.model.EnergyFrame;
import com.meetpulse.processing.EnergyCalculator;
import com.meetpulse.processing.SilenceDetector;
import com.meetpulse.processing.SpeakerTurnDetector;
import com.meetpulse.processing.VoiceActivityDetector;
import com.meetpulse.service.MeetingAnalyzer;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class AudioCaptureService {

    private static final float SAMPLE_RATE = 44100f;
    private static final int SAMPLE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final int BUFFER_BYTES = 4096;
    private static final long CALIBRATION_MS = 3500;
    private static final double EMA_ALPHA = 0.24;
    private static final double NOISE_TRACK_ALPHA = 0.015;

    public enum Phase { IDLE, CALIBRATING, RECORDING, STOPPED }

    private volatile Phase phase = Phase.IDLE;
    private volatile boolean stopFlag = false;

    private double threshold = 800.0;
    private double noiseFloor = 0.0;
    private double liveRawRms = 0.0;
    private double liveSmoothedRms = 0.0;
    private double liveZcr = 0.0;
    private double liveSpeechLikelihood = 0.0;
    private double speechMultiplier = 1.6;
    private double[] liveBandEnergies = new double[]{0.0, 0.0, 0.0, 0.0};

    private TargetDataLine line;
    private final EnergyCalculator calc = new EnergyCalculator();
    private SilenceDetector detector = new SilenceDetector(threshold);
    private VoiceActivityDetector voiceDetector = new VoiceActivityDetector();
    private SpeakerTurnDetector speakerDetector = new SpeakerTurnDetector();
    private final MeetingAnalyzer analyzer = new MeetingAnalyzer();

    private TriConsumer<Double, Boolean, Phase> onFrame;
    private Consumer<String> onLog;
    private Consumer<Phase> onPhaseChange;

    @FunctionalInterface
    public interface TriConsumer<A, B, C> { void accept(A a, B b, C c); }

    public void setOnFrame(TriConsumer<Double, Boolean, Phase> cb) { this.onFrame = cb; }
    public void setOnLog(Consumer<String> cb) { this.onLog = cb; }
    public void setOnPhaseChange(Consumer<Phase> cb) { this.onPhaseChange = cb; }

    public void setSpeechMultiplier(double multiplier) {
        this.speechMultiplier = multiplier;
    }

    public double getSpeechMultiplier() { return speechMultiplier; }

    public void start() {
        stopFlag = false;
        analyzer.reset();
        voiceDetector.reset();
        speakerDetector.reset();

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

            EnergyCalculator.VoiceMetrics metrics = calc.calculateAll(buf, read);
            if (ema == 0.0) ema = metrics.rms;
            ema = (EMA_ALPHA * metrics.rms) + ((1.0 - EMA_ALPHA) * ema);

            liveRawRms = metrics.rms;
            liveSmoothedRms = ema;
            liveZcr = metrics.zcr;
            liveBandEnergies = metrics.bandEnergies;

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
            voiceDetector = new VoiceActivityDetector(threshold);
            voiceDetector.calibrate(noiseFloor);
            voiceDetector.reset();

            log(String.format("Calibration done — floor %.0f, threshold %.0f", noiseFloor, threshold));
        } else {
            noiseFloor = 300.0;
            threshold = 800.0;
            detector = new SilenceDetector(threshold, threshold * 0.82);
            detector.reset(true);
            voiceDetector = new VoiceActivityDetector(threshold);
            log("WARN: Calibration data unavailable, using defaults.");
        }
    }

    private void runRecording() {
        setPhase(Phase.RECORDING);
        log("Recording started — speech detection active.");

        byte[] buf = new byte[BUFFER_BYTES];
        String lastState = "";
        double ema = liveSmoothedRms > 0 ? liveSmoothedRms : 0.0;
        int frameCount = 0;

        while (!stopFlag) {
            int read = line.read(buf, 0, buf.length);
            if (read <= 0) continue;

            EnergyCalculator.VoiceMetrics metrics = calc.calculateAll(buf, read);
            if (ema == 0.0) ema = metrics.rms;
            ema = (EMA_ALPHA * metrics.rms) + ((1.0 - EMA_ALPHA) * ema);

            liveRawRms = metrics.rms;
            liveSmoothedRms = ema;
            liveZcr = metrics.zcr;
            liveBandEnergies = metrics.bandEnergies;

            VoiceActivityDetector.DetectionResult vadResult = voiceDetector.detect(metrics);
            boolean isSpeaking = vadResult.isSpeaking;

            if (ema < threshold * 0.9) {
                if (noiseFloor <= 0) noiseFloor = ema;
                noiseFloor = ((1.0 - NOISE_TRACK_ALPHA) * noiseFloor) + (NOISE_TRACK_ALPHA * ema);
                threshold = computeAdaptiveThreshold(noiseFloor, threshold);
                detector.setThresholds(threshold, threshold * 0.82);
            }

            boolean silent = detector.isSilent(ema) && !isSpeaking;
            analyzer.addFrame(new EnergyFrame(System.currentTimeMillis(), ema, silent));

            speakerDetector.processFrame(ema, metrics.zcr, System.currentTimeMillis());

            if (onFrame != null) onFrame.accept(ema, silent, Phase.RECORDING);

            liveSpeechLikelihood = vadResult.confidence;

            frameCount++;
            String state = silent ? "SILENT" : "SPEAKING";
            if (!state.equals(lastState) && frameCount % 10 == 0) {
                SpeakerTurnDetector.SpeakerAnalysis analysis = speakerDetector.getAnalysis();
                log(String.format("%s  RMS: %.0f  ZCR: %.3f  VAD: %.0f%%  Speakers: ~%d",
                        silent ? "Silence" : "Speaking",
                        ema, metrics.zcr,
                        vadResult.confidence * 100,
                        analysis.estimatedSpeakers));
                lastState = state;
            }
        }

        SpeakerTurnDetector.SpeakerAnalysis finalAnalysis = speakerDetector.getAnalysis();
        log(String.format("Session complete — Detected ~%d speaker(s), %d turns",
                finalAnalysis.estimatedSpeakers, finalAnalysis.totalTurns));
    }

    public void stop() { stopFlag = true; }

    public void reset() {
        stopFlag = true;
        closeHardware();
        analyzer.reset();
        voiceDetector.reset();
        speakerDetector.reset();
        threshold = 800.0;
        noiseFloor = 0.0;
        liveRawRms = 0.0;
        liveSmoothedRms = 0.0;
        liveZcr = 0.0;
        liveSpeechLikelihood = 0.0;
        liveBandEnergies = new double[]{0.0, 0.0, 0.0, 0.0};
        detector = new SilenceDetector(threshold, threshold * 0.82);
        detector.reset(true);
        setPhase(Phase.IDLE);
    }

    public Phase getPhase() { return phase; }
    public double getThreshold() { return threshold; }
    public double getNoiseFloor() { return noiseFloor; }
    public double getLiveRawRms() { return liveRawRms; }
    public double getLiveSmoothedRms() { return liveSmoothedRms; }
    public double getLiveZcr() { return liveZcr; }
    public double getLiveSpeechLikelihood() { return liveSpeechLikelihood; }
    public double[] getLiveBandEnergies() { return liveBandEnergies; }
    public MeetingAnalyzer getAnalyzer() { return analyzer; }
    public SpeakerTurnDetector getSpeakerDetector() { return speakerDetector; }
    public VoiceActivityDetector getVoiceDetector() { return voiceDetector; }

    public double getLiveSpeakingPct() { return analyzer.getLiveSpeakingPct(); }
    public int getLiveSegmentCount() { return analyzer.getLiveSegmentCount(); }
    public double getLivePeakRms() { return analyzer.getLivePeakRms(); }
    public int getLiveFrameCount() { return analyzer.getLiveFrameCount(); }
    public int getEstimatedSpeakers() { return speakerDetector.getAnalysis().estimatedSpeakers; }
    public int getSpeakerTurnCount() { return speakerDetector.getTurnCount(); }

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

    private double computeAdaptiveThreshold(double floor, double anchor) {
        double base = floor + 120.0;
        double ratio = floor * 2.25;
        double guided = Math.max(base, ratio);
        if (anchor > 0) guided = Math.max(guided, anchor * 0.92);
        return clamp(guided, 120.0, 12000.0);
    }

    private double clamp(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return v;
        return v;
    }

    private void setPhase(Phase p) {
        phase = p;
        if (onPhaseChange != null) onPhaseChange.accept(p);
    }

    private void log(String msg) {
        if (onLog != null) onLog.accept(msg);
    }
}
