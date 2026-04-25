package com.meetpulse.processing;

public class EnergyCalculator {

    public double calculateRms(byte[] buffer, int bytesRead) {
        if (buffer == null || bytesRead < 2) return 0.0;
        int evenBytes = bytesRead & ~1;
        if (evenBytes < 2) return 0.0;
        long sum = 0;
        for (int i = 0; i < evenBytes - 1; i += 2) {
            short sample = (short) ((buffer[i] << 8) | (buffer[i + 1] & 0xFF));
            sum += (long) sample * sample;
        }
        int sampleCount = evenBytes / 2;
        if (sampleCount <= 0) return 0.0;
        return Math.sqrt((double) sum / sampleCount);
    }
}
