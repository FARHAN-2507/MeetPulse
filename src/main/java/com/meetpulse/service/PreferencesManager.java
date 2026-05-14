package com.meetpulse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.meetpulse.ui.ThemeManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PreferencesManager {

    private static final String CONFIG_DIR = ".meetpulse";
    private static final String PREFS_FILE = "preferences.json";

    private final Path configPath;
    private final ObjectMapper objectMapper;

    private ThemeManager.Theme theme = ThemeManager.Theme.LIGHT;
    private double sensitivity = 1.6;
    private int calibrationDurationMs = 3500;
    private int minSpeechFrames = 3;
    private int minSilenceFrames = 6;
    private boolean showDebugPanel = true;
    private boolean showSpectrumAnalyzer = true;
    private boolean minimizeToTray = false;
    private boolean autoSaveSession = true;
    private String defaultExportFormat = "PDF";

    public PreferencesManager() {
        String userHome = System.getProperty("user.home");
        configPath = Paths.get(userHome, CONFIG_DIR);

        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ensureDirectoryExists();
        loadPreferences();
    }

    private void ensureDirectoryExists() {
        try {
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
            }
        } catch (IOException e) {
            System.err.println("Failed to create config directory: " + e.getMessage());
        }
    }

    public void savePreferences() {
        try {
            Path prefsFile = configPath.resolve(PREFS_FILE);
            objectMapper.writeValue(prefsFile.toFile(), this);
        } catch (IOException e) {
            System.err.println("Failed to save preferences: " + e.getMessage());
        }
    }

    public void loadPreferences() {
        try {
            Path prefsFile = configPath.resolve(PREFS_FILE);
            if (Files.exists(prefsFile)) {
                PreferencesManager loaded = objectMapper.readValue(prefsFile.toFile(), PreferencesManager.class);
                this.theme = loaded.theme;
                this.sensitivity = loaded.sensitivity;
                this.calibrationDurationMs = loaded.calibrationDurationMs;
                this.minSpeechFrames = loaded.minSpeechFrames;
                this.minSilenceFrames = loaded.minSilenceFrames;
                this.showDebugPanel = loaded.showDebugPanel;
                this.showSpectrumAnalyzer = loaded.showSpectrumAnalyzer;
                this.minimizeToTray = loaded.minimizeToTray;
                this.autoSaveSession = loaded.autoSaveSession;
                this.defaultExportFormat = loaded.defaultExportFormat;
            }
        } catch (IOException e) {
            System.err.println("Failed to load preferences: " + e.getMessage());
        }
    }

    public ThemeManager.Theme getTheme() { return theme; }
    public void setTheme(ThemeManager.Theme theme) { this.theme = theme; }

    public double getSensitivity() { return sensitivity; }
    public void setSensitivity(double sensitivity) { this.sensitivity = sensitivity; }

    public int getCalibrationDurationMs() { return calibrationDurationMs; }
    public void setCalibrationDurationMs(int calibrationDurationMs) { this.calibrationDurationMs = calibrationDurationMs; }

    public int getMinSpeechFrames() { return minSpeechFrames; }
    public void setMinSpeechFrames(int minSpeechFrames) { this.minSpeechFrames = minSpeechFrames; }

    public int getMinSilenceFrames() { return minSilenceFrames; }
    public void setMinSilenceFrames(int minSilenceFrames) { this.minSilenceFrames = minSilenceFrames; }

    public boolean isShowDebugPanel() { return showDebugPanel; }
    public void setShowDebugPanel(boolean showDebugPanel) { this.showDebugPanel = showDebugPanel; }

    public boolean isShowSpectrumAnalyzer() { return showSpectrumAnalyzer; }
    public void setShowSpectrumAnalyzer(boolean showSpectrumAnalyzer) { this.showSpectrumAnalyzer = showSpectrumAnalyzer; }

    public boolean isMinimizeToTray() { return minimizeToTray; }
    public void setMinimizeToTray(boolean minimizeToTray) { this.minimizeToTray = minimizeToTray; }

    public boolean isAutoSaveSession() { return autoSaveSession; }
    public void setAutoSaveSession(boolean autoSaveSession) { this.autoSaveSession = autoSaveSession; }

    public String getDefaultExportFormat() { return defaultExportFormat; }
    public void setDefaultExportFormat(String defaultExportFormat) { this.defaultExportFormat = defaultExportFormat; }

    public void resetToDefaults() {
        this.theme = ThemeManager.Theme.LIGHT;
        this.sensitivity = 1.6;
        this.calibrationDurationMs = 3500;
        this.minSpeechFrames = 3;
        this.minSilenceFrames = 6;
        this.showDebugPanel = true;
        this.showSpectrumAnalyzer = true;
        this.minimizeToTray = false;
        this.autoSaveSession = true;
        this.defaultExportFormat = "PDF";
    }
}