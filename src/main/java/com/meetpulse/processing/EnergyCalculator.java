package com.meetpulse.processing;

public class EnergyCalculator {

    public double calculateRms(byte[] buffer, int bytesRead) {
        long sum = 0;
        for (int i = 0; i < bytesRead - 1; i += 2) {
            short sample = (short) ((buffer[i] << 8) | (buffer[i + 1] & 0xFF));
            sum += (long) sample * sample;
        }
        int sampleCount = bytesRead / 2;
        return Math.sqrt((double) sum / sampleCount);
    }
}