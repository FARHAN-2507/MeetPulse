package com.meetpulse;

import com.meetpulse.audio.AudioCaptureService;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        AudioCaptureService audio = new AudioCaptureService();

        // Step 1 — calibrate
        System.out.println("🎙 Stay silent for 3 seconds — calibrating...");
        Thread thread = new Thread(audio::calibrate);
        thread.start();
        Thread.sleep(3000);
        audio.stopCalibration();
        thread.join();

        System.out.println("✅ Calibration done! Threshold set to: " + audio.getThreshold());
        System.out.println();

        // Step 2 — start meeting
        System.out.println("🎤 Meeting started — speak freely!");
        Thread meetingThread = new Thread(audio::start);
        meetingThread.start();
        Thread.sleep(10000);
        audio.stop();
    }
}