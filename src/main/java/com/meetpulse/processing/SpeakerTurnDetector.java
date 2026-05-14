package com.meetpulse.processing;

import java.util.ArrayList;
import java.util.List;

public class SpeakerTurnDetector {

    public static final int DEFAULT_HISTORY_SIZE = 100;
    public static final double VOICE_ENVELOPE_WINDOW_MS = 150;
    public static final double SPEAKER_MIN_DURATION_MS = 800;
    public static final double TURN_GAP_THRESHOLD_MS = 500;

    private final int historySize;
    private final List<Double> energyHistory = new ArrayList<>();
    private final List<Long> timestampHistory = new ArrayList<>();
    private final List<Double> pitchHistory = new ArrayList<>();

    private final List<SpeakerTurn> turns = new ArrayList<>();
    private SpeakerTurn currentTurn = null;

    private double baselineEnergy = 0;
    private double currentEnergy = 0;
    private int turnCount = 0;
    private double totalSpeakingTime = 0;

    private long lastSpeakingStart = 0;
    private long lastSpeakingEnd = 0;
    private boolean wasSpeaking = false;

    private double estimatedSpeakerCount = 1.0;
    private double voiceLevelVariance = 0;
    private double averageVoiceLevel = 0;
    private final List<Double> voiceLevelSamples = new ArrayList<>();

    public SpeakerTurnDetector() {
        this(DEFAULT_HISTORY_SIZE);
    }

    public SpeakerTurnDetector(int historySize) {
        this.historySize = historySize;
    }

    public void processFrame(double rms, double zcr, long timestamp) {
        currentEnergy = rms;

        energyHistory.add(rms);
        timestampHistory.add(timestamp);

        double estimatedPitch = estimatePitch(rms, zcr);
        pitchHistory.add(estimatedPitch);

        if (energyHistory.size() > historySize) {
            energyHistory.remove(0);
            timestampHistory.remove(0);
            pitchHistory.remove(0);
        }

        updateBaseline(rms);
        updateVoiceLevelStats(rms);
        analyzeTurns(rms, timestamp, estimatedPitch);
    }

    private double estimatePitch(double rms, double zcr) {
        if (zcr <= 0) return 0;
        return (SAMPLE_RATE / 2.0) / zcr;
    }

    private void updateBaseline(double rms) {
        if (baselineEnergy <= 0) {
            baselineEnergy = rms;
        } else {
            baselineEnergy = (0.99 * baselineEnergy) + (0.01 * rms);
        }
    }

    private void updateVoiceLevelStats(double rms) {
        if (rms > baselineEnergy * 1.2) {
            voiceLevelSamples.add(rms);

            if (voiceLevelSamples.size() > 200) {
                voiceLevelSamples.remove(0);
            }

            if (voiceLevelSamples.size() >= 20) {
                calculateVoiceLevelMetrics();
                estimateSpeakerCount();
            }
        }
    }

    private void calculateVoiceLevelMetrics() {
        if (voiceLevelSamples.isEmpty()) return;

        double sum = 0;
        double sumSq = 0;
        for (double v : voiceLevelSamples) {
            sum += v;
            sumSq += v * v;
        }

        averageVoiceLevel = sum / voiceLevelSamples.size();
        double variance = (sumSq / voiceLevelSamples.size()) - (averageVoiceLevel * averageVoiceLevel);
        voiceLevelVariance = Math.max(0, variance);

        if (voiceLevelSamples.size() > 30) {
            double firstHalf = 0, secondHalf = 0;
            int mid = voiceLevelSamples.size() / 2;
            for (int i = 0; i < mid; i++) firstHalf += voiceLevelSamples.get(i);
            for (int i = mid; i < voiceLevelSamples.size(); i++) secondHalf += voiceLevelSamples.get(i);
            firstHalf /= mid;
            secondHalf /= (voiceLevelSamples.size() - mid);

            if (Math.abs(firstHalf - secondHalf) > averageVoiceLevel * 0.3) {
                voiceLevelVariance *= 1.5;
            }
        }
    }

    private void estimateSpeakerCount() {
        double stdDev = Math.sqrt(voiceLevelVariance);
        double cv = (averageVoiceLevel > 0) ? stdDev / averageVoiceLevel : 0;

        double pitchVariance = calculateVariance(pitchHistory);
        double pitchStdDev = Math.sqrt(Math.max(0, pitchVariance));

        double energySeparation = stdDev / (baselineEnergy + 1);
        double pitchSeparation = pitchStdDev / (estimatedSpeakerCount + 1);

        double separationScore = (cv * 0.5) + (energySeparation * 0.3) + (pitchSeparation * 0.2);

        if (separationScore > 0.4 || cv > 0.5) {
            estimatedSpeakerCount = 2.0;
        } else if (separationScore > 0.25 || cv > 0.3) {
            if (turnCount > 3 && voiceLevelSamples.size() > 50) {
                double turnRate = (double) turnCount / (voiceLevelSamples.size() / 10.0);
                if (turnRate > 0.3) {
                    estimatedSpeakerCount = 1.5;
                }
            }
        } else {
            estimatedSpeakerCount = 1.0;
        }
    }

    private double calculateVariance(List<Double> values) {
        if (values.size() < 2) return 0;
        double sum = 0;
        for (double v : values) sum += v;
        double mean = sum / values.size();
        double sumSq = 0;
        for (double v : values) {
            double d = v - mean;
            sumSq += d * d;
        }
        return sumSq / values.size();
    }

    private void analyzeTurns(double rms, long timestamp, double pitch) {
        boolean isSpeaking = rms > baselineEnergy * 1.5;

        if (isSpeaking && !wasSpeaking) {
            lastSpeakingStart = timestamp;
        } else if (!isSpeaking && wasSpeaking) {
            lastSpeakingEnd = timestamp;
            long duration = lastSpeakingEnd - lastSpeakingStart;

            if (duration >= SPEAKER_MIN_DURATION_MS) {
                completeTurn(duration, pitch);
            }
        }

        wasSpeaking = isSpeaking;
    }

    private void completeTurn(long durationMs, double avgPitch) {
        turnCount++;
        totalSpeakingTime += durationMs;

        if (currentTurn != null) {
            turns.add(currentTurn);
        }

        currentTurn = new SpeakerTurn(turnCount, durationMs, avgPitch);
    }

    public SpeakerAnalysis getAnalysis() {
        int estimatedSpeakers = (int) Math.ceil(estimatedSpeakerCount);

        if (estimatedSpeakerCount <= 1.2) {
            estimatedSpeakers = 1;
        } else if (estimatedSpeakerCount <= 1.7) {
            estimatedSpeakers = 2;
        } else {
            estimatedSpeakers = Math.min(estimatedSpeakers, 5);
        }

        double avgTurnDuration = turnCount > 0 ? totalSpeakingTime / turnCount : 0;
        double turnsPerMinute = (totalSpeakingTime > 0)
                ? (turnCount * 60000.0) / totalSpeakingTime
                : 0;

        return new SpeakerAnalysis(
                estimatedSpeakers,
                turnCount,
                totalSpeakingTime,
                avgTurnDuration,
                turnsPerMinute,
                voiceLevelVariance,
                averageVoiceLevel,
                new ArrayList<>(turns)
        );
    }

    public void reset() {
        energyHistory.clear();
        timestampHistory.clear();
        pitchHistory.clear();
        turns.clear();
        voiceLevelSamples.clear();
        currentTurn = null;
        turnCount = 0;
        totalSpeakingTime = 0;
        baselineEnergy = 0;
        currentEnergy = 0;
        estimatedSpeakerCount = 1.0;
        voiceLevelVariance = 0;
        averageVoiceLevel = 0;
        wasSpeaking = false;
        lastSpeakingStart = 0;
        lastSpeakingEnd = 0;
    }

    public double getCurrentEnergy() { return currentEnergy; }
    public double getBaselineEnergy() { return baselineEnergy; }
    public int getTurnCount() { return turnCount; }
    public double getEstimatedSpeakerCount() { return estimatedSpeakerCount; }

    public static class SpeakerTurn {
        public final int turnId;
        public final long durationMs;
        public final double avgPitch;

        public SpeakerTurn(int turnId, long durationMs, double avgPitch) {
            this.turnId = turnId;
            this.durationMs = durationMs;
            this.avgPitch = avgPitch;
        }
    }

    public static class SpeakerAnalysis {
        public final int estimatedSpeakers;
        public final int totalTurns;
        public final double totalSpeakingTimeMs;
        public final double avgTurnDurationMs;
        public final double turnsPerMinute;
        public final double voiceLevelVariance;
        public final double avgVoiceLevel;
        public final List<SpeakerTurn> turns;

        public SpeakerAnalysis(int estimatedSpeakers, int totalTurns, double totalSpeakingTimeMs,
                              double avgTurnDurationMs, double turnsPerMinute,
                              double voiceLevelVariance, double avgVoiceLevel,
                              List<SpeakerTurn> turns) {
            this.estimatedSpeakers = estimatedSpeakers;
            this.totalTurns = totalTurns;
            this.totalSpeakingTimeMs = totalSpeakingTimeMs;
            this.avgTurnDurationMs = avgTurnDurationMs;
            this.turnsPerMinute = turnsPerMinute;
            this.voiceLevelVariance = voiceLevelVariance;
            this.avgVoiceLevel = avgVoiceLevel;
            this.turns = turns;
        }
    }

    private static final float SAMPLE_RATE = 44100f;
}