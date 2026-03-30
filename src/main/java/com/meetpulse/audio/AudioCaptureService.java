package com.meetpulse.audio;

import com.meetpulse.model.EnergyFrame;
import com.meetpulse.processing.EnergyCalculator;
import com.meetpulse.processing.SilenceDetector;
import com.meetpulse.service.MeetingAnalyzer;

import javax.sound.sampled.*;

public class AudioCaptureService {

    private TargetDataLine line;
    private boolean running = false;

    private final EnergyCalculator  energyCalculator = new EnergyCalculator();
    private final SilenceDetector   silenceDetector  = new SilenceDetector();
    private final MeetingAnalyzer   analyzer         = new MeetingAnalyzer();

    public void start() {
        try {
            AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
            DataLine.Info info  = new DataLine.Info(TargetDataLine.class, format);

            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            running = true;

            System.out.println("🎤 Meeting started — speak freely!\n");

            byte[] buffer  = new byte[4096];
            String lastState = "";

            while (running) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    double rms     = energyCalculator.calculateRms(buffer, bytesRead);
                    boolean silent = silenceDetector.isSilent(rms);

                    EnergyFrame frame = new EnergyFrame(
                            System.currentTimeMillis(), rms, silent
                    );
                    analyzer.addFrame(frame);

                    // Live state indicator (only on change)
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
        System.out.println("\n🛑 Mic stopped.");
        System.out.println(analyzer.summarize());
    }
}
