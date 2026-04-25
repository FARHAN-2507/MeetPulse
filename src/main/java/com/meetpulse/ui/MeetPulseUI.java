package com.meetpulse.ui;

import com.meetpulse.audio.AudioCaptureService;
import com.meetpulse.audio.AudioCaptureService.Phase;
import com.meetpulse.report.PdfReportGenerator;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

public class MeetPulseUI {

    // ── palette ───────────────────────────────────────────────────────────
    private static final Color C_BG       = Color.web("#0b0e17");
    private static final Color C_SURFACE  = Color.web("#131929");
    private static final Color C_SURFACE2 = Color.web("#1a2035");
    private static final Color C_BORDER   = Color.web("#1f2d47");
    private static final Color C_ACCENT   = Color.web("#4f8ef7");
    private static final Color C_TEAL     = Color.web("#1dd6a0");
    private static final Color C_AMBER    = Color.web("#f5a623");
    private static final Color C_RED      = Color.web("#f05c5c");
    private static final Color C_TEXT     = Color.web("#e2e8f8");
    private static final Color C_MUTED    = Color.web("#5a6a8a");
    private static final Color C_MUTED2   = Color.web("#3a4a6a");

    private static final String S_BG       = "#0b0e17";
    private static final String S_SURFACE  = "#131929";
    private static final String S_SURFACE2 = "#1a2035";
    private static final String S_BORDER   = "#1f2d47";
    private static final String S_ACCENT   = "#4f8ef7";
    private static final String S_TEAL     = "#1dd6a0";
    private static final String S_AMBER    = "#f5a623";
    private static final String S_RED      = "#f05c5c";
    private static final String S_TEXT     = "#e2e8f8";
    private static final String S_MUTED    = "#5a6a8a";

    // ── waveform buffer ───────────────────────────────────────────────────
    private static final int    WAVE_SIZE  = 140;
    private final Deque<Double> waveBuffer = new ArrayDeque<>();
    private volatile double     liveRms    = 0;
    private volatile Phase      livePhase  = Phase.IDLE;
    private volatile boolean    liveSilent = true;
    private volatile int        uiFrameTick = 0;

    // ── audio ─────────────────────────────────────────────────────────────
    private AudioCaptureService audioService = new AudioCaptureService();
    private Thread              audioThread;

    // ── timer ─────────────────────────────────────────────────────────────
    private final AtomicInteger elapsedSec = new AtomicInteger(0);
    private Timeline            timerTimeline;
    private Timeline            calCountdown;
    private int                 calSecsLeft = 3;

    // ── ui nodes ──────────────────────────────────────────────────────────
    private Canvas        waveCanvas;
    private AnimationTimer waveAnim;

    private Circle        phaseDot;
    private Label         statusLabel;
    private Label         timerLabel;
    private Label         calCountLabel;
    private StackPane     calOverlay;

    private Button        btnStart;
    private Button        btnStop;
    private Button        btnReset;
    private Button        btnReport;

    private Label         mFrames;
    private Label         mSpeaking;
    private Label         mSegments;
    private Label         mPeak;
    private Label         mThreshold;
    private Label         mDuration;
    private Label         mRaw;
    private Label         mSmooth;
    private Label         mFloor;
    private Label         detectorMode;

    private TextArea      logArea;
    private VBox          root;

    // ─────────────────────────────────────────────────────────────────────
    public VBox buildRoot() {
        // seed waveform buffer
        for (int i = 0; i < WAVE_SIZE; i++) waveBuffer.addLast(0.0);

        root = new VBox(16);
        root.setStyle("-fx-background-color: " + S_BG + ";");
        root.setPadding(new Insets(24, 28, 24, 28));

        root.getChildren().addAll(
                buildHeader(),
                buildStatusBar(),
                buildWaveformSection(),
                buildControlBar(),
                buildMetricsGrid(),
                buildLogSection()
        );

        startWaveAnimation();
        return root;
    }

    // ══════════════════════════════════════════════════════════════════════
    // HEADER
    // ══════════════════════════════════════════════════════════════════════
    private HBox buildHeader() {
        // icon box
        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(42, 42);
        iconBox.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #4f8ef7, #1dd6a0);" +
                        "-fx-background-radius: 12;"
        );
        Label iconLbl = new Label("M");
        iconLbl.setStyle(
                "-fx-font-family: 'JetBrains Mono', monospace;" +
                        "-fx-font-size: 18px; -fx-font-weight: bold;" +
                        "-fx-text-fill: #0b0e17;"
        );
        iconBox.getChildren().add(iconLbl);

        Label title = new Label("MeetPulse");
        title.setStyle(
                "-fx-font-family: 'JetBrains Mono', monospace;" +
                        "-fx-font-size: 20px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + S_TEXT + ";"
        );
        Label subtitle = new Label("Audio Intelligence");
        subtitle.setStyle(
                "-fx-font-family: 'JetBrains Mono', monospace;" +
                        "-fx-font-size: 10px;" +
                        "-fx-text-fill: " + S_MUTED + ";" +
                        "-fx-letter-spacing: 1.5;"
        );
        VBox titleVBox = new VBox(2, title, subtitle);
        titleVBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // version badge
        Label ver = new Label("v1.2");
        ver.setStyle(
                "-fx-font-family: 'JetBrains Mono', monospace;" +
                        "-fx-font-size: 10px;" +
                        "-fx-text-fill: " + S_MUTED + ";" +
                        "-fx-background-color: " + S_SURFACE2 + ";" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: " + S_BORDER + ";" +
                        "-fx-border-radius: 20;" +
                        "-fx-padding: 4 12 4 12;"
        );

        HBox header = new HBox(12, iconBox, titleVBox, spacer, ver);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    // ══════════════════════════════════════════════════════════════════════
    // STATUS BAR
    // ══════════════════════════════════════════════════════════════════════
    private HBox buildStatusBar() {
        phaseDot = new Circle(7, C_MUTED);

        // glow effect on dot
        DropShadow glow = new DropShadow(12, C_MUTED);
        phaseDot.setEffect(glow);

        statusLabel = new Label("Ready — press Start to begin");
        statusLabel.setStyle(
                "-fx-font-family: 'JetBrains Mono', monospace;" +
                        "-fx-font-size: 12px;" +
                        "-fx-text-fill: " + S_MUTED + ";"
        );

        detectorMode = new Label("Adaptive RMS");
        detectorMode.setStyle(
                "-fx-font-family: 'JetBrains Mono', monospace;" +
                        "-fx-font-size: 10px;" +
                        "-fx-text-fill: " + S_ACCENT + ";" +
                        "-fx-background-color: rgba(79,142,247,0.15);" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 3 8 3 8;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        timerLabel = new Label("00:00");
        timerLabel.setStyle(
                "-fx-font-family: 'JetBrains Mono', monospace;" +
                        "-fx-font-size: 26px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + S_ACCENT + ";"
        );

        // calibration countdown overlay
        calCountLabel = new Label("3");
        calCountLabel.setStyle(
                "-fx-font-family: 'JetBrains Mono', monospace;" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + S_AMBER + ";"
        );
        calOverlay = new StackPane(calCountLabel);
        calOverlay.setPrefWidth(30);
        calOverlay.setAlignment(Pos.CENTER);
        calOverlay.setVisible(false);
        calOverlay.setStyle(
                "-fx-background-color: rgba(245,166,35,0.15);" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: " + S_AMBER + ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-padding: 2 6 2 6;"
        );

        HBox bar = new HBox(12,
                phaseDot, statusLabel, detectorMode, spacer, calOverlay, timerLabel
        );
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 20, 14, 20));
        bar.setStyle(card());
        return bar;
    }

    // ══════════════════════════════════════════════════════════════════════
    // WAVEFORM
    // ══════════════════════════════════════════════════════════════════════
    private VBox buildWaveformSection() {
        Label lbl = new Label("LIVE ENERGY");
        lbl.setStyle(mono(9, S_MUTED));

        Label rmsReadout = new Label("RMS: —");
        rmsReadout.setStyle(mono(9, S_ACCENT));

        Label threshLbl = new Label("— threshold");
        threshLbl.setStyle(mono(9, S_AMBER));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox labelRow = new HBox(8, lbl, sp, rmsReadout, threshLbl);
        labelRow.setAlignment(Pos.CENTER_LEFT);

        waveCanvas = new Canvas(756, 90);
        drawIdleWave();

        // update rmsReadout from animation loop
        AnimationTimer rmsUpdater = new AnimationTimer() {
            @Override public void handle(long now) {
                if (liveRms > 0) {
                    rmsReadout.setText(String.format("RMS: %.0f", liveRms));
                    threshLbl.setText(String.format("floor %.0f / thr %.0f", audioService.getNoiseFloor(), audioService.getThreshold()));
                }
            }
        };
        rmsUpdater.start();

        VBox box = new VBox(8, labelRow, waveCanvas);
        box.setPadding(new Insets(16, 20, 16, 20));
        box.setStyle(card());
        return box;
    }

    private void drawIdleWave() {
        GraphicsContext gc = waveCanvas.getGraphicsContext2D();
        double W = waveCanvas.getWidth(), H = waveCanvas.getHeight();
        gc.setFill(Color.TRANSPARENT);
        gc.clearRect(0, 0, W, H);

        gc.setStroke(Color.web(S_BORDER));
        gc.setLineWidth(1);
        gc.strokeLine(0, H / 2, W, H / 2);
    }

    private void startWaveAnimation() {
        waveAnim = new AnimationTimer() {
            long lastDraw = 0;

            @Override
            public void handle(long now) {
                if (now - lastDraw < 33_000_000) return; // ~30fps
                lastDraw = now;
                renderWave();
            }
        };
        waveAnim.start();
    }

    private void renderWave() {
        GraphicsContext gc = waveCanvas.getGraphicsContext2D();
        double W = waveCanvas.getWidth();
        double H = waveCanvas.getHeight();

        // dark background with subtle gradient
        gc.setFill(Color.web(S_SURFACE));
        gc.fillRect(0, 0, W, H);

        // grid lines
        gc.setStroke(Color.web(S_BORDER, 0.5));
        gc.setLineWidth(0.5);
        gc.strokeLine(0, H * 0.25, W, H * 0.25);
        gc.strokeLine(0, H * 0.5,  W, H * 0.5);
        gc.strokeLine(0, H * 0.75, W, H * 0.75);

        Double[] vals;
        synchronized (waveBuffer) {
            vals = waveBuffer.toArray(new Double[0]);
        }
        if (vals.length == 0) return;

        double maxVal = 1.0;
        for (double v : vals) if (v > maxVal) maxVal = v;

        // draw threshold line
        double thresh = audioService.getThreshold();
        if (thresh > 0 && maxVal > 0 && livePhase == Phase.RECORDING) {
            double ty = H - (thresh / maxVal) * (H - 8) - 4;
            ty = Math.max(4, Math.min(H - 4, ty));
            gc.setStroke(Color.web(S_AMBER, 0.6));
            gc.setLineWidth(1);
            gc.setLineDashes(6, 4);
            gc.strokeLine(0, ty, W, ty);
            gc.setLineDashes(0);
        }

        double barW = W / vals.length;

        for (int i = 0; i < vals.length; i++) {
            double norm  = vals[i] / maxVal;
            double barH  = Math.max(norm * (H - 8), 1.5);
            double x     = i * barW;
            double y     = (H - barH) / 2.0;

            // colour: idle=muted, calibrating=amber, silent=blue, speaking=teal
            Color fill;
            double alpha = 0.25 + norm * 0.75;
            if (livePhase == Phase.IDLE || livePhase == Phase.STOPPED) {
                fill = Color.web(S_MUTED, alpha * 0.5);
            } else if (livePhase == Phase.CALIBRATING) {
                fill = Color.web(S_AMBER, alpha);
            } else if (!liveSilent) {
                fill = Color.web(S_TEAL, alpha);
            } else {
                fill = Color.web(S_ACCENT, alpha * 0.6);
            }

            gc.setFill(fill);

            // rounded-ish bars: draw with slight rounding via arc
            double bw = Math.max(barW - 2, 1);
            gc.fillRoundRect(x + 1, y, bw, barH, 2, 2);

            // top glow on loud bars
            if (norm > 0.7 && livePhase == Phase.RECORDING && !liveSilent) {
                gc.setFill(Color.web(S_TEAL, 0.3 * norm));
                gc.fillRoundRect(x + 1, y, bw, barH * 0.3, 2, 2);
            }
        }

        // speaking indicator pulse overlay
        if (livePhase == Phase.RECORDING && !liveSilent) {
            // subtle teal border glow
            gc.setStroke(Color.web(S_TEAL, 0.3));
            gc.setLineWidth(2);
            gc.strokeRect(1, 1, W - 2, H - 2);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // CONTROLS
    // ══════════════════════════════════════════════════════════════════════
    private HBox buildControlBar() {
        btnStart  = makeBtn("▶  Start",           S_TEAL,   "#021a12", true);
        btnStop   = makeBtn("■  Stop",             S_RED,    "#1a0606", false);
        btnReset  = makeBtn("↺  Reset",            S_AMBER,  "#1a1006", false);
        btnReport = makeBtn("⬇  Generate Report",  S_ACCENT, "#040d1f", false);

        btnStart.setOnAction(e  -> onStart());
        btnStop.setOnAction(e   -> onStop());
        btnReset.setOnAction(e  -> onReset());
        btnReport.setOnAction(e -> onGenerateReport());

        // equal width
        HBox.setHgrow(btnStart,  Priority.ALWAYS);
        HBox.setHgrow(btnStop,   Priority.ALWAYS);
        HBox.setHgrow(btnReset,  Priority.ALWAYS);
        HBox.setHgrow(btnReport, Priority.ALWAYS);
        btnStart.setMaxWidth(Double.MAX_VALUE);
        btnStop.setMaxWidth(Double.MAX_VALUE);
        btnReset.setMaxWidth(Double.MAX_VALUE);
        btnReport.setMaxWidth(Double.MAX_VALUE);

        HBox bar = new HBox(12, btnStart, btnStop, btnReset, btnReport);
        return bar;
    }

    private Button makeBtn(String text, String bg, String fg, boolean enabled) {
        Button b = new Button(text);
        b.setDisable(!enabled);
        b.setStyle(btnStyle(bg, fg));
        b.setOnMouseEntered(e -> { if (!b.isDisabled()) b.setStyle(btnHover(bg, fg)); });
        b.setOnMouseExited(e  -> { if (!b.isDisabled()) b.setStyle(btnStyle(bg, fg)); });
        b.disabledProperty().addListener((obs, old, disabled) -> {
            b.setStyle(disabled ? btnDisabled() : btnStyle(bg, fg));
        });
        return b;
    }

    private String btnStyle(String bg, String fg) {
        return "-fx-background-color: " + bg + ";" +
                "-fx-text-fill: " + fg + ";" +
                "-fx-font-family: 'JetBrains Mono', monospace;" +
                "-fx-font-size: 12px; -fx-font-weight: bold;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 13 10 13 10;" +
                "-fx-cursor: hand;";
    }

    private String btnHover(String bg, String fg) {
        return btnStyle(bg, fg) + "-fx-opacity: 0.85;";
    }

    private String btnDisabled() {
        return "-fx-background-color: " + S_SURFACE2 + ";" +
                "-fx-text-fill: " + S_MUTED + ";" +
                "-fx-font-family: 'JetBrains Mono', monospace;" +
                "-fx-font-size: 12px;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 13 10 13 10;";
    }

    // ══════════════════════════════════════════════════════════════════════
    // METRICS GRID
    // ══════════════════════════════════════════════════════════════════════
    private GridPane buildMetricsGrid() {
        mFrames    = metricVal("—");
        mSpeaking  = metricVal("—");
        mSegments  = metricVal("—");
        mPeak      = metricVal("—");
        mThreshold = metricVal("—");
        mDuration  = metricVal("00:00");
        mRaw       = metricVal("—");
        mSmooth    = metricVal("—");
        mFloor     = metricVal("—");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        ColumnConstraints cc = new ColumnConstraints();
        cc.setHgrow(Priority.ALWAYS);
        cc.setPercentWidth(33.33);
        grid.getColumnConstraints().addAll(cc, cc, cc);

        grid.add(metricCard(mDuration,  "DURATION",     S_ACCENT), 0, 0);
        grid.add(metricCard(mFrames,    "FRAMES",       S_ACCENT), 1, 0);
        grid.add(metricCard(mThreshold, "THRESHOLD",    S_AMBER),  2, 0);
        grid.add(metricCard(mSpeaking,  "SPEAKING",     S_TEAL),   0, 1);
        grid.add(metricCard(mSegments,  "SEGMENTS",     S_TEAL),   1, 1);
        grid.add(metricCard(mPeak,      "PEAK RMS",     S_RED),    2, 1);
        grid.add(metricCard(mRaw,       "RAW RMS",      S_ACCENT), 0, 2);
        grid.add(metricCard(mSmooth,    "SMOOTH RMS",   S_TEAL),   1, 2);
        grid.add(metricCard(mFloor,     "NOISE FLOOR",  S_AMBER),  2, 2);

        return grid;
    }

    private VBox metricCard(Label val, String label, String color) {
        val.setStyle(
                "-fx-font-family: 'JetBrains Mono', monospace;" +
                        "-fx-font-size: 22px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + color + ";"
        );
        Label lbl = new Label(label);
        lbl.setStyle(mono(8, S_MUTED));

        // bottom accent bar
        Region accentBar = new Region();
        accentBar.setPrefHeight(2);
        accentBar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 1;");
        accentBar.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(4, val, lbl, accentBar);
        card.setPadding(new Insets(14, 16, 12, 16));
        card.setStyle(card());
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private Label metricVal(String text) {
        Label l = new Label(text);
        return l;
    }

    // ══════════════════════════════════════════════════════════════════════
    // LOG
    // ══════════════════════════════════════════════════════════════════════
    private VBox buildLogSection() {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(4, C_TEAL);
        // animate pulse
        FadeTransition ft = new FadeTransition(Duration.seconds(1.2), dot);
        ft.setFromValue(1); ft.setToValue(0.3);
        ft.setAutoReverse(true); ft.setCycleCount(Animation.INDEFINITE);
        ft.play();

        Label lbl = new Label("CONSOLE OUTPUT");
        lbl.setStyle(mono(9, S_MUTED));

        Button clearBtn = new Button("Clear");
        clearBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + S_MUTED + ";" +
                        "-fx-font-family: 'JetBrains Mono', monospace;" +
                        "-fx-font-size: 9px;" +
                        "-fx-border-color: " + S_BORDER + ";" +
                        "-fx-border-radius: 4;" +
                        "-fx-padding: 2 8 2 8;" +
                        "-fx-cursor: hand;"
        );
        clearBtn.setOnAction(e -> logArea.clear());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(dot, lbl, sp, clearBtn);

        logArea = new TextArea("[ MeetPulse ready ]\n");
        logArea.setEditable(false);
        logArea.setPrefHeight(130);
        logArea.setWrapText(true);
        logArea.setStyle(
                "-fx-background-color: #080c14;" +
                        "-fx-control-inner-background: #080c14;" +
                        "-fx-font-family: 'JetBrains Mono', monospace;" +
                        "-fx-font-size: 11px;" +
                        "-fx-text-fill: #4a7a6a;" +
                        "-fx-border-color: transparent;" +
                        "-fx-highlight-fill: #1f3a5a;" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;"
        );

        VBox box = new VBox(10, header, logArea);
        box.setPadding(new Insets(14, 18, 14, 18));
        box.setStyle(card());
        return box;
    }

    // ══════════════════════════════════════════════════════════════════════
    // SESSION ACTIONS
    // ══════════════════════════════════════════════════════════════════════
    private void onStart() {
        btnStart.setDisable(true);
        btnStop.setDisable(false);
        btnReset.setDisable(true);
        btnReport.setDisable(true);

        elapsedSec.set(0);
        timerLabel.setText("00:00");
        uiFrameTick = 0;

        // wire callbacks BEFORE starting thread
        audioService.setOnFrame((rms, silent, phase) -> {
            liveRms    = rms;
            liveSilent = silent;
            livePhase  = phase;
            uiFrameTick++;

            // push to waveform buffer (thread-safe deque)
            synchronized (waveBuffer) {
                waveBuffer.addLast(rms);
                if (waveBuffer.size() > WAVE_SIZE) waveBuffer.pollFirst();
            }

            // update metrics on FX thread every ~6 frames (independent of analyzer state)
            if (uiFrameTick % 6 == 0) {
                Platform.runLater(this::refreshMetrics);
            }
        });

        audioService.setOnLog(msg -> Platform.runLater(() -> appendLog(msg)));

        audioService.setOnPhaseChange(phase -> Platform.runLater(() -> {
            livePhase = phase;
            updatePhaseUI(phase);
        }));

        audioThread = new Thread(audioService::start, "meetpulse-audio");
        audioThread.setDaemon(true);
        audioThread.start();

        startTimerTick();
        startCalCountdown();

        appendLog("=== Session started ===");
    }

    private void onStop() {
        audioService.stop();
        stopTimerTick();
        stopCalCountdown();
        calOverlay.setVisible(false);

        btnStop.setDisable(true);
        btnReset.setDisable(false);
        btnReport.setDisable(false);
        btnStart.setDisable(true); // use Reset to start again

        // Wait briefly for audio thread to finish then refresh
        new Thread(() -> {
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                livePhase = Phase.STOPPED;
                updatePhaseUI(Phase.STOPPED);
                refreshMetrics();
                appendLog("=== Session stopped ===");
                appendLog(audioService.getAnalyzer().summarize().toString());
            });
        }).start();
    }

    private void onReset() {
        // stop if still running
        audioService.stop();
        stopTimerTick();
        stopCalCountdown();

        // create fresh service
        audioService = new AudioCaptureService();
        liveRms    = 0;
        liveSilent = true;
        livePhase  = Phase.IDLE;
        uiFrameTick = 0;

        // clear waveform
        synchronized (waveBuffer) {
            waveBuffer.clear();
            for (int i = 0; i < WAVE_SIZE; i++) waveBuffer.addLast(0.0);
        }

        // reset UI
        elapsedSec.set(0);
        timerLabel.setText("00:00");
        calOverlay.setVisible(false);
        updatePhaseUI(Phase.IDLE);

        mFrames.setText("—");
        mSpeaking.setText("—");
        mSegments.setText("—");
        mPeak.setText("—");
        mThreshold.setText("—");
        mDuration.setText("00:00");
        mRaw.setText("—");
        mSmooth.setText("—");
        mFloor.setText("—");

        btnStart.setDisable(false);
        btnStop.setDisable(true);
        btnReset.setDisable(true);
        btnReport.setDisable(true);

        appendLog("=== Session reset — ready to start ===");
    }

    private void onGenerateReport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save PDF Report");
        fc.setInitialFileName("meetpulse_report.pdf");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        File file = fc.showSaveDialog(root.getScene().getWindow());
        if (file == null) return;

        btnReport.setDisable(true);
        btnReport.setText("Generating...");

        new Thread(() -> {
            try {
                new PdfReportGenerator().generate(audioService, file.getAbsolutePath());
                Platform.runLater(() -> {
                    appendLog("PDF saved → " + file.getName());
                    btnReport.setText("⬇  Generate Report");
                    btnReport.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    appendLog("PDF error: " + ex.getMessage());
                    btnReport.setText("⬇  Generate Report");
                    btnReport.setDisable(false);
                });
            }
        }).start();
    }

    // ══════════════════════════════════════════════════════════════════════
    // UI STATE UPDATES
    // ══════════════════════════════════════════════════════════════════════
    private void updatePhaseUI(Phase phase) {
        switch (phase) {
            case IDLE -> {
                phaseDot.setFill(C_MUTED);
                setDotGlow(C_MUTED);
                stopDotPulse();
                setStatus("Ready — press Start to begin", S_MUTED);
            }
            case CALIBRATING -> {
                phaseDot.setFill(C_AMBER);
                setDotGlow(C_AMBER);
                startDotPulse(C_AMBER);
                setStatus("Calibrating noise floor — stay silent...", S_AMBER);
                calOverlay.setVisible(true);
            }
            case RECORDING -> {
                phaseDot.setFill(C_TEAL);
                setDotGlow(C_TEAL);
                startDotPulse(C_TEAL);
                setStatus("Recording — adaptive threshold active", S_TEAL);
                calOverlay.setVisible(false);
                mThreshold.setText(String.format("%.0f", audioService.getThreshold()));
            }
            case STOPPED -> {
                phaseDot.setFill(C_RED);
                setDotGlow(C_RED);
                stopDotPulse();
                setStatus("Stopped — generate report or reset", S_MUTED);
            }
        }
    }

    private ScaleTransition dotPulse;
    private void startDotPulse(Color color) {
        stopDotPulse();
        dotPulse = new ScaleTransition(Duration.millis(800), phaseDot);
        dotPulse.setFromX(1.0); dotPulse.setToX(1.5);
        dotPulse.setFromY(1.0); dotPulse.setToY(1.5);
        dotPulse.setAutoReverse(true);
        dotPulse.setCycleCount(Animation.INDEFINITE);
        dotPulse.play();
    }

    private void stopDotPulse() {
        if (dotPulse != null) { dotPulse.stop(); dotPulse = null; }
        phaseDot.setScaleX(1); phaseDot.setScaleY(1);
    }

    private void setDotGlow(Color c) {
        DropShadow glow = new DropShadow(14, c);
        phaseDot.setEffect(glow);
    }

    private void setStatus(String msg, String color) {
        statusLabel.setText(msg);
        statusLabel.setStyle(
                "-fx-font-family: 'JetBrains Mono', monospace;" +
                        "-fx-font-size: 12px;" +
                        "-fx-text-fill: " + color + ";"
        );
    }

    private void refreshMetrics() {
        if (livePhase == Phase.IDLE) return;
        mFrames.setText(String.valueOf(audioService.getLiveFrameCount()));
        mSpeaking.setText(String.format("%.1f%%", audioService.getLiveSpeakingPct()));
        mSegments.setText(String.valueOf(audioService.getLiveSegmentCount()));
        mPeak.setText(String.format("%.0f", audioService.getLivePeakRms()));
        mRaw.setText(String.format("%.0f", audioService.getLiveRawRms()));
        mSmooth.setText(String.format("%.0f", audioService.getLiveSmoothedRms()));
        mFloor.setText(String.format("%.0f", audioService.getNoiseFloor()));
        mThreshold.setText(String.format("%.0f (Δ%.0f)",
                audioService.getThreshold(),
                Math.max(0.0, audioService.getThreshold() - audioService.getNoiseFloor())));
        int s = elapsedSec.get();
        mDuration.setText(String.format("%02d:%02d", s / 60, s % 60));
    }

    // ══════════════════════════════════════════════════════════════════════
    // TIMER
    // ══════════════════════════════════════════════════════════════════════
    private void startTimerTick() {
        stopTimerTick();
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            int s = elapsedSec.incrementAndGet();
            timerLabel.setText(String.format("%02d:%02d", s / 60, s % 60));
            mDuration.setText(String.format("%02d:%02d",  s / 60, s % 60));
        }));
        timerTimeline.setCycleCount(Animation.INDEFINITE);
        timerTimeline.play();
    }

    private void stopTimerTick() {
        if (timerTimeline != null) { timerTimeline.stop(); timerTimeline = null; }
    }

    private void startCalCountdown() {
        calSecsLeft = 3;
        calCountLabel.setText("3s");
        calOverlay.setVisible(false); // shown by phase change
        stopCalCountdown();
        calCountdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            calSecsLeft--;
            if (calSecsLeft > 0) {
                calCountLabel.setText(calSecsLeft + "s");
            } else {
                calOverlay.setVisible(false);
                stopCalCountdown();
            }
        }));
        calCountdown.setCycleCount(3);
        calCountdown.play();
    }

    private void stopCalCountdown() {
        if (calCountdown != null) { calCountdown.stop(); calCountdown = null; }
    }

    // ══════════════════════════════════════════════════════════════════════
    // LOG
    // ══════════════════════════════════════════════════════════════════════
    private void appendLog(String msg) {
        logArea.appendText(msg + "\n");
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    // ══════════════════════════════════════════════════════════════════════
    // STYLE HELPERS
    // ══════════════════════════════════════════════════════════════════════
    private String card() {
        return "-fx-background-color: " + S_SURFACE + ";" +
                "-fx-border-color: " + S_BORDER + ";" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;";
    }

    private String mono(int size, String color) {
        return "-fx-font-family: 'JetBrains Mono', monospace;" +
                "-fx-font-size: " + size + "px;" +
                "-fx-text-fill: " + color + ";";
    }
}
