package com.meetpulse.audio;

import com.meetpulse.model.EnergyFrame;
import com.meetpulse.model.MeetingStats;
import com.meetpulse.processing.EnergyCalculator;
import com.meetpulse.processing.SilenceDetector;
import com.meetpulse.service.MeetingAnalyzer;
import com.meetpulse.service.ReportExporter;

import javax.sound.sampled.*;

public class AudioCaptureService {

    private TargetDataLine line;
    private boolean running      = false;
    private boolean calibrating  = false;

    private double threshold = 1100.0; // fallback default

    private final EnergyCalculator energyCalculator = new EnergyCalculator();
    private       SilenceDetector  silenceDetector  = new SilenceDetector(threshold);
    private final MeetingAnalyzer  analyzer         = new MeetingAnalyzer();

    // ─── Calibration ────────────────────────────────────────────────

    public void calibrate() {
        try {
            AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
            DataLine.Info info  = new DataLine.Info(TargetDataLine.class, format);

            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            calibrating = true;

            byte[] buffer   = new byte[4096];
            double sumRms   = 0;
            double maxRms   = 0;
            int    frames   = 0;

            while (calibrating) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    double rms = energyCalculator.calculateRms(buffer, bytesRead);
                    sumRms += rms;
                    if (rms > maxRms) maxRms = rms;
                    frames++;
                }
            }

            line.stop();
            line.close();
            line = null;

            // Set threshold to avg * 1.5 — gives comfortable headroom above noise floor
            double avgRms = frames > 0 ? sumRms / frames : 500.0;
            threshold = avgRms * 1.5;
            silenceDetector = new SilenceDetector(threshold);

            System.out.printf("  Noise floor — Avg RMS: %.2f | Max RMS: %.2f%n", avgRms, maxRms);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopCalibration() {
        calibrating = false;
    }

    public double getThreshold() {
        return threshold;
    }

    // ─── Recording ──────────────────────────────────────────────────

    public void start() {
        try {
            AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
            DataLine.Info info  = new DataLine.Info(TargetDataLine.class, format);

            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            running = true;

            byte[] buffer    = new byte[4096];
            String lastState = "";

            while (running) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    double  rms    = energyCalculator.calculateRms(buffer, bytesRead);
                    boolean silent = silenceDetector.isSilent(rms);

                    analyzer.addFrame(new EnergyFrame(
                            System.currentTimeMillis(), rms, silent
                    ));

                    // Live indicator — only print on state change
                    String state = silent ? "🔇 SILENT" : "🎤 SPEAKING";
                    if (!state.equals(lastState)) {
                        System.out.printf("%-12s | RMS: %.2f%n", state, rms);
                        lastState = state;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        if (line != null) {
            line.stop();
            line.close();
        }
        System.out.println("\n🛑 Meeting stopped.");
        MeetingStats stats = analyzer.summarize();
        System.out.println(stats);

        // Export JSON for PDF generation
        ReportExporter.exportJson(
                stats,
                analyzer.getSegments(),
                analyzer.getRmsBySecond(),
                threshold
        );
    }
}
