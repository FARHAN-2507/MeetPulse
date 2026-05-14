package com.meetpulse.ui;

import javafx.scene.paint.Color;

public class ThemeManager {

    public enum Theme { LIGHT, DARK }

    private static Theme currentTheme = Theme.LIGHT;

    public static class ThemeColors {
        public final String bg;
        public final String surface;
        public final String surface2;
        public final String border;
        public final String text;
        public final String textMuted;
        public final String accent;
        public final String accentHover;
        public final String teal;
        public final String amber;
        public final String red;
        public final String success;
        public final String shadow;

        public final String cardBg;
        public final String cardBorder;
        public final String inputBg;
        public final String sliderTrack;
        public final String sliderThumb;
        public final String logBg;
        public final String logText;
        public final String waveBg;
        public final String scrollbarThumb;
        public final String scrollbarTrack;

        public ThemeColors(String bg, String surface, String surface2, String border,
                          String text, String textMuted, String accent, String accentHover,
                          String teal, String amber, String red, String success, String shadow,
                          String cardBg, String cardBorder, String inputBg, String sliderTrack,
                          String sliderThumb, String logBg, String logText, String waveBg,
                          String scrollbarThumb, String scrollbarTrack) {
            this.bg = bg;
            this.surface = surface;
            this.surface2 = surface2;
            this.border = border;
            this.text = text;
            this.textMuted = textMuted;
            this.accent = accent;
            this.accentHover = accentHover;
            this.teal = teal;
            this.amber = amber;
            this.red = red;
            this.success = success;
            this.shadow = shadow;
            this.cardBg = cardBg;
            this.cardBorder = cardBorder;
            this.inputBg = inputBg;
            this.sliderTrack = sliderTrack;
            this.sliderThumb = sliderThumb;
            this.logBg = logBg;
            this.logText = logText;
            this.waveBg = waveBg;
            this.scrollbarThumb = scrollbarThumb;
            this.scrollbarTrack = scrollbarTrack;
        }
    }

    public static final ThemeColors LIGHT = new ThemeColors(
            "#f8fafc", "#ffffff", "#f1f5f9", "#e2e8f0",
            "#1e293b", "#64748b", "#4f8ef7", "#3a7ae8",
            "#10b981", "#f59e0b", "#ef4444", "#22c55e", "rgba(0,0,0,0.08)",
            "#ffffff", "#e2e8f0", "#f8fafc", "#e2e8f0",
            "#4f8ef7", "#f1f5f9", "#374151", "#f1f5f9",
            "#cbd5e1", "transparent"
    );

    public static final ThemeColors DARK = new ThemeColors(
            "#0f172a", "#1e293b", "#334155", "#475569",
            "#f1f5f9", "#94a3b8", "#60a5fa", "#93c5fd",
            "#34d399", "#fbbf24", "#f87171", "#4ade80", "rgba(0,0,0,0.3)",
            "#1e293b", "#334155", "#0f172a", "#334155",
            "#60a5fa", "#0f172a", "#94a3b8", "#1e293b",
            "#475569", "transparent"
    );

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static void setTheme(Theme theme) {
        currentTheme = theme;
    }

    public static void toggleTheme() {
        currentTheme = (currentTheme == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;
    }

    public static ThemeColors getColors() {
        return currentTheme == Theme.LIGHT ? LIGHT : DARK;
    }

    public static String getCss() {
        ThemeColors c = getColors();
        StringBuilder css = new StringBuilder();
        css.append("* { ")
           .append("-fx-background: ").append(c.bg).append("; ")
           .append("-fx-text-fill: ").append(c.text).append("; ")
           .append("}");
        return css.toString();
    }

    public static String getScrollbarCss() {
        ThemeColors c = getColors();
        return ".scroll-bar>.thumb{-fx-background-color:" + c.scrollbarThumb + ";-fx-background-radius:4;}" +
               ".scroll-bar>.track{-fx-background-color:" + c.scrollbarTrack + ";}";
    }
}