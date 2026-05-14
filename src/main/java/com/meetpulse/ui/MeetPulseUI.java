package com.meetpulse.ui;

import com.meetpulse.audio.AudioCaptureService;
import com.meetpulse.audio.AudioCaptureService.Phase;
import com.meetpulse.model.MeetingSession;
import com.meetpulse.processing.VoiceActivityDetector;
import com.meetpulse.report.PdfReportGenerator;
import com.meetpulse.service.ExportService;
import com.meetpulse.service.PreferencesManager;
import com.meetpulse.service.SessionHistoryManager;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MeetPulseUI {

    private ThemeManager.ThemeColors T;
    private final PreferencesManager prefs;
    private final SessionHistoryManager history;
    private final ExportService exportService;

    private static final int WAVE_SIZE = 140;
    private final Deque<Double> waveBuffer = new ArrayDeque<>();
    private volatile double liveRms = 0;
    private volatile double liveZcr = 0;
    private volatile Phase livePhase = Phase.IDLE;
    private volatile boolean liveSilent = true;
    private volatile int uiFrameTick = 0;

    private AudioCaptureService audioService = new AudioCaptureService();
    private Thread audioThread;

    private final AtomicInteger elapsedSec = new AtomicInteger(0);
    private Timeline timerTimeline;
    private Timeline calCountdown;
    private int calSecsLeft = 3;

    private Canvas waveCanvas;
    private AnimationTimer waveAnim;

    private Circle phaseDot;
    private Label statusLabel;
    private Label timerLabel;
    private Label calCountLabel;
    private StackPane calOverlay;
    private Label qualityScoreLabel;

    private Button btnStart, btnStop, btnReset, btnReport, btnSettings, btnHistory;

    private Label mFrames, mSpeaking, mPeak, mThreshold, mDuration;
    private Label mZcr, mVad, mSpeakers, mTurns, mQuality, mWpm;

    private Label dRms, dFloor, dThr, dZcr, dVad, dSpeaker;
    private ProgressBar energyBar;

    private TextArea logArea;
    private Slider sensitivitySlider;
    private Label sensitivityLabel;
    private ToggleButton themeToggle;
    private VBox root;

    public MeetPulseUI() {
        this.prefs = new PreferencesManager();
        this.history = new SessionHistoryManager();
        this.exportService = new ExportService();
        T = ThemeManager.LIGHT;
        ThemeManager.setTheme(prefs.getTheme());
        applyTheme();
    }

    private void applyTheme() {
        T = ThemeManager.getColors();
    }

    public VBox buildRoot() {
        for (int i = 0; i < WAVE_SIZE; i++) waveBuffer.addLast(0.0);

        root = new VBox(16);
        root.setStyle("-fx-background-color: " + T.bg + "; -fx-padding: 20;");
        root.setFocusTraversable(true);

        root.getChildren().addAll(
                buildHeader(),
                buildStatusBar(),
                buildWaveformSection(),
                buildControlBar(),
                buildMetricsGrid(),
                buildDebugPanel(),
                buildLogSection()
        );

        setupKeyboardShortcuts();
        startWaveAnimation();
        return root;
    }

    private HBox buildHeader() {
        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(44, 44);
        iconBox.setStyle(
                "-fx-background-color: " + T.accent + ";" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, " + T.shadow + ", 4, 0, 0, 2);"
        );
        Label iconLbl = new Label("M");
        iconLbl.setStyle(
                "-fx-font-family: 'Segoe UI', sans-serif;" +
                        "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;"
        );
        iconBox.getChildren().add(iconLbl);

        Label title = new Label("MeetPulse");
        title.setStyle(
                "-fx-font-family: 'Segoe UI', sans-serif;" +
                        "-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: " + T.text + ";"
        );
        Label subtitle = new Label("Audio Intelligence");
        subtitle.setStyle(
                "-fx-font-family: 'Segoe UI', sans-serif;" +
                        "-fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: " + T.textMuted + ";"
        );
        VBox titleVBox = new VBox(2, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        themeToggle = new ToggleButton();
        themeToggle.setStyle(
                "-fx-background-color: " + T.surface2 + ";" +
                        "-fx-border-color: " + T.border + ";" +
                        "-fx-border-radius: 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 6 12 6 12;" +
                        "-fx-cursor: hand;" +
                        "-fx-text-fill: " + T.text + ";"
        );
        themeToggle.setText(ThemeManager.getCurrentTheme() == ThemeManager.Theme.LIGHT ? "Dark Mode" : "Light Mode");
        themeToggle.setOnAction(e -> {
            ThemeManager.toggleTheme();
            applyTheme();
            prefs.setTheme(ThemeManager.getCurrentTheme());
            prefs.savePreferences();
            refreshTheme();
        });

        qualityScoreLabel = new Label("—");
        qualityScoreLabel.setStyle(
                "-fx-font-family: 'Segoe UI', sans-serif;" +
                        "-fx-font-size: 14px; -fx-font-weight: 700;" +
                        "-fx-text-fill: " + T.accent + ";"
        );

        HBox header = new HBox(12, iconBox, titleVBox, spacer, qualityScoreLabel, themeToggle);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private void refreshTheme() {
        root.setStyle("-fx-background-color: " + T.bg + "; -fx-padding: 20;");
        themeToggle.setText(ThemeManager.getCurrentTheme() == ThemeManager.Theme.LIGHT ? "Dark Mode" : "Light Mode");
        themeToggle.setTextFill(Color.web(T.text));
    }

    private HBox buildStatusBar() {
        phaseDot = new Circle(7, Color.web(T.textMuted));
        DropShadow glow = new DropShadow(12, Color.web(T.textMuted));
        phaseDot.setEffect(glow);

        statusLabel = new Label("Ready — press Start to begin");
        statusLabel.setStyle(mono(13, T.textMuted, true));

        Label detectorMode = new Label("Adaptive RMS");
        detectorMode.setStyle(
                "-fx-font-family: 'Segoe UI', sans-serif;" +
                        "-fx-font-size: 11px; -fx-font-weight: 600;" +
                        "-fx-text-fill: " + T.accent + ";" +
                        "-fx-background-color: " + T.surface2 + ";" +
                        "-fx-background-radius: 10; -fx-padding: 3 10 3 10;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        timerLabel = new Label("00:00");
        timerLabel.setStyle(
                "-fx-font-family: 'Segoe UI', sans-serif;" +
                        "-fx-font-size: 28px; -fx-font-weight: 700; -fx-text-fill: " + T.text + ";"
        );

        calCountLabel = new Label("3");
        calCountLabel.setStyle(
                "-fx-font-family: 'Segoe UI', sans-serif;" +
                        "-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + T.amber + ";"
        );
        calOverlay = new StackPane(calCountLabel);
        calOverlay.setPrefWidth(30);
        calOverlay.setAlignment(Pos.CENTER);
        calOverlay.setVisible(false);
        calOverlay.setStyle(
                "-fx-background-color: " + T.surface2 + ";" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: " + T.amber + ";" +
                        "-fx-border-radius: 6; -fx-padding: 2 6 2 6;"
        );

        HBox bar = new HBox(12, phaseDot, statusLabel, detectorMode, spacer, calOverlay, timerLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 20, 14, 20));
        bar.setStyle(cardStyle());
        return bar;
    }

    private VBox buildWaveformSection() {
        Label lbl = new Label("LIVE ENERGY");
        lbl.setStyle(mono(10, T.textMuted, true));

        Label rmsReadout = new Label("RMS: —");
        rmsReadout.setStyle(mono(10, T.accent, false));

        Label threshLbl = new Label("— threshold");
        threshLbl.setStyle(mono(10, T.amber, false));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox labelRow = new HBox(8, lbl, sp, rmsReadout, threshLbl);
        labelRow.setAlignment(Pos.CENTER_LEFT);

        waveCanvas = new Canvas(700, 90);
        drawIdleWave();

        VBox box = new VBox(8, labelRow, waveCanvas);
        box.setPadding(new Insets(16, 20, 16, 20));
        box.setStyle(cardStyle());
        return box;
    }

    private void drawIdleWave() {
        GraphicsContext gc = waveCanvas.getGraphicsContext2D();
        double W = waveCanvas.getWidth(), H = waveCanvas.getHeight();
        gc.clearRect(0, 0, W, H);
        gc.setFill(Color.web(T.surface2));
        gc.fillRect(0, 0, W, H);
        gc.setStroke(Color.web(T.border));
        gc.setLineWidth(1);
        gc.strokeLine(0, H / 2, W, H / 2);
    }

    private void startWaveAnimation() {
        waveAnim = new AnimationTimer() {
            long lastDraw = 0;
            @Override public void handle(long now) {
                if (now - lastDraw < 33_000_000) return;
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

        gc.setFill(Color.web(T.surface2));
        gc.fillRect(0, 0, W, H);

        gc.setStroke(Color.web(T.border));
        gc.setLineWidth(0.5);
        gc.strokeLine(0, H * 0.5, W, H * 0.5);

        Double[] vals;
        synchronized (waveBuffer) {
            vals = waveBuffer.toArray(new Double[0]);
        }
        if (vals.length == 0) return;

        double maxVal = 1.0;
        for (double v : vals) if (v > maxVal) maxVal = v;

        double barW = W / vals.length;
        for (int i = 0; i < vals.length; i++) {
            double norm = vals[i] / maxVal;
            double barH = Math.max(norm * (H - 8), 1.5);
            double x = i * barW;
            double y = (H - barH) / 2.0;

            Color fill;
            double alpha = 0.25 + norm * 0.75;
            if (livePhase == Phase.IDLE || livePhase == Phase.STOPPED) {
                fill = Color.web(T.textMuted, alpha * 0.5);
            } else if (livePhase == Phase.CALIBRATING) {
                fill = Color.web(T.amber, alpha);
            } else if (!liveSilent) {
                fill = Color.web(T.teal, alpha);
            } else {
                fill = Color.web(T.accent, alpha * 0.6);
            }

            gc.setFill(fill);
            gc.fillRoundRect(x + 1, y, Math.max(barW - 2, 1), barH, 2, 2);
        }

        if (livePhase == Phase.RECORDING && !liveSilent) {
            gc.setStroke(Color.web(T.teal, 0.4));
            gc.setLineWidth(2);
            gc.strokeRect(1, 1, W - 2, H - 2);
        }
    }

    private HBox buildControlBar() {
        sensitivityLabel = new Label("Sensitivity: " + String.format("%.1fx", prefs.getSensitivity()));
        sensitivityLabel.setStyle(mono(11, T.textMuted, true));

        sensitivitySlider = new Slider(1.2, 2.5, prefs.getSensitivity());
        sensitivitySlider.setStyle(
                "-fx-control-inner-background: " + T.surface2 + ";" +
                        "-fx-thumb: " + T.accent + ";"
        );
        sensitivitySlider.setPrefWidth(150);
        sensitivitySlider.valueProperty().addListener((obs, old, val) -> {
            double val1 = val.doubleValue();
            sensitivityLabel.setText("Sensitivity: " + String.format("%.1fx", val1));
            prefs.setSensitivity(val1);
        });

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        btnStart = makeBtn("▶  Start", T.teal, true);
        btnStop = makeBtn("■  Stop", T.red, false);
        btnReset = makeBtn("↺  Reset", T.amber, false);
        btnReport = makeBtn("⬇  Export", T.accent, false);
        btnSettings = makeBtn("⚙", T.textMuted, true);
        btnHistory = makeBtn("📋", T.textMuted, true);

        btnStart.setOnAction(e -> onStart());
        btnStop.setOnAction(e -> onStop());
        btnReset.setOnAction(e -> onReset());
        btnReport.setOnAction(e -> onGenerateReport());
        btnSettings.setOnAction(e -> showSettingsDialog());
        btnHistory.setOnAction(e -> showHistoryDialog());

        HBox.setHgrow(btnStart, Priority.ALWAYS);
        HBox.setHgrow(btnStop, Priority.ALWAYS);
        HBox.setHgrow(btnReset, Priority.ALWAYS);
        HBox.setHgrow(btnReport, Priority.ALWAYS);
        btnStart.setMaxWidth(Double.MAX_VALUE);
        btnStop.setMaxWidth(Double.MAX_VALUE);
        btnReset.setMaxWidth(Double.MAX_VALUE);
        btnReport.setMaxWidth(Double.MAX_VALUE);

        HBox controls = new HBox(8, btnStart, btnStop, btnReset, btnReport, btnSettings, btnHistory);

        VBox sliderBox = new VBox(4, sensitivityLabel, sensitivitySlider);
        sliderBox.setPadding(new Insets(0, 0, 0, 20));

        HBox bar = new HBox(12, sliderBox, sp, controls);
        return bar;
    }

    private Button makeBtn(String text, String color, boolean enabled) {
        Button b = new Button(text);
        b.setDisable(!enabled);
        b.setStyle(btnStyle(color));
        b.setOnMouseEntered(e -> { if (!b.isDisabled()) b.setStyle(btnHover(color)); });
        b.setOnMouseExited(e -> { if (!b.isDisabled()) b.setStyle(btnStyle(color)); });
        b.disabledProperty().addListener((obs, old, disabled) -> {
            b.setStyle(disabled ? btnDisabled() : btnStyle(color));
        });
        return b;
    }

    private String btnStyle(String color) {
        return "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-family: 'Segoe UI', sans-serif;" +
                "-fx-font-size: 13px; -fx-font-weight: 600;" +
                "-fx-background-radius: 10; -fx-padding: 12 16 12 16;" +
                "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, " + T.shadow + ", 4, 0, 0, 2);";
    }

    private String btnHover(String color) {
        return btnStyle(color) + "-fx-opacity: 0.85;";
    }

    private String btnDisabled() {
        return "-fx-background-color: " + T.surface2 + ";" +
                "-fx-text-fill: " + T.textMuted + ";" +
                "-fx-font-family: 'Segoe UI', sans-serif;" +
                "-fx-font-size: 13px; -fx-background-radius: 10; -fx-padding: 12 16 12 16;";
    }

    private GridPane buildMetricsGrid() {
        mDuration = metricVal("00:00");
        mFrames = metricVal("—");
        mThreshold = metricVal("—");
        mSpeaking = metricVal("—");
        mSpeakers = metricVal("—");
        mTurns = metricVal("—");
        mPeak = metricVal("—");
        mZcr = metricVal("—");
        mVad = metricVal("—");
        mQuality = metricVal("—");
        mWpm = metricVal("—");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        ColumnConstraints cc = new ColumnConstraints();
        cc.setHgrow(Priority.ALWAYS);
        cc.setPercentWidth(33.33);
        grid.getColumnConstraints().addAll(cc, cc, cc);

        grid.add(metricCard(mDuration, "DURATION", T.accent), 0, 0);
        grid.add(metricCard(mFrames, "FRAMES", T.accent), 1, 0);
        grid.add(metricCard(mThreshold, "THRESHOLD", T.amber), 2, 0);

        grid.add(metricCard(mSpeaking, "SPEAKING", T.teal), 0, 1);
        grid.add(metricCard(mSpeakers, "SPEAKERS", T.teal), 1, 1);
        grid.add(metricCard(mTurns, "TURNS", T.accent), 2, 1);

        grid.add(metricCard(mPeak, "PEAK RMS", T.red), 0, 2);
        grid.add(metricCard(mZcr, "ZCR", T.accent), 1, 2);
        grid.add(metricCard(mVad, "VAD %", T.teal), 2, 2);

        return grid;
    }

    private VBox buildDebugPanel() {
        Label title = new Label("DEBUG PANEL");
        title.setStyle(mono(10, T.textMuted, true));

        HBox metricsRow = new HBox(20);

        dRms = new Label("RMS: —");
        dFloor = new Label("Floor: —");
        dThr = new Label("Thr: —");
        dZcr = new Label("ZCR: —");
        dVad = new Label("VAD: —");
        dSpeaker = new Label("Spk: —");

        Label[] debugLabels = {dRms, dFloor, dThr, dZcr, dVad, dSpeaker};
        for (Label lbl : debugLabels) {
            lbl.setStyle(mono(10, T.textMuted, false));
        }

        energyBar = new ProgressBar(0);
        energyBar.setPrefWidth(200);
        energyBar.setStyle(
                "-fx-control-inner-background: " + T.surface2 + ";" +
                        "-fx-accent: " + T.accent + ";"
        );

        HBox energyRow = new HBox(8, new Label("Energy:"), energyBar);
        energyRow.setAlignment(Pos.CENTER_LEFT);

        metricsRow.getChildren().addAll(debugLabels);
        metricsRow.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(8, title, metricsRow, energyRow);
        panel.setPadding(new Insets(14, 18, 14, 18));
        panel.setStyle(cardStyle());

        if (!prefs.isShowDebugPanel()) {
            panel.setVisible(false);
        }

        return panel;
    }

    private VBox metricCard(Label val, String label, String color) {
        val.setStyle(
                "-fx-font-family: 'Segoe UI', sans-serif;" +
                        "-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: " + color + ";"
        );
        Label lbl = new Label(label);
        lbl.setStyle(mono(10, T.textMuted, true));

        Region accentBar = new Region();
        accentBar.setPrefHeight(3);
        accentBar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");
        accentBar.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(6, val, lbl, accentBar);
        card.setPadding(new Insets(16, 18, 12, 18));
        card.setStyle(cardStyle() + "-fx-effect: dropshadow(gaussian, " + T.shadow + ", 4, 0, 0, 2);");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private Label metricVal(String text) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-font-family: 'Segoe UI', sans-serif;" +
                        "-fx-font-size: 14px; -fx-text-fill: " + T.text + ";"
        );
        return l;
    }

    private VBox buildLogSection() {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(4, Color.web(T.teal));
        FadeTransition ft = new FadeTransition(Duration.seconds(1.2), dot);
        ft.setFromValue(1); ft.setToValue(0.3);
        ft.setAutoReverse(true); ft.setCycleCount(Animation.INDEFINITE);
        ft.play();

        Label lbl = new Label("CONSOLE OUTPUT");
        lbl.setStyle(mono(10, T.textMuted, true));

        Button clearBtn = new Button("Clear");
        clearBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: " + T.textMuted + ";" +
                        "-fx-font-family: 'Segoe UI', sans-serif;" +
                        "-fx-font-size: 11px; -fx-font-weight: 500;" +
                        "-fx-border-color: " + T.border + "; -fx-border-radius: 6;" +
                        "-fx-padding: 4 12 4 12; -fx-cursor: hand;"
        );
        clearBtn.setOnAction(e -> logArea.clear());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(dot, lbl, sp, clearBtn);

        logArea = new TextArea("[ MeetPulse ready ]\n");
        logArea.setEditable(false);
        logArea.setPrefHeight(100);
        logArea.setWrapText(true);
        logArea.setStyle(
                "-fx-background-color: " + T.surface2 + ";" +
                        "-fx-control-inner-background: " + T.surface2 + ";" +
                        "-fx-font-family: 'Segoe UI', sans-serif;" +
                        "-fx-font-size: 12px; -fx-text-fill: " + T.text + ";" +
                        "-fx-border-color: " + T.border + ";" +
                        "-fx-border-radius: 10; -fx-background-radius: 10;"
        );

        VBox box = new VBox(10, header, logArea);
        box.setPadding(new Insets(14, 18, 14, 18));
        box.setStyle(cardStyle());
        return box;
    }

    private void setupKeyboardShortcuts() {
        root.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();

            if (code == KeyCode.SPACE) {
                if (!btnStart.isDisabled()) onStart();
            } else if (code == KeyCode.R && !e.isControlDown()) {
                if (!btnReset.isDisabled()) onReset();
            } else if (code == KeyCode.ESCAPE) {
                System.exit(0);
            }
        });
    }

    private void onStart() {
        btnStart.setDisable(true);
        btnStop.setDisable(false);
        btnReset.setDisable(true);
        btnReport.setDisable(true);

        elapsedSec.set(0);
        timerLabel.setText("00:00");
        uiFrameTick = 0;

        audioService = new AudioCaptureService();
        audioService.setSpeechMultiplier(prefs.getSensitivity());

        audioService.setOnFrame((rms, silent, phase) -> {
            liveRms = rms;
            liveSilent = silent;
            livePhase = phase;
            uiFrameTick++;

            synchronized (waveBuffer) {
                waveBuffer.addLast(rms);
                if (waveBuffer.size() > WAVE_SIZE) waveBuffer.pollFirst();
            }

            if (uiFrameTick % 6 == 0) {
                Platform.runLater(this::refreshMetrics);
            }
        });

        audioService.setOnLog(msg -> Platform.runLater(() -> appendLog(msg)));

        audioService.setOnPhaseChange(phase -> Platform.runLater(() -> {
            livePhase = phase;
            updatePhaseUI(phase);
        }));

        audioThread = new Thread(() -> {
            try {
                audioService.start();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    appendLog("ERROR: " + e.getMessage());
                    onReset();
                });
            }
        }, "meetpulse-audio");
        audioThread.setDaemon(true);
        audioThread.start();

        startTimerTick();
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
        btnStart.setDisable(true);

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
        audioService.stop();
        stopTimerTick();
        stopCalCountdown();

        audioService = new AudioCaptureService();
        liveRms = 0;
        liveZcr = 0;
        liveSilent = true;
        livePhase = Phase.IDLE;
        uiFrameTick = 0;

        synchronized (waveBuffer) {
            waveBuffer.clear();
            for (int i = 0; i < WAVE_SIZE; i++) waveBuffer.addLast(0.0);
        }

        elapsedSec.set(0);
        timerLabel.setText("00:00");
        calOverlay.setVisible(false);
        updatePhaseUI(Phase.IDLE);

        mDuration.setText("00:00");
        mFrames.setText("—");
        mThreshold.setText("—");
        mSpeaking.setText("—");
        mSpeakers.setText("—");
        mTurns.setText("—");
        mPeak.setText("—");
        mZcr.setText("—");
        mVad.setText("—");
        mQuality.setText("—");
        mWpm.setText("—");
        qualityScoreLabel.setText("—");

        btnStart.setDisable(false);
        btnStop.setDisable(true);
        btnReset.setDisable(true);
        btnReport.setDisable(true);

        appendLog("=== Session reset — ready to start ===");
    }

    private void onGenerateReport() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("PDF", "PDF", "JSON", "CSV");
        dialog.setTitle("Export Format");
        dialog.setHeaderText("Select export format");
        dialog.setContentText("Format:");

        dialog.showAndWait().ifPresent(format -> {
            String ext = format.toLowerCase();
            FileChooser fc = new FileChooser();
            fc.setTitle("Export Report");
            fc.setInitialFileName("meetpulse_report." + ext);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(format.toUpperCase() + " Files", "*." + ext));

            File file = fc.showSaveDialog(root.getScene().getWindow());
            if (file == null) return;

            btnReport.setDisable(true);
            btnReport.setText("Exporting...");

            new Thread(() -> {
                try {
                    ExportService.ExportFormat exportFormat = ExportService.ExportFormat.valueOf(format.toUpperCase());
                    exportService.exportSession(audioService, file.getAbsolutePath(), exportFormat);

                    if (prefs.isAutoSaveSession()) {
                        saveCurrentSession();
                    }

                    Platform.runLater(() -> {
                        appendLog("Export saved → " + file.getName());
                        btnReport.setText("⬇  Export");
                        btnReport.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        appendLog("Export error: " + ex.getMessage());
                        btnReport.setText("⬇  Export");
                        btnReport.setDisable(false);
                    });
                }
            }).start();
        });
    }

    private void saveCurrentSession() {
        MeetingSession session = new MeetingSession();
        session.setDurationMs(elapsedSec.get() * 1000L);
        session.setSpeakingRatio(audioService.getAnalyzer().getLiveSpeakingPct() / 100.0);
        session.setSpeakerCount(audioService.getEstimatedSpeakers());
        session.setTotalTurns(audioService.getSpeakerTurnCount());
        session.setPeakRms(audioService.getAnalyzer().getLivePeakRms());
        session.setAvgNoiseFloor(audioService.getNoiseFloor());
        session.setAvgThreshold(audioService.getThreshold());
        session.setQualityScore(exportService.calculateQualityScore(audioService));
        session.setEstimatedWpm(exportService.calculateWpm(audioService));
        history.saveSession(session);
    }

    private void showSettingsDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("MeetPulse Settings");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        CheckBox autoSaveCheck = new CheckBox("Auto-save sessions");
        autoSaveCheck.setSelected(prefs.isAutoSaveSession());
        autoSaveCheck.setOnAction(e -> {
            prefs.setAutoSaveSession(autoSaveCheck.isSelected());
            prefs.savePreferences();
        });

        CheckBox debugCheck = new CheckBox("Show debug panel");
        debugCheck.setSelected(prefs.isShowDebugPanel());
        debugCheck.setOnAction(e -> {
            prefs.setShowDebugPanel(debugCheck.isSelected());
            prefs.savePreferences();
        });

        Label resetLabel = new Label("Reset to Defaults:");
        Button resetBtn = new Button("Reset Settings");
        resetBtn.setOnAction(e -> {
            prefs.resetToDefaults();
            prefs.savePreferences();
            autoSaveCheck.setSelected(prefs.isAutoSaveSession());
            debugCheck.setSelected(prefs.isShowDebugPanel());
            sensitivitySlider.setValue(prefs.getSensitivity());
            sensitivityLabel.setText("Sensitivity: " + String.format("%.1fx", prefs.getSensitivity()));
            appendLog("Settings reset to defaults");
        });

        Label infoLabel = new Label("Sessions stored at:\n" + history.getHistoryPath());

        content.getChildren().addAll(autoSaveCheck, debugCheck, new Separator(), resetLabel, resetBtn, new Separator(), infoLabel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: " + T.surface + ";" +
                "-fx-border-color: " + T.border + ";" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;"
        );

        dialog.showAndWait();
    }

    private void showHistoryDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Session History");
        dialog.setHeaderText("Past Meeting Sessions");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        List<MeetingSession> sessions = history.getRecentSessions(20);

        if (sessions.isEmpty()) {
            content.getChildren().add(new Label("No sessions saved yet.\nStart recording to save sessions."));
        } else {
            for (MeetingSession s : sessions) {
                HBox sessionRow = new HBox(20);
                sessionRow.getChildren().addAll(
                        new Label(s.getId()),
                        new Label(s.getFormattedDate()),
                        new Label(String.format("Score: %.0f", s.getQualityScore())),
                        new Label(String.format("Speaking: %.0f%%", s.getSpeakingRatio() * 100))
                );
                content.getChildren().add(sessionRow);
            }

            Button clearBtn = new Button("Clear History");
            clearBtn.setOnAction(e -> {
                history.clearHistory();
                appendLog("Session history cleared");
                dialog.close();
                showHistoryDialog();
            });
            content.getChildren().add(new Separator());
            content.getChildren().add(clearBtn);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: " + T.surface + ";" +
                "-fx-border-color: " + T.border + ";" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;"
        );

        dialog.showAndWait();
    }

    private void updatePhaseUI(Phase phase) {
        switch (phase) {
            case IDLE -> {
                phaseDot.setFill(Color.web(T.textMuted));
                setDotGlow(Color.web(T.textMuted));
                stopDotPulse();
                setStatus("Ready — press Start to begin", T.textMuted);
            }
            case CALIBRATING -> {
                phaseDot.setFill(Color.web(T.amber));
                setDotGlow(Color.web(T.amber));
                startDotPulse(Color.web(T.amber));
                setStatus("Calibrating — stay silent...", T.amber);
                calOverlay.setVisible(true);
            }
            case RECORDING -> {
                phaseDot.setFill(Color.web(T.teal));
                setDotGlow(Color.web(T.teal));
                startDotPulse(Color.web(T.teal));
                setStatus("Recording — adaptive threshold active", T.teal);
                calOverlay.setVisible(false);
            }
            case STOPPED -> {
                phaseDot.setFill(Color.web(T.red));
                setDotGlow(Color.web(T.red));
                stopDotPulse();
                setStatus("Stopped — export or reset", T.textMuted);
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
        statusLabel.setStyle(mono(13, color, true));
    }

    private void refreshMetrics() {
        if (livePhase == Phase.IDLE) return;

        mFrames.setText(String.valueOf(audioService.getLiveFrameCount()));
        mSpeaking.setText(String.format("%.1f%%", audioService.getAnalyzer().getLiveSpeakingPct()));
        mPeak.setText(String.format("%.0f", audioService.getAnalyzer().getLivePeakRms()));
        mZcr.setText(String.format("%.3f", audioService.getLiveZcr()));
        mVad.setText(String.format("%.0f%%", audioService.getLiveSpeechLikelihood() * 100));
        mSpeakers.setText(String.valueOf(audioService.getEstimatedSpeakers()));
        mTurns.setText(String.valueOf(audioService.getSpeakerTurnCount()));
        mThreshold.setText(String.format("%.0f", audioService.getThreshold()));

        int s = elapsedSec.get();
        mDuration.setText(String.format("%02d:%02d", s / 60, s % 60));

        double quality = exportService.calculateQualityScore(audioService);
        mQuality.setText(String.format("%.0f", quality));
        qualityScoreLabel.setText(String.format("Q: %.0f", quality));

        double wpm = exportService.calculateWpm(audioService);
        mWpm.setText(String.format("%.0f", wpm));

        dRms.setText(String.format("RMS: %.0f", liveRms));
        dFloor.setText(String.format("Floor: %.0f", audioService.getNoiseFloor()));
        dThr.setText(String.format("Thr: %.0f", audioService.getThreshold()));
        dZcr.setText(String.format("ZCR: %.3f", audioService.getLiveZcr()));
        dVad.setText(String.format("VAD: %.0f%%", audioService.getLiveSpeechLikelihood() * 100));
        dSpeaker.setText(String.format("Spk: %d", audioService.getEstimatedSpeakers()));

        double maxRms = audioService.getAnalyzer().getLivePeakRms();
        double energyLevel = maxRms > 0 ? Math.min(liveRms / maxRms, 1.0) : 0;
        energyBar.setProgress(energyLevel);
    }

    private void startTimerTick() {
        stopTimerTick();
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            int s = elapsedSec.incrementAndGet();
            timerLabel.setText(String.format("%02d:%02d", s / 60, s % 60));
            mDuration.setText(String.format("%02d:%02d", s / 60, s % 60));
        }));
        timerTimeline.setCycleCount(Animation.INDEFINITE);
        timerTimeline.play();
    }

    private void stopTimerTick() {
        if (timerTimeline != null) { timerTimeline.stop(); timerTimeline = null; }
    }

    private void stopCalCountdown() {
        if (calCountdown != null) { calCountdown.stop(); calCountdown = null; }
    }

    private void appendLog(String msg) {
        logArea.appendText(msg + "\n");
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    private String cardStyle() {
        return "-fx-background-color: " + T.surface + ";" +
                "-fx-border-color: " + T.border + ";" +
                "-fx-border-radius: 12; -fx-background-radius: 12;";
    }

    private String mono(int size, String color, boolean weight500) {
        String weight = weight500 ? "500" : "400";
        return "-fx-font-family: 'Segoe UI', sans-serif;" +
                "-fx-font-size: " + size + "px; -fx-font-weight: " + weight + "; -fx-text-fill: " + color + ";";
    }
}