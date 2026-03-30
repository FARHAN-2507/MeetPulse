package com.meetpulse;

import com.meetpulse.audio.AudioCaptureService;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== CALIBRATION MODE ===");
        System.out.println("Stay SILENT for 5 seconds...");
        System.out.println();

        AudioCaptureService audio = new AudioCaptureService();
        Thread thread = new Thread(audio::start);
        thread.start();

        Thread.sleep(10000);

        audio.stop();
    }
}