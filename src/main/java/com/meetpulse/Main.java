package com.meetpulse;

import com.meetpulse.audio.AudioCaptureService;

public class Main {
    public static void main(String[] args) throws InterruptedException {

        AudioCaptureService audio = new AudioCaptureService();

        Thread thread = new Thread(audio::start);
        thread.start();

        Thread.sleep(5000);

        audio.stop();
    }
}