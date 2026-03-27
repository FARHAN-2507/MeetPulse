package com.meetpulse.audio;

import javax.sound.sampled.*;

public class AudioCaptureService {

    private TargetDataLine line;
    private boolean running = false;

    public void start() {
        try {
            AudioFormat format = new AudioFormat(
                    44100.0f,
                    16,
                    1,
                    true,
                    true
            );

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("❌ Microphone not supported");
                return;
            }

            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            running = true;

            System.out.println("🎤 Mic started...");

            byte[] buffer = new byte[4096];

            while (running) {
                int bytesRead = line.read(buffer, 0, buffer.length);

                if (bytesRead > 0) {
                    System.out.println("Audio chunk: " + bytesRead);
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
        System.out.println("🛑 Mic stopped.");
    }
}