package com.meetpulse.service;

import com.meetpulse.model.EnergyFrame;
import com.meetpulse.model.MeetingStats;
import com.meetpulse.model.SpeakingSegment;

import java.util.ArrayList;
import java.util.List;

public class MeetingAnalyzer {

    private final List<EnergyFrame>     frames   = new ArrayList<>();
    private final List<SpeakingSegment> segments = new ArrayList<>();
    private final long startTime = System.currentTimeMillis();

    private SpeakingSegment currentSegment = null;

    public void addFrame(EnergyFrame frame) {
        frames.add(frame);
        long relativeMs = frame.getTimestamp() - startTime;

        if (!frame.isSilent()) {
            if (currentSegment == null) {
                currentSegment = new SpeakingSegment(relativeMs);
            } else {
                currentSegment.close(relativeMs);
            }
        } else {
            if (currentSegment != null) {
                currentSegment.close(relativeMs);
                if (currentSegment.getDurationMs() >= 200) {
                    segments.add(currentSegment);
                }
                currentSegment = null;
            }
        }
    }

    // Bucket frames into 1-second RMS averages for timeline
    public List<Double> getRmsBySecond() {
        List<Double> result = new ArrayList<>();
        if (frames.isEmpty()) return result;

        long bucketStart = frames.get(0).getTimestamp();
        double bucketSum = 0;
        int    bucketCount = 0;

        for (EnergyFrame f : frames) {
            if (f.getTimestamp() - bucketStart >= 1000) {
                result.add(bucketCount > 0 ? bucketSum / bucketCount : 0);
                bucketSum   = 0;
                bucketCount = 0;
                bucketStart = f.getTimestamp();
            }
            bucketSum += f.getRms();
            bucketCount++;
        }
        if (bucketCount > 0) result.add(bucketSum / bucketCount);
        return result;
    }

    public List<SpeakingSegment> getSegments() { return segments; }

    public MeetingStats summarize() {
        if (currentSegment != null) {
            currentSegment.close(System.currentTimeMillis() - startTime);
            if (currentSegment.getDurationMs() >= 200) segments.add(currentSegment);
            currentSegment = null;
        }

        int total = frames.size(), silent = 0;
        double sumRms = 0, peak = 0;

        for (EnergyFrame f : frames) {
            if (f.isSilent()) silent++;
            sumRms += f.getRms();
            if (f.getRms() > peak) peak = f.getRms();
        }

        long durationMs = System.currentTimeMillis() - startTime;
        double avg = total > 0 ? sumRms / total : 0;
        return new MeetingStats(total, silent, avg, peak, durationMs, segments);
    }
}