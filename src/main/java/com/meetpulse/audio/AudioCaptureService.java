package com.meetpulse.audio;

import com.meetpulse.model.EnergyFrame;
import com.meetpulse.processing.EnergyCalculator;
import com.meetpulse.processing.SilenceDetector;
import com.meetpulse.service.MeetingAnalyzer;

import javax.sound.sampled.*;
import java.util.function.Consumer;

public class AudioCaptureService {

    // ── audio config ──────────────────────────────────────────────────────
    private static final float  SAMPLE_RATE   = 44100f;
    private static final int    SAMPLE_BITS   = 16;
    private static final int    CHANNELS      = 1;
    private static final int    BUFFER_BYTES  = 4096;
    private static final long   CALIBRATION_MS = 3000;
    private static final long   MIN_SEGMENT_MS = 200;

    // ── state ─────────────────────────────────────────────────────────────
    public enum Phase { IDLE, CALIBRATING, RECORDING, STOPPED }
    private volatile Phase  phase      = Phase.IDLE;
    private volatile boolean stopFlag  = false;

    private double threshold = 800.0;

    // ── components ────────────────────────────────────────────────────────
    private TargetDataLine      line;
    private final EnergyCalculator  calc     = new EnergyCalculator();
    private       SilenceDetector   detector = new SilenceDetector(threshold);
    private final MeetingAnalyzer   analyzer = new MeetingAnalyzer();

    // ── callbacks (all called from the audio thread) ───────────────────────
    /** Called every frame: (rms, isSilent, phase) */
    private TriConsumer<Double, Boolean, Phase> onFrame;
    /** Called for log messages */
    private Consumer<String> onLog;
    /** Called when phase changes */
    private Consumer<Phase> onPhaseChange;

    @FunctionalInterface
    public interface TriConsumer<A, B, C> { void accept(A a, B b, C c); }

    public void setOnFrame(TriConsumer<Double, Boolean, Phase> cb)  { this.onFrame = cb; }
    public void setOnLog(Consumer<String> cb)                        { this.onLog = cb; }
    public void setOnPhaseChange(Consumer<Phase> cb)                 { this.onPhaseChange = cb; }

    // ── main entry ────────────────────────────────────────────────────────
    public void start() {
        stopFlag = false;
        analyzer.reset();

        try {
            AudioFormat format = new AudioFormat(
                    SAMPLE_RATE, SAMPLE_BITS, CHANNELS, true, true
            );
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
        log("Calibrating — please stay silent...");

        byte[]  buf       = new byte[BUFFER_BYTES];
        double  sumRms    = 0;
        int     count     = 0;
        long    deadline  = System.currentTimeMillis() + CALIBRATION_MS;

        while (!stopFlag && System.currentTimeMillis() < deadline) {
            int read = line.read(buf, 0, buf.length);
            if (read <= 0) continue;

            double rms = calc.calculateRms(buf, read);
            sumRms += rms;
            count++;

            if (onFrame != null) onFrame.accept(rms, true, Phase.CALIBRATING);
        }

        if (count > 0) {
            double avg = sumRms / count;
            threshold  = Math.max(avg * 1.8, 200.0); // 1.8× headroom
            detector   = new SilenceDetector(threshold);
            log(String.format("Calibration done — noise floor %.0f → threshold %.0f", avg, threshold));
        }
    }

    // ── recording ─────────────────────────────────────────────────────────
    private void runRecording() {
        setPhase(Phase.RECORDING);
        log("Recording started — speak freely!");

        byte[]  buf       = new byte[BUFFER_BYTES];
        String  lastState = "";

        while (!stopFlag) {
            int read = line.read(buf, 0, buf.length);
            if (read <= 0) continue;

            double  rms    = calc.calculateRms(buf, read);
            boolean silent = detector.isSilent(rms);

            analyzer.addFrame(new EnergyFrame(System.currentTimeMillis(), rms, silent));

            if (onFrame != null) onFrame.accept(rms, silent, Phase.RECORDING);

            String state = silent ? "SILENT" : "SPEAKING";
            if (!state.equals(lastState)) {
                log(String.format("%s  RMS: %.0f",
                        silent ? "🔇 Silence" : "🎤 Speaking", rms));
                lastState = state;
            }
        }
    }

    // ── stop ──────────────────────────────────────────────────────────────
    public void stop() {
        stopFlag = true;
    }

    public void reset() {
        stopFlag = true;
        closeHardware();
        analyzer.reset();
        threshold = 800.0;
        detector  = new SilenceDetector(threshold);
        setPhase(Phase.IDLE);
    }

    // ── accessors ─────────────────────────────────────────────────────────
    public Phase   getPhase()      { return phase; }
    public double  getThreshold()  { return threshold; }
    public MeetingAnalyzer getAnalyzer() { return analyzer; }

    /** Safe live snapshot — never mutates segment state */
    public double getLiveSpeakingPct() { return analyzer.getLiveSpeakingPct(); }
    public int    getLiveSegmentCount(){ return analyzer.getLiveSegmentCount(); }
    public double getLivePeakRms()     { return analyzer.getLivePeakRms(); }
    public int    getLiveFrameCount()  { return analyzer.getLiveFrameCount(); }

    // ── helpers ───────────────────────────────────────────────────────────
    private void closeHardware() {
        if (line != null && line.isOpen()) {
            line.stop();
            line.close();
        }
    }

    private void setPhase(Phase p) {
        phase = p;
        if (onPhaseChange != null) onPhaseChange.accept(p);
    }

    private void log(String msg) {
        if (onLog != null) onLog.accept(msg);
    }
}