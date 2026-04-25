package com.meetpulse.processing;

public class SilenceDetector {

    private static final double THRESHOLD = 1100.0;
    private double speechThreshold;
    private double silenceThreshold;
    private boolean silentState = true;

    public SilenceDetector() {
        this(THRESHOLD);
    }

    public SilenceDetector(double threshold) {
        this(threshold, threshold * 0.82);
    }

    public SilenceDetector(double speechThreshold, double silenceThreshold) {
        setThresholds(speechThreshold, silenceThreshold);
    }

    public boolean isSilent(double rms) {
        // Hysteresis: harder to switch states, avoids rapid flicker near threshold.
        if (silentState) {
            if (rms >= speechThreshold) silentState = false;
        } else {
            if (rms <= silenceThreshold) silentState = true;
        }
        return silentState;
    }

    public void reset(boolean silent) {
        this.silentState = silent;
    }

    public void setThresholds(double speechThreshold, double silenceThreshold) {
        this.speechThreshold = speechThreshold;
        this.silenceThreshold = Math.min(silenceThreshold, speechThreshold);
    }
}
