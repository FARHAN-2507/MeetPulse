package com.meetpulse.model;

import java.util.List;

public class MeetingStats {
    private final int totalFrames;
    private final int silentFrames;
    private final double avgRms;
    private final double peakRms;
    private final long durationMs;
    private final List<SpeakingSegment> segments;

    public MeetingStats(int totalFrames, int silentFrames,
                        double avgRms, double peakRms,
                        long durationMs, List<SpeakingSegment> segments) {
        this.totalFrames  = totalFrames;
        this.silentFrames = silentFrames;
        this.avgRms       = avgRms;
        this.peakRms      = peakRms;
        this.durationMs   = durationMs;
        this.segments     = segments;
    }

    public int    getTotalFrames()  { return totalFrames; }
    public int    getSilentFrames() { return silentFrames; }
    public double getAvgRms()       { return avgRms; }
    public double getPeakRms()      { return peakRms; }
    public long   getDurationMs()   { return durationMs; }
    public List<SpeakingSegment> getSegments() { return segments; }

    public double getSilenceRatio() {
        return totalFrames == 0 ? 0 : (double) silentFrames / totalFrames;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "%n=== Meeting Stats ===%n" +
                        "Duration      : %.1f sec%n" +
                        "Total frames  : %d%n" +
                        "Silent frames : %d (%.1f%%)%n" +
                        "Speaking      : %.1f%%%n" +
                        "Avg RMS       : %.2f%n" +
                        "Peak RMS      : %.2f%n",
                durationMs / 1000.0,
                totalFrames,
                silentFrames,
                getSilenceRatio() * 100,
                (1 - getSilenceRatio()) * 100,
                avgRms,
                peakRms
        ));

        sb.append(String.format("%n=== Speaking Segments (%d) ===%n", segments.size()));
        if (segments.isEmpty()) {
            sb.append("  No speaking detected.\n");
        } else {
            long totalSpeakingMs = 0;
            for (int i = 0; i < segments.size(); i++) {
                SpeakingSegment seg = segments.get(i);
                sb.append(String.format("  #%-3d %s%n", i + 1, seg));
                totalSpeakingMs += seg.getDurationMs();
            }
            sb.append(String.format("%n  Total speaking time : %.1f sec%n", totalSpeakingMs / 1000.0));
            sb.append(String.format("  Avg segment length  : %.1f sec%n",
                    totalSpeakingMs / 1000.0 / segments.size()));
        }
        return sb.toString();
    }
}