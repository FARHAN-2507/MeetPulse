package com.meetpulse.model;

import java.util.List;

public class MeetingSession {

    private String id;
    private long timestamp;
    private long durationMs;
    private double speakingRatio;
    private int speakerCount;
    private int totalTurns;
    private double avgTurnDuration;
    private double qualityScore;
    private double estimatedWpm;
    private double peakRms;
    private double avgNoiseFloor;
    private double avgThreshold;
    private int totalFrames;

    private List<SpeakingSegment> segments;

    public MeetingSession() {
        this.id = java.util.UUID.randomUUID().toString().substring(0, 8);
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public double getSpeakingRatio() { return speakingRatio; }
    public void setSpeakingRatio(double speakingRatio) { this.speakingRatio = speakingRatio; }

    public int getSpeakerCount() { return speakerCount; }
    public void setSpeakerCount(int speakerCount) { this.speakerCount = speakerCount; }

    public int getTotalTurns() { return totalTurns; }
    public void setTotalTurns(int totalTurns) { this.totalTurns = totalTurns; }

    public double getAvgTurnDuration() { return avgTurnDuration; }
    public void setAvgTurnDuration(double avgTurnDuration) { this.avgTurnDuration = avgTurnDuration; }

    public double getQualityScore() { return qualityScore; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }

    public double getEstimatedWpm() { return estimatedWpm; }
    public void setEstimatedWpm(double estimatedWpm) { this.estimatedWpm = estimatedWpm; }

    public double getPeakRms() { return peakRms; }
    public void setPeakRms(double peakRms) { this.peakRms = peakRms; }

    public double getAvgNoiseFloor() { return avgNoiseFloor; }
    public void setAvgNoiseFloor(double avgNoiseFloor) { this.avgNoiseFloor = avgNoiseFloor; }

    public double getAvgThreshold() { return avgThreshold; }
    public void setAvgThreshold(double avgThreshold) { this.avgThreshold = avgThreshold; }

    public int getTotalFrames() { return totalFrames; }
    public void setTotalFrames(int totalFrames) { this.totalFrames = totalFrames; }

    public List<SpeakingSegment> getSegments() { return segments; }
    public void setSegments(List<SpeakingSegment> segments) { this.segments = segments; }

    public String getFormattedDuration() {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public String getFormattedDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm");
        return sdf.format(new java.util.Date(timestamp));
    }
}