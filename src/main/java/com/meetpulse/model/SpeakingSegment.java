package com.meetpulse.model;

public class SpeakingSegment {
    private final long startMs;
    private long endMs;

    public SpeakingSegment(long startMs) {
        this.startMs = startMs;
        this.endMs   = startMs;
    }

    public void close(long endMs) {
        this.endMs = endMs;
    }

    public long getStartMs()    { return startMs; }
    public long getEndMs()      { return endMs; }
    public long getDurationMs() { return endMs - startMs; }

    @Override
    public String toString() {
        return String.format("  [%5.1fs → %5.1fs]  duration: %.1fs",
                startMs / 1000.0,
                endMs   / 1000.0,
                getDurationMs() / 1000.0
        );
    }
}