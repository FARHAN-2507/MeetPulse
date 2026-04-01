package com.meetpulse.service;

import com.meetpulse.model.MeetingStats;
import com.meetpulse.model.SpeakingSegment;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportExporter {

    public static void exportJson(MeetingStats stats, List<SpeakingSegment> segments,
                                  List<Double> rmsBySecond, double threshold) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "meetpulse_report_" + timestamp + ".json";

        try (FileWriter fw = new FileWriter(filename)) {
            fw.write("{\n");
            fw.write("  \"timestamp\": \"" + timestamp.replace("_", " ") + "\",\n");
            fw.write("  \"duration_sec\": " + stats.getDurationMs() / 1000.0 + ",\n");
            fw.write("  \"total_frames\": " + stats.getTotalFrames() + ",\n");
            fw.write("  \"silent_frames\": " + stats.getSilentFrames() + ",\n");
            fw.write("  \"silence_pct\": " + String.format("%.2f", stats.getSilenceRatio() * 100) + ",\n");
            fw.write("  \"speaking_pct\": " + String.format("%.2f", (1 - stats.getSilenceRatio()) * 100) + ",\n");
            fw.write("  \"avg_rms\": " + String.format("%.2f", stats.getAvgRms()) + ",\n");
            fw.write("  \"peak_rms\": " + String.format("%.2f", stats.getPeakRms()) + ",\n");
            fw.write("  \"threshold\": " + String.format("%.2f", threshold) + ",\n");

            // Segments
            fw.write("  \"segments\": [\n");
            for (int i = 0; i < segments.size(); i++) {
                SpeakingSegment s = segments.get(i);
                fw.write("    {\"start\": " + s.getStartMs() / 1000.0
                        + ", \"end\": " + s.getEndMs() / 1000.0
                        + ", \"duration\": " + s.getDurationMs() / 1000.0 + "}");
                if (i < segments.size() - 1) fw.write(",");
                fw.write("\n");
            }
            fw.write("  ],\n");

            // RMS per second timeline
            fw.write("  \"rms_timeline\": [");
            for (int i = 0; i < rmsBySecond.size(); i++) {
                fw.write(String.format("%.2f", rmsBySecond.get(i)));
                if (i < rmsBySecond.size() - 1) fw.write(", ");
            }
            fw.write("]\n");
            fw.write("}\n");

            System.out.println("📄 Report data saved to: " + filename);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}