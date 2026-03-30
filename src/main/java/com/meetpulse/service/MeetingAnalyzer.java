package com.meetpulse.service;

import com.meetpulse.model.EnergyFrame;
import com.meetpulse.model.MeetingStats;
import java.util.ArrayList;
import java.util.List;

public class MeetingAnalyzer {

    private final List<EnergyFrame> frames = new ArrayList<>();
    private final long startTime = System.currentTimeMillis();

    public void addFrame(EnergyFrame frame) {
        frames.add(frame);
    }

    public MeetingStats summarize() {
        int total   = frames.size();
        int silent  = 0;
        double sumRms = 0;
        double peak   = 0;

        for (EnergyFrame f : frames) {
            if (f.isSilent()) silent++;
            sumRms += f.getRms();
            if (f.getRms() > peak) peak = f.getRms();
        }

        long durationMs = System.currentTimeMillis() - startTime;
        double avg = total > 0 ? sumRms / total : 0;
        return new MeetingStats(total, silent, avg, peak, durationMs);
    }
}