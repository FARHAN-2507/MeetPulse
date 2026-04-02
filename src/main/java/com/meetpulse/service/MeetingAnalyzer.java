package com.meetpulse.service;

import com.meetpulse.model.EnergyFrame;
import com.meetpulse.model.MeetingStats;
import com.meetpulse.model.SpeakingSegment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MeetingAnalyzer {

    private final List<EnergyFrame>      frames   = new ArrayList<>();
    private final List<SpeakingSegment>  segments = new ArrayList<>();
    private       SpeakingSegment        current  = null;
    private       long                   startTime;
    private       double                 peakRms  = 0;

    public MeetingAnalyzer() { startTime = System.currentTimeMillis(); }

    // ── reset for new session ─────────────────────────────────────────────
    public synchronized void reset() {
        frames.clear();
        segments.clear();
        current   = null;
        peakRms   = 0;
        startTime = System.currentTimeMillis();
    }

    // ── add frame ─────────────────────────────────────────────────────────
    public synchronized void addFrame(EnergyFrame frame) {
        frames.add(frame);
        if (frame.getRms() > peakRms) peakRms = frame.getRms();

        long relMs = frame.getTimestamp() - startTime;

        if (!frame.isSilent()) {
            if (current == null) {
                current = new SpeakingSegment(relMs);
            } else {
                current.extend(relMs);
            }
        } else {
            if (current != null) {
                current.close(relMs);
                if (current.getDurationMs() >= 200) {
                    segments.add(current);
                }
                current = null;
            }
        }
    }

    // ── live stats (NON-mutating, safe to call anytime) ───────────────────
    public synchronized double getLiveSpeakingPct() {
        int total = frames.size();
        if (total == 0) return 0;
        long silent = frames.stream().filter(EnergyFrame::isSilent).count();
        return (1.0 - (double) silent / total) * 100.0;
    }

    public synchronized int getLiveSegmentCount() {
        return segments.size() + (current != null ? 1 : 0);
    }

    public synchronized double getLivePeakRms() { return peakRms; }

    public synchronized int getLiveFrameCount() { return frames.size(); }

    // ── final summary (called after stop) ────────────────────────────────
    public synchronized MeetingStats summarize() {
        // Close any open segment at the time of summarizing
        List<SpeakingSegment> allSegs = new ArrayList<>(segments);
        if (current != null) {
            SpeakingSegment closing = new SpeakingSegment(current.getStartMs());
            closing.extend(current.getEndMs());
            closing.close(current.getEndMs());
            if (closing.getDurationMs() >= 200) allSegs.add(closing);
        }

        int total   = frames.size();
        int silent  = 0;
        double sumRms = 0;

        for (EnergyFrame f : frames) {
            if (f.isSilent()) silent++;
            sumRms += f.getRms();
        }

        long   durationMs = System.currentTimeMillis() - startTime;
        double avg        = total > 0 ? sumRms / total : 0;

        return new MeetingStats(total, silent, avg, peakRms, durationMs,
                Collections.unmodifiableList(allSegs));
    }

    // ── per-second RMS for charts ─────────────────────────────────────────
    public synchronized List<Double> getRmsBySecond() {
        List<Double> result = new ArrayList<>();
        if (frames.isEmpty()) return result;

        long bucketStart = frames.get(0).getTimestamp();
        double sumBucket = 0;
        int    cntBucket = 0;

        for (EnergyFrame f : frames) {
            if (f.getTimestamp() - bucketStart >= 1000) {
                result.add(cntBucket > 0 ? sumBucket / cntBucket : 0);
                sumBucket  = 0;
                cntBucket  = 0;
                bucketStart = f.getTimestamp();
            }
            sumBucket += f.getRms();
            cntBucket++;
        }
        if (cntBucket > 0) result.add(sumBucket / cntBucket);
        return result;
    }
}