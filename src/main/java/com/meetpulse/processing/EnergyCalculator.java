package com.meetpulse.processing;

public class EnergyCalculator {

    public double calculateRms(byte[] buffer, int bytesRead) {
        if (buffer == null || bytesRead < 2) return 0.0;
        int evenBytes = bytesRead & ~1;
        if (evenBytes < 2) return 0.0;
        long sum = 0;
        for (int i = 0; i < evenBytes - 1; i += 2) {
            short sample = (short) ((buffer[i] << 8) | (buffer[i + 1] & 0xFF));
            sum += (long) sample * sample;
        }
        int sampleCount = evenBytes / 2;
        if (sampleCount <= 0) return 0.0;
        return Math.sqrt((double) sum / sampleCount);
    }

    public int countZeroCrossings(byte[] buffer, int bytesRead) {
        if (buffer == null || bytesRead < 2) return 0;
        int crossings = 0;
        short prevSample = 0;
        boolean first = true;

        for (int i = 0; i < bytesRead - 1; i += 2) {
            short sample = (short) ((buffer[i] << 8) | (buffer[i + 1] & 0xFF));
            if (first) {
                prevSample = sample;
                first = false;
                continue;
            }
            if ((prevSample >= 0 && sample < 0) || (prevSample < 0 && sample >= 0)) {
                crossings++;
            }
            prevSample = sample;
        }
        return crossings;
    }

    public double calculateZcr(byte[] buffer, int bytesRead) {
        int crossings = countZeroCrossings(buffer, bytesRead);
        int sampleCount = (bytesRead / 2);
        if (sampleCount <= 0) return 0.0;
        return (double) crossings / sampleCount;
    }

    public double[] calculateBandEnergies(byte[] buffer, int bytesRead, int numBands) {
        double[] bandEnergies = new double[numBands];
        if (buffer == null || bytesRead < 2 || numBands <= 0) return bandEnergies;

        int samplesPerBand = (bytesRead / 2) / numBands;
        if (samplesPerBand <= 0) samplesPerBand = 1;

        for (int band = 0; band < numBands; band++) {
            long sum = 0;
            int startSample = band * samplesPerBand * 2;
            int endSample = Math.min(startSample + samplesPerBand * 2, bytesRead);

            for (int i = startSample; i < endSample - 1; i += 2) {
                short sample = (short) ((buffer[i] << 8) | (buffer[i + 1] & 0xFF));
                sum += (long) sample * sample;
            }

            int count = (endSample - startSample) / 2;
            if (count > 0) {
                bandEnergies[band] = Math.sqrt((double) sum / count);
            }
        }
        return bandEnergies;
    }

    public double calculateSpectralCentroid(byte[] buffer, int bytesRead) {
        if (buffer == null || bytesRead < 2) return 0.0;

        double[] bandEnergies = calculateBandEnergies(buffer, bytesRead, 8);
        double totalEnergy = 0.0;
        double weightedSum = 0.0;

        for (int i = 0; i < bandEnergies.length; i++) {
            totalEnergy += bandEnergies[i];
            weightedSum += bandEnergies[i] * i;
        }

        if (totalEnergy <= 0) return 0.0;
        return weightedSum / totalEnergy;
    }

    public VoiceMetrics calculateAll(byte[] buffer, int bytesRead) {
        double rms = calculateRms(buffer, bytesRead);
        double zcr = calculateZcr(buffer, bytesRead);
        double[] bandEnergies = calculateBandEnergies(buffer, bytesRead, 4);
        double centroid = calculateSpectralCentroid(buffer, bytesRead);

        return new VoiceMetrics(rms, zcr, bandEnergies, centroid);
    }

    public static class VoiceMetrics {
        public final double rms;
        public final double zcr;
        public final double[] bandEnergies;
        public final double spectralCentroid;

        public double lowBand() { return bandEnergies.length > 0 ? bandEnergies[0] : 0; }
        public double midBand() { return bandEnergies.length > 1 ? bandEnergies[1] : 0; }
        public double highBand() { return bandEnergies.length > 2 ? bandEnergies[2] : 0; }
        public double ultraHighBand() { return bandEnergies.length > 3 ? bandEnergies[3] : 0; }

        public double speechLikelihood() {
            double zcrScore = Math.min(zcr * 25, 1.0);
            double centroidScore = Math.min(spectralCentroid / 3.5, 1.0);
            double lowEnergyRatio = bandEnergies.length > 0 && bandEnergies[0] > 0
                    ? Math.min(bandEnergies[0] / (rms + 1), 0.8)
                    : 0;
            return (zcrScore * 0.4) + (centroidScore * 0.3) + (lowEnergyRatio * 0.3);
        }

        public VoiceMetrics(double rms, double zcr, double[] bandEnergies, double spectralCentroid) {
            this.rms = rms;
            this.zcr = zcr;
            this.bandEnergies = bandEnergies;
            this.spectralCentroid = spectralCentroid;
        }
    }
}