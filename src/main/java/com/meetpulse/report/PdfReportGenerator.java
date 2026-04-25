package com.meetpulse.report;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.meetpulse.audio.AudioCaptureService;
import com.meetpulse.model.MeetingStats;
import com.meetpulse.model.SpeakingSegment;
import com.meetpulse.service.MeetingAnalyzer;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.*;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PdfReportGenerator {

    // ── Palette (light theme) ─────────────────────────────────────────────
    private static final BaseColor C_WHITE      = BaseColor.WHITE;
    private static final BaseColor C_PAGE_BG    = new BaseColor(248, 249, 252);
    private static final BaseColor C_INK        = new BaseColor(15,  23,  42);
    private static final BaseColor C_INK2       = new BaseColor(51,  65,  85);
    private static final BaseColor C_MUTED      = new BaseColor(100, 116, 139);
    private static final BaseColor C_LIGHT      = new BaseColor(241, 245, 249);
    private static final BaseColor C_BORDER     = new BaseColor(226, 232, 240);
    private static final BaseColor C_ACCENT     = new BaseColor(59,  130, 246);
    private static final BaseColor C_ACCENT_LT  = new BaseColor(219, 234, 254);
    private static final BaseColor C_TEAL       = new BaseColor(16,  185, 129);
    private static final BaseColor C_TEAL_LT    = new BaseColor(209, 250, 229);
    private static final BaseColor C_AMBER      = new BaseColor(245, 158, 11);
    private static final BaseColor C_AMBER_LT   = new BaseColor(254, 243, 199);
    private static final BaseColor C_RED        = new BaseColor(239, 68,  68);
    private static final BaseColor C_RED_LT     = new BaseColor(254, 226, 226);
    private static final BaseColor C_PURPLE     = new BaseColor(139, 92,  246);
    private static final BaseColor C_COVER_TOP  = new BaseColor(15,  23,  42);
    private static final BaseColor C_COVER_BOT  = new BaseColor(30,  58,  138);

    // AWT colours for JFreeChart
    private static final java.awt.Color AWT_BG      = new java.awt.Color(255, 255, 255);
    private static final java.awt.Color AWT_ACCENT  = new java.awt.Color(59,  130, 246);
    private static final java.awt.Color AWT_TEAL    = new java.awt.Color(16,  185, 129);
    private static final java.awt.Color AWT_AMBER   = new java.awt.Color(245, 158, 11);
    private static final java.awt.Color AWT_RED     = new java.awt.Color(239, 68,  68);
    private static final java.awt.Color AWT_MUTED   = new java.awt.Color(148, 163, 184);
    private static final java.awt.Color AWT_INK     = new java.awt.Color(15,  23,  42);
    private static final java.awt.Color AWT_LIGHT   = new java.awt.Color(248, 249, 252);
    private static final java.awt.Color AWT_BORDER  = new java.awt.Color(226, 232, 240);
    private static final java.awt.Color AWT_GRID    = new java.awt.Color(241, 245, 249);

    private static final float PW = PageSize.A4.getWidth();
    private static final float PH = PageSize.A4.getHeight();
    private static final float ML = 45f;
    private static final float MR = 45f;
    private static final float CW = PW - ML - MR;

    // ── Fonts ─────────────────────────────────────────────────────────────
    private static final Font F_COVER_TITLE = new Font(Font.FontFamily.HELVETICA, 36, Font.BOLD,   C_WHITE);
    private static final Font F_COVER_SUB   = new Font(Font.FontFamily.HELVETICA, 13, Font.NORMAL, new BaseColor(148, 163, 184));
    private static final Font F_COVER_BADGE = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   C_WHITE);
    private static final Font F_SEC_TITLE   = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD,   C_INK);
    private static final Font F_SEC_SUB     = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, C_MUTED);
    private static final Font F_BODY        = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, C_INK2);
    private static final Font F_BODY_BOLD   = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   C_INK);
    private static final Font F_CAPTION     = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, C_MUTED);
    private static final Font F_KPI_VAL     = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD,   C_INK);
    private static final Font F_KPI_LBL     = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD,   C_MUTED);
    private static final Font F_TBL_HDR     = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   C_WHITE);
    private static final Font F_TBL_CELL    = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, C_INK2);
    private static final Font F_TBL_FOOT    = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD,   C_INK);
    private static final Font F_MONO        = new Font(Font.FontFamily.COURIER,    9, Font.NORMAL, C_ACCENT);

    // ── Entry point ───────────────────────────────────────────────────────
    public void generate(AudioCaptureService service, String outputPath) throws Exception {
        MeetingAnalyzer analyzer = service.getAnalyzer();
        MeetingStats    stats    = analyzer.summarize();
        List<Double>    timeline = analyzer.getRmsBySecond();
        double          thresh   = service.getThreshold();
        String          ts       = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy  •  HH:mm"));

        Document doc = new Document(PageSize.A4, ML, MR, 36f, 48f);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(outputPath));

        // Page event: footer on every page except cover
        writer.setPageEvent(new FooterEvent(ts));

        doc.open();

        // ── Cover page ────────────────────────────────────────────────────
        buildCoverPage(doc, writer, ts, stats);
        doc.newPage();

        // ── Page 2: KPIs + Energy Timeline ───────────────────────────────
        sectionHeader(doc, writer, "Session Overview",
                "Key performance indicators from your meeting session");

        buildKpiStrip(doc, stats);
        space(doc, 18);

        sectionHeader2(doc, "Energy Timeline");
        doc.add(chartCaption("RMS energy per second — amber dashed line marks the silence threshold"));
        doc.add(makeImage(chartTimeline(timeline, thresh), CW, 130));
        space(doc, 20);

        // ── Page 3: Distribution charts ───────────────────────────────────
        sectionHeader(doc, writer, "Audio Distribution",
                "How your session energy was distributed between speech and silence");

        // side by side: donut + histogram
        PdfPTable twoCol = new PdfPTable(2);
        twoCol.setWidthPercentage(100);
        twoCol.setWidths(new float[]{1f, 1.4f});
        twoCol.setSpacingAfter(20);

        PdfPCell donutCell = imgCell(makeImage(chartDonut(stats), 200, 190));
        donutCell.setPaddingRight(12);
        PdfPCell histCell  = imgCell(makeImage(chartHistogram(timeline, thresh), 290, 190));

        twoCol.addCell(donutCell);
        twoCol.addCell(histCell);
        doc.add(twoCol);

        // captions
        PdfPTable capRow = new PdfPTable(2);
        capRow.setWidthPercentage(100);
        capRow.setWidths(new float[]{1f, 1.4f});
        capRow.setSpacingAfter(24);
        capRow.addCell(captionCell("Talk ratio — speaking vs silence"));
        capRow.addCell(captionCell("RMS value distribution with threshold marker"));
        doc.add(capRow);

        // ── Engagement profile ───────────────────────────────────────────
        sectionHeader(doc, writer, "Engagement Profile",
                "How energy evolved from opening to close, and whether momentum sustained");

        PdfPTable profile = new PdfPTable(2);
        profile.setWidthPercentage(100);
        profile.setWidths(new float[]{1f, 1f});
        profile.setSpacingAfter(12);
        profile.addCell(imgCell(makeImage(chartPhaseEnergy(timeline, thresh), 250, 180)));
        profile.addCell(imgCell(makeImage(chartMomentum(timeline, thresh), 250, 180)));
        doc.add(profile);

        PdfPTable profileCap = new PdfPTable(2);
        profileCap.setWidthPercentage(100);
        profileCap.setWidths(new float[]{1f, 1f});
        profileCap.setSpacingAfter(20);
        profileCap.addCell(captionCell("Phase Energy — opening, middle, closing averages"));
        profileCap.addCell(captionCell("Momentum Trend — raw energy vs rolling baseline"));
        doc.add(profileCap);

        // ── Segments ──────────────────────────────────────────────────────
        sectionHeader(doc, writer, "Speaking Segments",
                "Individual speaking bursts detected during the session");

        List<SpeakingSegment> segs = stats.getSegments();
        if (segs.isEmpty()) {
            doc.add(emptyNotice("No speaking segments were detected in this session."));
        } else {
            // Gantt chart
            doc.add(makeImage(chartGantt(stats), CW,
                    Math.min(200, 40 + segs.size() * 24)));
            doc.add(chartCaption("Each bar represents one speaking burst on the session timeline"));
            space(doc, 14);
            buildSpeakingDynamics(doc, segs, stats.getDurationMs());
        }
        buildTimelineMoments(doc, timeline, thresh);

        // ── Technical Details ─────────────────────────────────────────────
        doc.newPage();
        sectionHeader(doc, writer, "Technical Details",
                "Audio capture configuration and session diagnostics");

        buildTechTable(doc, stats, thresh);
        space(doc, 24);

        // ── Insights box ──────────────────────────────────────────────────
        buildInsightsBox(doc, writer, stats, segs);
        space(doc, 12);
        buildExecutiveActionPlan(doc, stats, timeline, thresh);

        doc.close();
    }

    // ══════════════════════════════════════════════════════════════════════
    // COVER PAGE
    // ══════════════════════════════════════════════════════════════════════
    private void buildCoverPage(Document doc, PdfWriter writer,
                                String ts, MeetingStats stats) throws Exception {
        PdfContentByte cb = writer.getDirectContent();
        double score = sessionScore(stats);
        String tier = sessionTier(score);
        BaseColor scoreColor = score >= 80 ? C_TEAL : (score >= 60 ? C_AMBER : C_RED);
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);

        // Light background
        cb.setColorFill(new BaseColor(245, 249, 255));
        cb.rectangle(0, 0, PW, PH);
        cb.fill();

        cb.setColorFill(new BaseColor(226, 238, 255));
        cb.moveTo(0, PH * 0.52f);
        cb.lineTo(PW, PH * 0.48f);
        cb.lineTo(PW, 0);
        cb.lineTo(0, 0);
        cb.fill();

        // Ambient glow circles
        cb.setColorFill(new BaseColor(79, 142, 247, 36));
        cb.circle(PW - 90, PH - 120, 150);
        cb.fill();
        cb.setColorFill(new BaseColor(29, 214, 160, 24));
        cb.circle(110, 120, 120);
        cb.fill();

        // Main hero card
        float cardX = ML;
        float cardY = 122f;
        float cardW = CW;
        float cardH = PH - 220f;
        cb.setColorFill(new BaseColor(255, 255, 255, 245));
        cb.roundRectangle(cardX, cardY, cardW, cardH, 18);
        cb.fill();
        cb.setColorStroke(new BaseColor(186, 202, 226));
        cb.setLineWidth(0.9f);
        cb.roundRectangle(cardX, cardY, cardW, cardH, 18);
        cb.stroke();

        // Header line
        cb.setColorFill(new BaseColor(96, 165, 250));
        cb.rectangle(cardX + 18, cardY + cardH - 34, 54, 3);
        cb.fill();

        // Brand + report title
        cb.setColorFill(new BaseColor(67, 98, 145));
        cb.setFontAndSize(bf, 10);
        cb.beginText();
        cb.showTextAligned(Element.ALIGN_LEFT, "MEETPULSE • PRIVATE AUDIO INTELLIGENCE", cardX + 18, cardY + cardH - 24, 0);
        cb.endText();

        cb.setColorFill(C_INK);
        cb.setFontAndSize(bfBold, 34);
        cb.beginText();
        cb.showTextAligned(Element.ALIGN_LEFT, "Meeting Report", cardX + 18, cardY + cardH - 68, 0);
        cb.endText();

        cb.setColorFill(new BaseColor(84, 102, 128));
        cb.setFontAndSize(bf, 11);
        cb.beginText();
        cb.showTextAligned(Element.ALIGN_LEFT, ts, cardX + 18, cardY + cardH - 88, 0);
        cb.endText();

        // Tier badge
        float badgeW = 78f;
        float badgeH = 20f;
        float badgeX = cardX + cardW - badgeW - 22f;
        float badgeY = cardY + cardH - 34f;
        cb.setColorFill(new BaseColor(226, 236, 248));
        cb.roundRectangle(badgeX, badgeY, badgeW, badgeH, 10);
        cb.fill();
        cb.setColorFill(C_INK);
        cb.setFontAndSize(bfBold, 9);
        cb.beginText();
        cb.showTextAligned(Element.ALIGN_CENTER, tier.toUpperCase(), badgeX + badgeW / 2, badgeY + 7, 0);
        cb.endText();

        // Right score gauge
        float gaugeCx = cardX + cardW - 118f;
        float gaugeCy = cardY + cardH - 138f;
        float r = 58f;
        cb.setColorStroke(new BaseColor(210, 220, 236));
        cb.setLineWidth(8f);
        cb.arc(gaugeCx - r, gaugeCy - r, gaugeCx + r, gaugeCy + r, 150, 240);
        cb.stroke();

        cb.setColorStroke(scoreColor);
        cb.setLineWidth(8f);
        cb.arc(gaugeCx - r, gaugeCy - r, gaugeCx + r, gaugeCy + r, 150, (float) (240.0 * (score / 100.0)));
        cb.stroke();

        cb.setColorFill(C_INK);
        cb.setFontAndSize(bfBold, 28);
        cb.beginText();
        cb.showTextAligned(Element.ALIGN_CENTER, String.format("%.0f", score), gaugeCx, gaugeCy - 10, 0);
        cb.endText();
        cb.setColorFill(new BaseColor(95, 112, 139));
        cb.setFontAndSize(bf, 9);
        cb.beginText();
        cb.showTextAligned(Element.ALIGN_CENTER, "SESSION SCORE", gaugeCx, gaugeCy - 24, 0);
        cb.endText();

        // Privacy promise
        cb.setColorFill(new BaseColor(84, 102, 128));
        cb.setFontAndSize(bf, 9);
        cb.beginText();
        cb.showTextAligned(Element.ALIGN_LEFT, "No transcript stored • Signal-level analytics only", cardX + 18, cardY + cardH - 120, 0);
        cb.endText();

        // Metric tiles
        float tilesY = cardY + 20f;
        float tileH = 76f;
        float gap = 10f;
        float tileW = (cardW - (gap * 5)) / 4;
        String[][] coverStats = {
                { String.format("%.0fs", stats.getDurationMs() / 1000.0), "DURATION" },
                { String.format("%.1f%%", (1 - stats.getSilenceRatio()) * 100), "SPEAKING" },
                { String.valueOf(stats.getSegments().size()), "SEGMENTS" },
                { String.format("%.0f", stats.getPeakRms()), "PEAK RMS" },
        };
        for (int i = 0; i < coverStats.length; i++) {
            float x = cardX + gap + i * (tileW + gap);
            cb.setColorFill(new BaseColor(248, 252, 255));
            cb.roundRectangle(x, tilesY, tileW, tileH, 9);
            cb.fill();
            cb.setColorStroke(new BaseColor(200, 213, 234));
            cb.setLineWidth(0.6f);
            cb.roundRectangle(x, tilesY, tileW, tileH, 9);
            cb.stroke();

            cb.setColorFill(C_INK);
            cb.setFontAndSize(bfBold, 22);
            cb.beginText();
            cb.showTextAligned(Element.ALIGN_CENTER, coverStats[i][0], x + tileW / 2, tilesY + 42, 0);
            cb.endText();

            cb.setColorFill(new BaseColor(95, 112, 139));
            cb.setFontAndSize(bf, 8);
            cb.beginText();
            cb.showTextAligned(Element.ALIGN_CENTER, coverStats[i][1], x + tileW / 2, tilesY + 18, 0);
            cb.endText();
        }
        cb.setColorFill(new BaseColor(91, 108, 133));
        cb.setFontAndSize(bfBold, 10);
        cb.beginText();
        cb.showTextAligned(Element.ALIGN_RIGHT, "MeetPulse", PW - MR, PH - 26, 0);
        cb.endText();
    }

    private double sessionScore(MeetingStats s) {
        double speakingPct = (1.0 - s.getSilenceRatio()) * 100.0;
        double speakScore = clampScore(100.0 - Math.abs(55.0 - speakingPct) * 1.8);
        double durationMin = s.getDurationMs() / 60000.0;
        double segRate = durationMin > 0 ? s.getSegments().size() / durationMin : 0;
        double cadenceScore = clampScore(segRate * 18.0);
        double avgSeg = s.getSegments().isEmpty() ? 0.0
                : s.getSegments().stream().mapToLong(SpeakingSegment::getDurationMs).average().orElse(0) / 1000.0;
        double segScore = clampScore(100.0 - Math.abs(3.5 - avgSeg) * 16.0);
        return (speakScore * 0.50) + (cadenceScore * 0.30) + (segScore * 0.20);
    }

    private String sessionTier(double score) {
        if (score >= 80) return "Premium";
        if (score >= 60) return "Strong";
        return "Basic";
    }

    private double clampScore(double v) {
        if (v < 0) return 0;
        if (v > 100) return 100;
        return v;
    }

    // ══════════════════════════════════════════════════════════════════════
    // SECTION HEADERS
    // ══════════════════════════════════════════════════════════════════════
    private void sectionHeader(Document doc, PdfWriter writer,
                               String title, String subtitle) throws Exception {
        PdfContentByte cb = writer.getDirectContent();

        // Blue left accent bar
        float y = writer.getVerticalPosition(false);
        cb.setColorFill(C_ACCENT);
        cb.rectangle(ML, y - 2, 4, 28);
        cb.fill();

        // Light background strip
        cb.setColorFill(new BaseColor(248, 249, 252));
        cb.rectangle(ML + 4, y - 2, CW - 4, 28);
        cb.fill();

        Paragraph p = new Paragraph();
        p.setIndentationLeft(14);
        p.add(new Chunk(title + "\n", F_SEC_TITLE));
        p.add(new Chunk(subtitle, F_SEC_SUB));
        p.setSpacingBefore(0);
        p.setSpacingAfter(14);
        doc.add(p);
    }

    private void sectionHeader2(Document doc, String title) throws Exception {
        Paragraph p = new Paragraph(title,
                new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, C_INK));
        p.setSpacingBefore(4);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    // ══════════════════════════════════════════════════════════════════════
    // KPI STRIP
    // ══════════════════════════════════════════════════════════════════════
    private void buildKpiStrip(Document doc, MeetingStats s) throws Exception {
        double speakSec = s.getSegments().stream()
                .mapToLong(SpeakingSegment::getDurationMs).sum() / 1000.0;
        double avgSeg = s.getSegments().isEmpty() ? 0
                : speakSec / s.getSegments().size();

        Object[][] kpis = {
                { String.format("%.0fs",  s.getDurationMs() / 1000.0), "Duration",      C_ACCENT,  C_ACCENT_LT },
                { String.format("%.1f%%", (1-s.getSilenceRatio())*100), "Speaking",     C_TEAL,    C_TEAL_LT   },
                { String.format("%.1f%%", s.getSilenceRatio()*100),     "Silence",      C_MUTED,   C_LIGHT     },
                { String.valueOf(s.getSegments().size()),                "Segments",     C_PURPLE,  new BaseColor(237,233,254) },
                { String.format("%.1fs",  avgSeg),                      "Avg Segment",  C_AMBER,   C_AMBER_LT  },
                { String.format("%.0f",   s.getPeakRms()),              "Peak RMS",     C_RED,     C_RED_LT    },
        };

        PdfPTable table = new PdfPTable(kpis.length);
        table.setWidthPercentage(100);
        table.setSpacingAfter(4);

        for (Object[] kpi : kpis) {
            String    val   = (String)    kpi[0];
            String    lbl   = (String)    kpi[1];
            BaseColor color = (BaseColor) kpi[2];
            BaseColor bg    = (BaseColor) kpi[3];

            Font fVal = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, color);
            Font fLbl = new Font(Font.FontFamily.HELVETICA,  8, Font.BOLD, color);

            Paragraph valP = new Paragraph(val, fVal);
            valP.setAlignment(Element.ALIGN_CENTER);
            Paragraph lblP = new Paragraph(lbl.toUpperCase(), fLbl);
            lblP.setAlignment(Element.ALIGN_CENTER);

            // thin colored top border
            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setBackgroundColor(bg);
            cell.setPadding(12);
            cell.setPaddingTop(14);
            cell.addElement(valP);
            cell.addElement(lblP);

            // top accent stripe via border
            cell.setBorderWidthTop(3);
            cell.setBorderColorTop(color);
            cell.setBorderWidthBottom(0);
            cell.setBorderWidthLeft(0);
            cell.setBorderWidthRight(0);

            table.addCell(cell);
        }
        doc.add(table);
    }

    // ══════════════════════════════════════════════════════════════════════
    // SEGMENTS TABLE
    // ══════════════════════════════════════════════════════════════════════
    private void buildSpeakingDynamics(Document doc, List<SpeakingSegment> segs,
                                       long durationMs) throws Exception {
        double totalSec = durationMs / 1000.0;
        long totalSpeakMs = 0;
        long longestMs = 0;
        List<Double> lens = new ArrayList<>();

        for (SpeakingSegment s : segs) {
            long d = Math.max(0, s.getDurationMs());
            totalSpeakMs += d;
            longestMs = Math.max(longestMs, d);
            lens.add(d / 1000.0);
        }
        lens.sort(Comparator.naturalOrder());
        double medianSeg = lens.isEmpty() ? 0 : (lens.size() % 2 == 1
                ? lens.get(lens.size() / 2)
                : (lens.get((lens.size() / 2) - 1) + lens.get(lens.size() / 2)) / 2.0);
        double segmentsPerMin = totalSec > 0 ? (segs.size() / (totalSec / 60.0)) : 0;
        double talkCoverage = totalSec > 0 ? ((totalSpeakMs / 1000.0) / totalSec) * 100.0 : 0;

        sectionHeader2(doc, "Speaking Dynamics");
        PdfPTable k = new PdfPTable(4);
        k.setWidthPercentage(100);
        k.setSpacingAfter(14);
        k.setWidths(new float[]{1f, 1f, 1f, 1f});
        k.addCell(statPill(String.format("%.1f%%", talkCoverage), "Talk Coverage", C_TEAL, C_TEAL_LT));
        k.addCell(statPill(String.format("%.1f/min", segmentsPerMin), "Segments Rate", C_ACCENT, C_ACCENT_LT));
        k.addCell(statPill(String.format("%.1fs", medianSeg), "Median Segment", C_AMBER, C_AMBER_LT));
        k.addCell(statPill(String.format("%.1fs", longestMs / 1000.0), "Longest Burst", C_RED, C_RED_LT));
        doc.add(k);
    }

    private void buildTimelineMoments(Document doc, List<Double> timeline, double threshold) throws Exception {
        sectionHeader2(doc, "Key Timeline Moments");
        if (timeline == null || timeline.isEmpty()) {
            doc.add(emptyNotice("No timeline data available."));
            return;
        }

        int peakSec = 0;
        double peakVal = -1;
        int quietStart = -1;
        int quietBestStart = -1;
        int quietBestLen = 0;
        List<Integer> above = new ArrayList<>();

        for (int i = 0; i < timeline.size(); i++) {
            double v = timeline.get(i);
            if (v > peakVal) { peakVal = v; peakSec = i; }
            if (v >= threshold) above.add(i);

            boolean silent = v < threshold;
            if (silent && quietStart < 0) quietStart = i;
            if (!silent && quietStart >= 0) {
                int len = i - quietStart;
                if (len > quietBestLen) { quietBestLen = len; quietBestStart = quietStart; }
                quietStart = -1;
            }
        }
        if (quietStart >= 0) {
            int len = timeline.size() - quietStart;
            if (len > quietBestLen) { quietBestLen = len; quietBestStart = quietStart; }
        }

        PdfPTable list = new PdfPTable(1);
        list.setWidthPercentage(100);
        list.setSpacingAfter(14);
        list.addCell(momentCell("Peak energy",
                String.format("Highest intensity at %ds (RMS %.0f).", peakSec, peakVal), C_RED));

        if (!above.isEmpty()) {
            int firstActive = above.get(0);
            int lastActive = above.get(above.size() - 1);
            list.addCell(momentCell("Active range",
                    String.format("Sustained above-threshold activity from %ds to %ds.", firstActive, lastActive), C_TEAL));
        } else {
            list.addCell(momentCell("Activity warning",
                    "No timeline points exceeded the calibrated threshold. Check mic level or calibration silence.", C_AMBER));
        }

        if (quietBestLen > 0) {
            list.addCell(momentCell("Longest quiet window",
                    String.format("%ds of low activity starting at %ds.", quietBestLen, quietBestStart), C_ACCENT));
        }

        doc.add(list);
    }

    // ══════════════════════════════════════════════════════════════════════
    // TECH TABLE
    // ══════════════════════════════════════════════════════════════════════
    private void buildTechTable(Document doc, MeetingStats stats,
                                double thresh) throws Exception {
        String[][] rows = {
                {"Audio Format",      "44,100 Hz  •  16-bit PCM  •  Mono"},
                {"Buffer Size",       "4,096 bytes per chunk"},
                {"Silence Threshold", String.format("%.2f RMS  (auto-calibrated)", thresh)},
                {"Average RMS",       String.format("%.2f", stats.getAvgRms())},
                {"Peak RMS",          String.format("%.2f", stats.getPeakRms())},
                {"Total Frames",      String.format("%,d", stats.getTotalFrames())},
                {"Silent Frames",     String.format("%,d  (%.1f%%)", stats.getSilentFrames(),
                        stats.getSilenceRatio() * 100)},
                {"Speaking Frames",   String.format("%,d  (%.1f%%)",
                        stats.getTotalFrames() - stats.getSilentFrames(),
                        (1 - stats.getSilenceRatio()) * 100)},
                {"Session Duration",  String.format("%.1f seconds", stats.getDurationMs() / 1000.0)},
        };

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(82);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        t.setWidths(new float[]{1.3f, 2f});
        t.setSpacingAfter(16);

        // header row
        PdfPCell hk = new PdfPCell(new Phrase("Parameter", F_TBL_HDR));
        PdfPCell hv = new PdfPCell(new Phrase("Value",     F_TBL_HDR));
        for (PdfPCell c : new PdfPCell[]{hk, hv}) {
            c.setBackgroundColor(C_INK);
            c.setPadding(8); c.setPaddingLeft(12);
            c.setBorder(Rectangle.NO_BORDER);
        }
        t.addCell(hk); t.addCell(hv);

        for (int i = 0; i < rows.length; i++) {
            BaseColor bg = i % 2 == 0 ? C_WHITE : C_PAGE_BG;
            t.addCell(tblCell(rows[i][0], bg, F_BODY_BOLD));
            t.addCell(tblCell(rows[i][1], bg, F_MONO));
        }
        doc.add(t);
    }

    // ══════════════════════════════════════════════════════════════════════
    // INSIGHTS BOX
    // ══════════════════════════════════════════════════════════════════════
    private void buildInsightsBox(Document doc, PdfWriter writer,
                                  MeetingStats stats,
                                  List<SpeakingSegment> segs) throws Exception {
        double speakPct  = (1 - stats.getSilenceRatio()) * 100;
        double avgSegSec = segs.isEmpty() ? 0
                : segs.stream().mapToLong(SpeakingSegment::getDurationMs).average().orElse(0) / 1000.0;
        long longestMs = segs.stream().mapToLong(SpeakingSegment::getDurationMs).max().orElse(0);
        double density = stats.getDurationMs() > 0 ? (segs.size() / (stats.getDurationMs() / 60000.0)) : 0;

        // pick an insight based on the data
        String insight;
        BaseColor insightColor;
        BaseColor insightBg;

        if (speakPct < 10) {
            insight      = "Very low speaking activity detected (" + String.format("%.1f%%", speakPct) +
                    "). The session was mostly silent — consider if the microphone captured the right input.";
            insightColor = C_AMBER;
            insightBg    = C_AMBER_LT;
        } else if (speakPct > 70) {
            insight      = "High speaking activity (" + String.format("%.1f%%", speakPct) +
                    "). Excellent engagement — nearly continuous speech was detected throughout the session.";
            insightColor = C_TEAL;
            insightBg    = C_TEAL_LT;
        } else if (avgSegSec < 0.5) {
            insight      = "Speaking segments are very short (avg " + String.format("%.1fs", avgSegSec) +
                    "). This may indicate fragmented speech, background clicks, or a threshold that needs adjusting.";
            insightColor = C_RED;
            insightBg    = C_RED_LT;
        } else if (longestMs > 15000) {
            insight      = "A long monologue burst was detected (" + String.format("%.1fs", longestMs / 1000.0) +
                    "). Consider introducing checkpoints to improve interaction quality.";
            insightColor = C_AMBER;
            insightBg    = C_AMBER_LT;
        } else if (density > 12) {
            insight      = "High exchange frequency (" + String.format("%.1f segments/min", density) +
                    "). The conversation had fast turn-taking and strong cadence.";
            insightColor = C_TEAL;
            insightBg    = C_TEAL_LT;
        } else {
            insight      = "Good session profile — " + String.format("%.1f%%", speakPct) +
                    " speaking with " + segs.size() + " distinct segments averaging " +
                    String.format("%.1f seconds", avgSegSec) + " each.";
            insightColor = C_ACCENT;
            insightBg    = C_ACCENT_LT;
        }

        // Draw insight card
        PdfPTable card = new PdfPTable(1);
        card.setWidthPercentage(100);
        card.setSpacingBefore(8);

        Font fInsightTitle = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, insightColor);
        Font fInsightBody  = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, C_INK2);

        Paragraph content = new Paragraph();
        content.add(new Chunk("Session Insight  ", fInsightTitle));
        content.add(new Chunk("\n" + insight, fInsightBody));
        content.setLeading(16);

        PdfPCell cell = new PdfPCell(content);
        cell.setBackgroundColor(insightBg);
        cell.setBorder(Rectangle.LEFT);
        cell.setBorderWidthLeft(4);
        cell.setBorderColorLeft(insightColor);
        cell.setPadding(14);
        card.addCell(cell);
        doc.add(card);
    }

    private void buildExecutiveActionPlan(Document doc, MeetingStats stats,
                                          List<Double> timeline, double threshold) throws Exception {
        sectionHeader2(doc, "Executive Action Plan");

        String p1Title = "Priority 1 — Calibration Confidence";
        String p1Body;
        if (timeline.stream().noneMatch(v -> v >= threshold)) {
            p1Body = "No timeline samples crossed threshold. Re-run calibration in silence, then check mic gain to avoid under-reporting speaking activity.";
        } else {
            p1Body = "Calibration appears functional. Keep first 3-4 seconds silent at each session start for stable baseline estimation.";
        }

        String p2Title = "Priority 2 — Participation Shape";
        String p2Body;
        double speakingPct = (1 - stats.getSilenceRatio()) * 100.0;
        if (speakingPct < 20) {
            p2Body = "Low participation detected. Add agenda checkpoints every 8-10 minutes and explicit prompts to increase interaction.";
        } else if (speakingPct > 75) {
            p2Body = "Very high talking density detected. Introduce short pauses to improve comprehension and reduce fatigue.";
        } else {
            p2Body = "Participation density is in a healthy range. Preserve this cadence for similar meeting types.";
        }

        String p3Title = "Priority 3 — Speaking Rhythm";
        String p3Body;
        double avgSegSec = stats.getSegments().isEmpty()
                ? 0 : stats.getSegments().stream().mapToLong(SpeakingSegment::getDurationMs).average().orElse(0) / 1000.0;
        if (avgSegSec < 0.7) {
            p3Body = "Segment rhythm is fragmented. Reduce background noise and avoid interruptive overlaps to improve continuity.";
        } else if (avgSegSec > 8.0) {
            p3Body = "Long speaking bursts detected. Consider periodic handoffs to keep collaboration balanced.";
        } else {
            p3Body = "Speaking rhythm is balanced. Continue with the same facilitation pattern.";
        }

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 1f, 1f});
        t.setSpacingAfter(8);
        t.addCell(actionCard(p1Title, p1Body, C_ACCENT, C_ACCENT_LT));
        t.addCell(actionCard(p2Title, p2Body, C_TEAL, C_TEAL_LT));
        t.addCell(actionCard(p3Title, p3Body, C_AMBER, C_AMBER_LT));
        doc.add(t);
    }

    // ══════════════════════════════════════════════════════════════════════
    // CHARTS
    // ══════════════════════════════════════════════════════════════════════
    private JFreeChart chartTimeline(List<Double> rms, double thresh) {
        XYSeries series = new XYSeries("Energy");
        for (int i = 0; i < rms.size(); i++) series.add(i, rms.get(i));

        XYSeriesCollection ds = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYAreaChart(
                null, "Time (seconds)", "RMS Energy",
                ds, PlotOrientation.VERTICAL, false, false, false
        );
        chart.setBackgroundPaint(AWT_BG);
        chart.setBorderVisible(false);
        chart.setPadding(new org.jfree.chart.ui.RectangleInsets(10, 10, 10, 10));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(AWT_BG);
        plot.setDomainGridlinePaint(AWT_GRID);
        plot.setRangeGridlinePaint(AWT_GRID);
        plot.setDomainGridlineStroke(new BasicStroke(0.8f));
        plot.setRangeGridlineStroke(new BasicStroke(0.8f));
        plot.setOutlineVisible(false);

        XYAreaRenderer rend = new XYAreaRenderer();
        rend.setSeriesPaint(0, new java.awt.Color(59, 130, 246, 80));
        rend.setSeriesOutlinePaint(0, AWT_ACCENT);
        rend.setSeriesOutlineStroke(0, new BasicStroke(1.8f));
        plot.setRenderer(rend);

        // threshold dashed line
        org.jfree.chart.plot.ValueMarker marker =
                new org.jfree.chart.plot.ValueMarker(thresh);
        marker.setPaint(AWT_AMBER);
        marker.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 1f, new float[]{8f, 5f}, 0f));
        marker.setLabel("threshold");
        marker.setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 9));
        marker.setLabelPaint(AWT_AMBER);
        marker.setLabelOffset(new org.jfree.chart.ui.RectangleInsets(4, 4, 0, 0));
        plot.addRangeMarker(marker);

        styleAxis(plot.getDomainAxis(), "Time (seconds)");
        styleAxis(plot.getRangeAxis(), "RMS Energy");

        return chart;
    }

    private JFreeChart chartDonut(MeetingStats s) {
        org.jfree.data.general.DefaultPieDataset ds =
                new org.jfree.data.general.DefaultPieDataset();
        double sp = (1 - s.getSilenceRatio()) * 100;
        double si = s.getSilenceRatio() * 100;
        ds.setValue("Speaking", sp);
        ds.setValue("Silence",  si);

        JFreeChart chart = ChartFactory.createRingChart(
                null, ds, true, false, false
        );
        chart.setBackgroundPaint(AWT_BG);
        chart.setBorderVisible(false);

        RingPlot plot = (RingPlot) chart.getPlot();
        plot.setBackgroundPaint(AWT_BG);
        plot.setOutlineVisible(false);
        plot.setSectionPaint("Speaking", AWT_TEAL);
        plot.setSectionPaint("Silence",  AWT_BORDER);
        plot.setSectionOutlinesVisible(false);
        plot.setSectionDepth(0.38);
        plot.setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
        plot.setLabelPaint(AWT_INK);
        plot.setLabelBackgroundPaint(null);
        plot.setLabelOutlinePaint(null);
        plot.setLabelShadowPaint(null);

        // legend
        chart.getLegend().setBackgroundPaint(AWT_BG);
        chart.getLegend().setItemFont(
                new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
        chart.getLegend().setItemPaint(AWT_INK);
        chart.getLegend().setFrame(
                new org.jfree.chart.block.BlockBorder(AWT_BG));

        return chart;
    }

    private JFreeChart chartHistogram(List<Double> rms, double thresh) {
        double[] vals = rms.stream().mapToDouble(Double::doubleValue).toArray();
        if (vals.length == 0) vals = new double[]{0, 1};

        HistogramDataset ds = new HistogramDataset();
        ds.setType(org.jfree.data.statistics.HistogramType.FREQUENCY);
        ds.addSeries("RMS", vals, 22);

        JFreeChart chart = ChartFactory.createHistogram(
                null, "RMS Value", "Frequency",
                ds, PlotOrientation.VERTICAL, false, false, false
        );
        chart.setBackgroundPaint(AWT_BG);
        chart.setBorderVisible(false);
        chart.setPadding(new org.jfree.chart.ui.RectangleInsets(10, 10, 10, 10));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(AWT_BG);
        plot.setDomainGridlinePaint(AWT_GRID);
        plot.setRangeGridlinePaint(AWT_GRID);
        plot.setOutlineVisible(false);

        XYBarRenderer rend = (XYBarRenderer) plot.getRenderer();
        rend.setSeriesPaint(0, new java.awt.Color(59, 130, 246, 160));
        rend.setShadowVisible(false);
        rend.setBarPainter(new org.jfree.chart.renderer.xy.StandardXYBarPainter());

        // threshold marker
        org.jfree.chart.plot.ValueMarker marker =
                new org.jfree.chart.plot.ValueMarker(thresh);
        marker.setPaint(AWT_AMBER);
        marker.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 1f, new float[]{8f, 5f}, 0f));
        marker.setLabel("threshold");
        marker.setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 9));
        marker.setLabelPaint(AWT_AMBER);
        plot.addDomainMarker(marker);

        styleAxis(plot.getDomainAxis(), "RMS Value");
        styleAxis(plot.getRangeAxis(), "Frequency");

        return chart;
    }

    private JFreeChart chartGantt(MeetingStats stats) {
        List<SpeakingSegment> segs = stats.getSegments();
        double totalSec = stats.getDurationMs() / 1000.0;

        // Build an area chart showing speaking activity as binary signal
        XYSeries active = new XYSeries("Speaking");
        XYSeries silent = new XYSeries("Silence");

        // Add silence baseline across full duration
        active.add(0, 0);

        for (SpeakingSegment sg : segs) {
            double s = sg.getStartMs() / 1000.0;
            double e = sg.getEndMs()   / 1000.0;
            active.add(s - 0.001, 0);
            active.add(s,         1);
            active.add(e,         1);
            active.add(e + 0.001, 0);
        }
        active.add(totalSec, 0);

        XYSeriesCollection ds = new XYSeriesCollection();
        ds.addSeries(active);

        JFreeChart chart = ChartFactory.createXYAreaChart(
                null, "Time (seconds)", null,
                ds, PlotOrientation.VERTICAL, false, false, false
        );
        chart.setBackgroundPaint(AWT_BG);
        chart.setBorderVisible(false);
        chart.setPadding(new org.jfree.chart.ui.RectangleInsets(8, 8, 8, 8));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(AWT_BG);
        plot.setDomainGridlinePaint(AWT_GRID);
        plot.setOutlineVisible(false);

        XYAreaRenderer rend = new XYAreaRenderer();
        rend.setSeriesPaint(0, new java.awt.Color(16, 185, 129, 120));
        rend.setSeriesOutlinePaint(0, AWT_TEAL);
        rend.setSeriesOutlineStroke(0, new BasicStroke(1.5f));
        plot.setRenderer(rend);

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setRange(0, 1.3);
        yAxis.setVisible(false);

        styleAxis(plot.getDomainAxis(), "Time (seconds)");

        return chart;
    }

    private JFreeChart chartPhaseEnergy(List<Double> timeline, double threshold) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        double[] phases = phaseAverages(timeline);
        String[] labels = {"Opening", "Middle", "Closing"};

        for (int i = 0; i < phases.length; i++) {
            ds.addValue(phases[i], "Avg RMS", labels[i]);
            ds.addValue(threshold, "Threshold", labels[i]);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                null, null, "RMS", ds,
                PlotOrientation.VERTICAL, true, false, false
        );
        chart.setBackgroundPaint(AWT_BG);
        chart.setBorderVisible(false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(AWT_BG);
        plot.setRangeGridlinePaint(AWT_GRID);
        plot.setOutlineVisible(false);

        BarRenderer br = (BarRenderer) plot.getRenderer();
        br.setSeriesPaint(0, new java.awt.Color(59, 130, 246, 190));
        br.setSeriesPaint(1, new java.awt.Color(245, 158, 11, 150));
        br.setShadowVisible(false);
        br.setMaximumBarWidth(0.18);

        NumberAxis y = (NumberAxis) plot.getRangeAxis();
        y.setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 9));
        y.setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 8));
        y.setTickLabelPaint(new java.awt.Color(100, 116, 139));
        y.setAxisLinePaint(AWT_BORDER);
        y.setTickMarkPaint(AWT_BORDER);

        CategoryAxis x = plot.getDomainAxis();
        x.setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 9));
        x.setTickLabelPaint(AWT_INK);
        x.setAxisLinePaint(AWT_BORDER);

        chart.getLegend().setBackgroundPaint(AWT_BG);
        chart.getLegend().setItemFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 9));
        chart.getLegend().setItemPaint(AWT_INK);
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(AWT_BG));
        return chart;
    }

    private JFreeChart chartMomentum(List<Double> timeline, double threshold) {
        XYSeries raw = new XYSeries("Raw");
        XYSeries roll = new XYSeries("Rolling Avg");
        int w = 6;
        double sum = 0;

        for (int i = 0; i < timeline.size(); i++) {
            double v = timeline.get(i);
            raw.add(i, v);
            sum += v;
            if (i >= w) sum -= timeline.get(i - w);
            double avg = sum / Math.min(i + 1, w);
            roll.add(i, avg);
        }

        XYSeriesCollection ds = new XYSeriesCollection();
        ds.addSeries(raw);
        ds.addSeries(roll);

        JFreeChart chart = ChartFactory.createXYLineChart(
                null, "Time (s)", "RMS", ds,
                PlotOrientation.VERTICAL, true, false, false
        );
        chart.setBackgroundPaint(AWT_BG);
        chart.setBorderVisible(false);
        chart.setPadding(new org.jfree.chart.ui.RectangleInsets(6, 6, 6, 6));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(AWT_BG);
        plot.setDomainGridlinePaint(AWT_GRID);
        plot.setRangeGridlinePaint(AWT_GRID);
        plot.setOutlineVisible(false);

        XYLineAndShapeRenderer r = new XYLineAndShapeRenderer(true, false);
        r.setSeriesPaint(0, new java.awt.Color(148, 163, 184, 160));
        r.setSeriesStroke(0, new BasicStroke(1.1f));
        r.setSeriesPaint(1, AWT_TEAL);
        r.setSeriesStroke(1, new BasicStroke(2.2f));
        plot.setRenderer(r);

        org.jfree.chart.plot.ValueMarker marker = new org.jfree.chart.plot.ValueMarker(threshold);
        marker.setPaint(AWT_AMBER);
        marker.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 1f, new float[]{6f, 4f}, 0f));
        marker.setLabel("threshold");
        marker.setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 8));
        marker.setLabelPaint(AWT_AMBER);
        plot.addRangeMarker(marker);

        styleAxis(plot.getDomainAxis(), "Time (seconds)");
        styleAxis(plot.getRangeAxis(), "RMS");

        chart.getLegend().setBackgroundPaint(AWT_BG);
        chart.getLegend().setItemFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 8));
        chart.getLegend().setItemPaint(AWT_INK);
        chart.getLegend().setFrame(new org.jfree.chart.block.BlockBorder(AWT_BG));
        return chart;
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════
    private void styleAxis(org.jfree.chart.axis.Axis axis, String label) {
        axis.setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
        axis.setLabelPaint(AWT_INK);
        axis.setTickLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 9));
        axis.setTickLabelPaint(new java.awt.Color(100, 116, 139));
        axis.setAxisLinePaint(AWT_BORDER);
        if (axis instanceof ValueAxis va) {
            va.setTickMarkPaint(AWT_BORDER);
        }
    }

    private Image makeImage(JFreeChart chart, float w, float h) throws Exception {
        int pw = Math.round(w * 2.2f);
        int ph = Math.round(h * 2.2f);
        BufferedImage img = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        chart.draw(g2, new java.awt.Rectangle(pw, ph));
        g2.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        Image im = Image.getInstance(baos.toByteArray());
        im.scaleToFit(w, h);
        return im;
    }

    private PdfPCell imgCell(Image img) {
        PdfPCell c = new PdfPCell(img, false);
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(C_BORDER);
        c.setBorderWidth(0.5f);
        c.setPadding(8);
        c.setBackgroundColor(C_WHITE);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }

    private Paragraph chartCaption(String text) {
        Paragraph p = new Paragraph(text, F_CAPTION);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(4);
        p.setSpacingAfter(14);
        return p;
    }

    private PdfPCell captionCell(String text) {
        Paragraph p = new Paragraph(text, F_CAPTION);
        p.setAlignment(Element.ALIGN_CENTER);
        PdfPCell c = new PdfPCell(p);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPaddingTop(4);
        return c;
    }

    private PdfPCell statPill(String value, String label, BaseColor tone, BaseColor bg) {
        Paragraph p1 = new Paragraph(value, new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, tone));
        p1.setAlignment(Element.ALIGN_CENTER);
        Paragraph p2 = new Paragraph(label.toUpperCase(), new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, tone));
        p2.setAlignment(Element.ALIGN_CENTER);
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(10);
        c.setPaddingTop(12);
        c.setBorderWidthTop(3);
        c.setBorderColorTop(tone);
        c.addElement(p1);
        c.addElement(p2);
        return c;
    }

    private PdfPCell momentCell(String title, String detail, BaseColor tone) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(title + "\n", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, tone)));
        p.add(new Chunk(detail, new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, C_INK2)));
        PdfPCell c = new PdfPCell(p);
        c.setBackgroundColor(C_WHITE);
        c.setBorder(Rectangle.LEFT);
        c.setBorderWidthLeft(3);
        c.setBorderColorLeft(tone);
        c.setBorderColor(C_BORDER);
        c.setPadding(10);
        c.setPaddingLeft(12);
        c.setPaddingBottom(11);
        return c;
    }

    private PdfPCell actionCard(String title, String body, BaseColor tone, BaseColor bg) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(title + "\n", new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, tone)));
        p.add(new Chunk(body, new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, C_INK2)));
        p.setLeading(13f);

        PdfPCell c = new PdfPCell(p);
        c.setBackgroundColor(bg);
        c.setBorder(Rectangle.LEFT);
        c.setBorderWidthLeft(3f);
        c.setBorderColorLeft(tone);
        c.setPadding(10);
        c.setPaddingTop(11);
        c.setBorderColor(C_BORDER);
        return c;
    }

    private double[] phaseAverages(List<Double> timeline) {
        if (timeline == null || timeline.isEmpty()) return new double[]{0, 0, 0};
        int n = timeline.size();
        int a = Math.max(1, n / 3);
        int b = Math.max(a + 1, (2 * n) / 3);
        return new double[]{
                avgSlice(timeline, 0, a),
                avgSlice(timeline, a, b),
                avgSlice(timeline, b, n)
        };
    }

    private double avgSlice(List<Double> list, int from, int to) {
        if (from >= to) return 0;
        double s = 0;
        for (int i = from; i < to; i++) s += list.get(i);
        return s / (to - from);
    }

    private Paragraph emptyNotice(String msg) {
        Paragraph p = new Paragraph(msg, F_CAPTION);
        p.setSpacingBefore(8);
        p.setSpacingAfter(16);
        return p;
    }

    private void space(Document doc, float pts) throws Exception {
        doc.add(new Paragraph(" ") {{ setSpacingAfter(pts); }});
    }

    private void addRow(PdfPTable t, BaseColor bg, String... vals) {
        for (int i = 0; i < vals.length; i++) {
            Font f = (i == vals.length - 1)
                    ? new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, C_TEAL)
                    : F_TBL_CELL;
            t.addCell(tblCell(vals[i], bg, f));
        }
    }

    private PdfPCell tblCell(String text, BaseColor bg, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(C_BORDER);
        c.setBorderWidth(0.4f);
        c.setPadding(8);
        c.setPaddingLeft(10);
        return c;
    }

    // ── Footer event ──────────────────────────────────────────────────────
    private static class FooterEvent extends PdfPageEventHelper {
        private final String ts;
        private int pageNum = 0;

        FooterEvent(String ts) { this.ts = ts; }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            pageNum++;
            if (pageNum == 1) return; // skip cover

            PdfContentByte cb = writer.getDirectContent();
            BaseFont bf;
            try {
                bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
            } catch (Exception e) { return; }

            // footer line
            cb.setColorStroke(new BaseColor(226, 232, 240));
            cb.setLineWidth(0.5f);
            cb.moveTo(ML, 38);
            cb.lineTo(PW - MR, 38);
            cb.stroke();

            // left: app name
            cb.setColorFill(new BaseColor(148, 163, 184));
            cb.setFontAndSize(bf, 8);
            cb.beginText();
            cb.showTextAligned(Element.ALIGN_LEFT,
                    "MeetPulse  •  Audio Analysis Report", ML, 26, 0);
            cb.endText();

            // center: timestamp
            cb.beginText();
            cb.showTextAligned(Element.ALIGN_CENTER, ts, PW / 2, 26, 0);
            cb.endText();

            // right: page number
            cb.beginText();
            cb.showTextAligned(Element.ALIGN_RIGHT,
                    "Page " + pageNum, PW - MR, 26, 0);
            cb.endText();
        }
    }
}
