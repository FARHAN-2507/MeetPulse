package com.meetpulse.processing;

public class VoiceActivityDetector {

    private double energyThreshold;
    private double speechMultiplier = 1.6;
    private int minSpeechFrames = 3;
    private int minSilenceFrames = 6;

    private boolean isSpeaking = false;
    private int consecutiveSpeechFrames = 0;
    private int consecutiveSilenceFrames = 0;

    private double longTermEnergyAvg = 0;
    private double calibratedNoiseFloor = 0;
    private double shortTermEnergyAvg = 0;
    private static final double ST_ALPHA = 0.3;
    private static final double LT_ALPHA = 0.02;

    private int frameCount = 0;

    public VoiceActivityDetector() {
        this(800.0);
    }

    public VoiceActivityDetector(double energyThreshold) {
        this.energyThreshold = energyThreshold;
    }

    public DetectionResult detect(EnergyCalculator.VoiceMetrics metrics) {
        frameCount++;
        updateEnergyTracking(metrics.rms);

        boolean energyBasedSpeech = isEnergySpeech(metrics.rms);

        if (energyBasedSpeech) {
            consecutiveSpeechFrames++;
            consecutiveSilenceFrames = 0;
        } else {
            consecutiveSilenceFrames++;
            if (consecutiveSpeechFrames > 0) {
                consecutiveSpeechFrames--;
            }
        }

        if (consecutiveSpeechFrames >= minSpeechFrames) {
            isSpeaking = true;
        }
        if (consecutiveSilenceFrames >= minSilenceFrames) {
            isSpeaking = false;
        }

        double confidence = calculateConfidence(energyBasedSpeech, metrics);

        return new DetectionResult(isSpeaking, energyBasedSpeech,
                metrics.rms, metrics.zcr, confidence);
    }

    private boolean isEnergySpeech(double rms) {
        double adaptiveThreshold = energyThreshold;

        if (longTermEnergyAvg > 0) {
            double adaptive = longTermEnergyAvg * speechMultiplier;
            adaptiveThreshold = Math.max(adaptive, energyThreshold * 0.7);
        }

        if (calibratedNoiseFloor > 0) {
            double calibrated = calibratedNoiseFloor * speechMultiplier;
            adaptiveThreshold = Math.max(adaptiveThreshold, calibrated);
        }

        return rms >= adaptiveThreshold;
    }

    private double calculateConfidence(boolean energyBased, EnergyCalculator.VoiceMetrics metrics) {
        if (!energyBased) return 0.0;

        double confidence = 0.6;

        double rmsRatio = longTermEnergyAvg > 0 ? metrics.rms / longTermEnergyAvg : 1;
        if (rmsRatio > 2.0) confidence += 0.2;
        else if (rmsRatio > 1.5) confidence += 0.1;

        if (isSpeaking) confidence += 0.2;

        return Math.min(1.0, confidence);
    }

    private void updateEnergyTracking(double rms) {
        if (shortTermEnergyAvg <= 0) {
            shortTermEnergyAvg = rms;
            longTermEnergyAvg = rms;
        } else {
            shortTermEnergyAvg = (ST_ALPHA * rms) + ((1.0 - ST_ALPHA) * shortTermEnergyAvg);
            longTermEnergyAvg = (LT_ALPHA * rms) + ((1.0 - LT_ALPHA) * longTermEnergyAvg);
        }

        if (calibratedNoiseFloor <= 0 && rms > 0) {
            calibratedNoiseFloor = rms;
        }
    }

    public void calibrate(double noiseFloorValue) {
        this.calibratedNoiseFloor = noiseFloorValue;
    }

    public void reset() {
        isSpeaking = false;
        consecutiveSpeechFrames = 0;
        consecutiveSilenceFrames = 0;
        longTermEnergyAvg = 0;
        shortTermEnergyAvg = 0;
        calibratedNoiseFloor = 0;
        frameCount = 0;
    }

    public void setThresholds(double energyThreshold, double speechMultiplier,
                              int minSpeechFrames, int minSilenceFrames) {
        this.energyThreshold = energyThreshold;
        this.speechMultiplier = speechMultiplier;
        this.minSpeechFrames = minSpeechFrames;
        this.minSilenceFrames = minSilenceFrames;
    }

    public void setEnergyThreshold(double threshold) {
        this.energyThreshold = threshold;
    }

    public void setSpeechMultiplier(double multiplier) {
        this.speechMultiplier = multiplier;
    }

    public double getLongTermEnergy() { return longTermEnergyAvg; }
    public double getShortTermEnergy() { return shortTermEnergyAvg; }
    public double getNoiseFloor() { return calibratedNoiseFloor; }
    public int getFrameCount() { return frameCount; }
    public boolean isCurrentlySpeaking() { return isSpeaking; }

    public static class DetectionResult {
        public final boolean isSpeaking;
        public final boolean energyBased;
        public final double rms;
        public final double zcr;
        public final double confidence;

        public DetectionResult(boolean isSpeaking, boolean energyBased,
                              double rms, double zcr, double confidence) {
            this.isSpeaking = isSpeaking;
            this.energyBased = energyBased;
            this.rms = rms;
            this.zcr = zcr;
            this.confidence = confidence;
        }
    }
}