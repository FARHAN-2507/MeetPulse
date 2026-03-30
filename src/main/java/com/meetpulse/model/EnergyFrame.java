package com.meetpulse.model;

public class EnergyFrame {
    private final long timestamp;
    private final double rms;
    private final boolean silent;

    public EnergyFrame(long timestamp, double rms, boolean silent) {
        this.timestamp = timestamp;
        this.rms = rms;
        this.silent = silent;
    }

    public long getTimestamp() { return timestamp; }
    public double getRms()     { return rms; }
    public boolean isSilent()  { return silent; }
}