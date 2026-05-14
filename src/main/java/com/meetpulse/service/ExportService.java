package com.meetpulse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.meetpulse.audio.AudioCaptureService;
import com.meetpulse.model.MeetingSession;
import com.meetpulse.model.MeetingStats;
import com.meetpulse.model.SpeakingSegment;
import com.meetpulse.processing.SpeakerTurnDetector;
import com.meetpulse.report.PdfReportGenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExportService {

    public enum ExportFormat { PDF, JSON, CSV }

    private final SessionHistoryManager historyManager;

    public ExportService() {
        this.historyManager = new SessionHistoryManager();
    }

    public void exportSession(AudioCaptureService service, String filePath, ExportFormat format) throws IOException {
        switch (format) {
            case PDF -> exportToPdf(service, filePath);
            case JSON -> exportToJson(service, filePath);
            case CSV -> exportToCsv(service, filePath);
        }
    }

    private void exportToPdf(AudioCaptureService service, String filePath) throws IOException {
        try {
            PdfReportGenerator pdfGenerator = new PdfReportGenerator();
            pdfGenerator.generate(service, filePath);
        } catch (Exception e) {
            throw new IOException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    public void exportSessionData(MeetingSession session, String filePath, ExportFormat format) throws IOException {
        switch (format) {
            case JSON -> exportSessionToJson(session, filePath);
            case CSV -> exportSessionToCsv(session, filePath);
            case PDF -> throw new IOException("PDF export from session data not supported. Use live export.");
        }
    }

    private void exportToJson(AudioCaptureService service, String filePath) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("exportTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        data.put("duration", getSessionDuration(service));
        data.put("totalFrames", service.getAnalyzer().getLiveFrameCount());
        data.put("speakingRatio", service.getAnalyzer().getLiveSpeakingPct() / 100.0);
        data.put("segmentCount", service.getAnalyzer().getLiveSegmentCount());
        data.put("peakRms", service.getAnalyzer().getLivePeakRms());
        data.put("estimatedSpeakers", service.getEstimatedSpeakers());
        data.put("totalTurns", service.getSpeakerTurnCount());
        data.put("noiseFloor", service.getNoiseFloor());
        data.put("threshold", service.getThreshold());
        data.put("liveZcr", service.getLiveZcr());
        data.put("liveVad", service.getLiveSpeechLikelihood());

        SpeakerTurnDetector.SpeakerAnalysis analysis = service.getSpeakerDetector().getAnalysis();
        data.put("avgTurnDuration", analysis.avgTurnDurationMs);
        data.put("turnsPerMinute", analysis.turnsPerMinute);
        data.put("voiceLevelVariance", analysis.voiceLevelVariance);
        data.put("avgVoiceLevel", analysis.avgVoiceLevel);

        Map<String, Object> qualityMetrics = calculateQualityMetrics(service);

        data.put("qualityMetrics", qualityMetrics);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File(filePath), data);
    }

    private long getSessionDuration(AudioCaptureService service) {
        MeetingStats stats = service.getAnalyzer().summarize();
        return stats != null ? stats.getDurationMs() : 0;
    }

    private void exportToCsv(AudioCaptureService service, String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("Metric,Value,Unit");

            writer.println("Duration," + (getSessionDuration(service) / 1000.0) + ",seconds");
            writer.println("Total Frames," + service.getAnalyzer().getLiveFrameCount() + ",frames");
            writer.println("Speaking Ratio," + (service.getAnalyzer().getLiveSpeakingPct() / 100.0) + ",%");
            writer.println("Segment Count," + service.getAnalyzer().getLiveSegmentCount() + ",segments");
            writer.println("Peak RMS," + service.getAnalyzer().getLivePeakRms() + ",RMS");
            writer.println("Estimated Speakers," + service.getEstimatedSpeakers() + ",count");
            writer.println("Total Turns," + service.getSpeakerTurnCount() + ",turns");
            writer.println("Noise Floor," + service.getNoiseFloor() + ",RMS");
            writer.println("Threshold," + service.getThreshold() + ",RMS");
            writer.println("Average ZCR," + service.getLiveZcr() + ",rate");
            writer.println("Average VAD," + (service.getLiveSpeechLikelihood() * 100) + ",%");

            SpeakerTurnDetector.SpeakerAnalysis analysis = service.getSpeakerDetector().getAnalysis();
            writer.println("Avg Turn Duration," + analysis.avgTurnDurationMs + ",ms");
            writer.println("Turns Per Minute," + analysis.turnsPerMinute + ",tpm");
            writer.println("Voice Level Variance," + analysis.voiceLevelVariance + ",RMS^2");
            writer.println("Average Voice Level," + analysis.avgVoiceLevel + ",RMS");

            Map<String, Object> qualityMetrics = calculateQualityMetrics(service);
            for (Map.Entry<String, Object> entry : qualityMetrics.entrySet()) {
                writer.println(entry.getKey() + "," + entry.getValue() + ",score");
            }
        }
    }

    private void exportSessionToJson(MeetingSession session, String filePath) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", session.getId());
        data.put("timestamp", session.getFormattedDate());
        data.put("duration", session.getDurationMs() / 1000.0);
        data.put("speakingRatio", session.getSpeakingRatio());
        data.put("qualityScore", session.getQualityScore());
        data.put("estimatedWpm", session.getEstimatedWpm());
        data.put("speakerCount", session.getSpeakerCount());
        data.put("totalTurns", session.getTotalTurns());
        data.put("peakRms", session.getPeakRms());
        data.put("noiseFloor", session.getAvgNoiseFloor());
        data.put("threshold", session.getAvgThreshold());

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File(filePath), data);
    }

    private void exportSessionToCsv(MeetingSession session, String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("Session Summary - " + session.getFormattedDate());
            writer.println();
            writer.println("Metric,Value");
            writer.println("Session ID," + session.getId());
            writer.println("Duration (seconds)," + (session.getDurationMs() / 1000.0));
            writer.println("Speaking Ratio (%)," + (session.getSpeakingRatio() * 100));
            writer.println("Quality Score," + session.getQualityScore());
            writer.println("Estimated WPM," + session.getEstimatedWpm());
            writer.println("Speaker Count," + session.getSpeakerCount());
            writer.println("Total Turns," + session.getTotalTurns());
            writer.println("Average Turn Duration (ms)," + session.getAvgTurnDuration());
            writer.println("Peak RMS," + session.getPeakRms());
            writer.println("Average Noise Floor," + session.getAvgNoiseFloor());
            writer.println("Average Threshold," + session.getAvgThreshold());
        }
    }

    private Map<String, Object> calculateQualityMetrics(AudioCaptureService service) {
        Map<String, Object> metrics = new LinkedHashMap<>();

        double speakingRatio = service.getAnalyzer().getLiveSpeakingPct() / 100.0;
        int speakers = service.getEstimatedSpeakers();
        int turns = service.getSpeakerTurnCount();
        double duration = getSessionDuration(service) / 60000.0;
        double turnsPerMinute = duration > 0 ? turns / duration : 0;

        double speakingScore = Math.min(speakingRatio / 0.5, 1.0) * 30;
        double engagementScore = speakers > 1 ? Math.min(turnsPerMinute / 3.0, 1.0) * 25 : 15;
        double balanceScore = speakers > 1 ? 20 : (speakingRatio > 0.3 && speakingRatio < 0.7 ? 20 : 10);
        double activityScore = Math.min(turns / 10.0, 1.0) * 25;

        double qualityScore = speakingScore + engagementScore + balanceScore + activityScore;

        double avgZcr = service.getLiveZcr();
        double estimatedWpm = avgZcr > 0 ? (avgZcr * 44100 / 60.0) * 15 : 120;

        metrics.put("qualityScore", Math.round(qualityScore * 10.0) / 10.0);
        metrics.put("speakingScore", Math.round(speakingScore * 10.0) / 10.0);
        metrics.put("engagementScore", Math.round(engagementScore * 10.0) / 10.0);
        metrics.put("balanceScore", Math.round(balanceScore * 10.0) / 10.0);
        metrics.put("activityScore", Math.round(activityScore * 10.0) / 10.0);
        metrics.put("estimatedWpm", Math.round(estimatedWpm));

        return metrics;
    }

    public SessionHistoryManager getHistoryManager() {
        return historyManager;
    }

    public double calculateQualityScore(AudioCaptureService service) {
        Map<String, Object> metrics = calculateQualityMetrics(service);
        return (double) metrics.get("qualityScore");
    }

    public double calculateWpm(AudioCaptureService service) {
        double avgZcr = service.getLiveZcr();
        double speakingRatio = service.getAnalyzer().getLiveSpeakingPct() / 100.0;

        double baseWpm = avgZcr > 0 ? (avgZcr * 44100 / 60.0) * 15 : 120;
        double adjustedWpm = baseWpm * speakingRatio;

        return Math.round(adjustedWpm);
    }
}