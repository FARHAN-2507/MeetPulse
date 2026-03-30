package com.meetpulse.processing;

public class SilenceDetector {

    private static final double THRESHOLD = 1100.0;
    private final double threshold;

    public SilenceDetector() {
        this(THRESHOLD);
    }

    public SilenceDetector(double threshold) {
        this.threshold = threshold;
    }

    public boolean isSilent(double rms) {
        return rms < threshold;
    }
}