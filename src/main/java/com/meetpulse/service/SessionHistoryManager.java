package com.meetpulse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.meetpulse.model.MeetingSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SessionHistoryManager {

    private static final String HISTORY_DIR = "meetpulse_sessions";
    private static final String SESSIONS_FILE = "sessions.json";
    private static final int MAX_SESSIONS = 50;

    private final Path historyPath;
    private final Path sessionsFile;
    private final ObjectMapper objectMapper;
    private List<MeetingSession> sessions;

    public SessionHistoryManager() {
        String userHome = System.getProperty("user.home");
        historyPath = Paths.get(userHome, HISTORY_DIR);
        sessionsFile = historyPath.resolve(SESSIONS_FILE);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        sessions = new ArrayList<>();
        ensureDirectoryExists();
        loadSessions();
    }

    private void ensureDirectoryExists() {
        try {
            if (!Files.exists(historyPath)) {
                Files.createDirectories(historyPath);
            }
        } catch (IOException e) {
            System.err.println("Failed to create history directory: " + e.getMessage());
        }
    }

    public void saveSession(MeetingSession session) {
        sessions.add(0, session);

        if (sessions.size() > MAX_SESSIONS) {
            sessions = new ArrayList<>(sessions.subList(0, MAX_SESSIONS));
        }

        persistSessions();
    }

    public List<MeetingSession> getSessions() {
        return new ArrayList<>(sessions);
    }

    public List<MeetingSession> getRecentSessions(int count) {
        List<MeetingSession> recent = new ArrayList<>();
        for (int i = 0; i < Math.min(count, sessions.size()); i++) {
            recent.add(sessions.get(i));
        }
        return recent;
    }

    public MeetingSession getSessionById(String id) {
        for (MeetingSession session : sessions) {
            if (session.getId().equals(id)) {
                return session;
            }
        }
        return null;
    }

    public void deleteSession(String id) {
        sessions.removeIf(s -> s.getId().equals(id));
        persistSessions();
    }

    public void clearHistory() {
        sessions.clear();
        persistSessions();
    }

    public MeetingSession getAverageStats() {
        if (sessions.isEmpty()) return null;

        MeetingSession avg = new MeetingSession();
        double totalSpeakingRatio = 0;
        double totalQualityScore = 0;
        double totalWpm = 0;
        double totalDuration = 0;
        int totalSpeakers = 0;
        int totalTurns = 0;

        for (MeetingSession s : sessions) {
            totalSpeakingRatio += s.getSpeakingRatio();
            totalQualityScore += s.getQualityScore();
            totalWpm += s.getEstimatedWpm();
            totalDuration += s.getDurationMs();
            totalSpeakers += s.getSpeakerCount();
            totalTurns += s.getTotalTurns();
        }

        int count = sessions.size();
        avg.setSpeakingRatio(totalSpeakingRatio / count);
        avg.setQualityScore(totalQualityScore / count);
        avg.setEstimatedWpm(totalWpm / count);
        avg.setDurationMs((long) (totalDuration / count));
        avg.setSpeakerCount(totalSpeakers / count);
        avg.setTotalTurns(totalTurns / count);

        return avg;
    }

    private void loadSessions() {
        try {
            if (Files.exists(sessionsFile)) {
                MeetingSession[] loaded = objectMapper.readValue(sessionsFile.toFile(), MeetingSession[].class);
                sessions = new ArrayList<>(java.util.Arrays.asList(loaded));
                Collections.sort(sessions, Comparator.comparingLong(MeetingSession::getTimestamp).reversed());
            }
        } catch (IOException e) {
            System.err.println("Failed to load sessions: " + e.getMessage());
            sessions = new ArrayList<>();
        }
    }

    private void persistSessions() {
        try {
            objectMapper.writeValue(sessionsFile.toFile(), sessions);
        } catch (IOException e) {
            System.err.println("Failed to save sessions: " + e.getMessage());
        }
    }

    public String getHistoryPath() {
        return historyPath.toString();
    }

    public int getSessionCount() {
        return sessions.size();
    }
}