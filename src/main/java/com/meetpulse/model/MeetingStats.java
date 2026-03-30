package com.meetpulse.model;

public class MeetingStats {
    private final int totalFrames;
    private final int silentFrames;
    private final double avgRms;
    private final double peakRms;
    private final long durationMs;

    public MeetingStats(int totalFrames, int silentFrames,
                        double avgRms, double peakRms, long durationMs) {
        this.totalFrames  = totalFrames;
        this.silentFrames = silentFrames;
        this.avgRms       = avgRms;
        this.peakRms      = peakRms;
        this.durationMs   = durationMs;
    }

    public double getSilenceRatio() {
        return totalFrames == 0 ? 0 : (double) silentFrames / totalFrames;
    }

    @Override
    public String toString() {
        return String.format(
                "%n=== Meeting Stats ===%n" +
                        "Duration      : %.1f sec%n" +
                        "Total frames  : %d%n" +
                        "Silent frames : %d (%.1f%%)%n" +
                        "Speaking      : %.1f%%%n" +
                        "Avg RMS       : %.2f%n" +
                        "Peak RMS      : %.2f",
                durationMs / 1000.0,
                totalFrames,
                silentFrames,
                getSilenceRatio() * 100,
                (1 - getSilenceRatio()) * 100,
                avgRms,
                peakRms
        );
    }
}