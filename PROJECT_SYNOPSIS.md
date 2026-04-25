# MeetPulse Project Synopsis

## 1. Project Overview

**MeetPulse** is a Java-based desktop application focused on **privacy-first meeting quality analysis** using only low-level acoustic signal behavior. The project intentionally avoids speech-to-text or semantic content extraction and instead evaluates session engagement through metrics derived from amplitude patterns over time.

Core principle:
- Capture signal activity
- Derive engagement-oriented metrics
- Generate visual post-meeting analysis
- Preserve privacy by design (no transcript pipeline)

This positions MeetPulse as a lightweight alternative for teams who care about interaction quality but do not want conversation content processed.

## 2. Problem Statement

Most meeting analytics tools optimize for “what was said” (transcripts, summaries, semantic action items). MeetPulse addresses a different question:

- How active was the meeting?
- Were there long silent periods?
- Was speaking continuous or fragmented?
- Did session momentum improve or decay over time?

The project treats these as **signal-structure problems**, not NLP problems.

## 3. Product Goals

1. Provide real-time meeting activity tracking during session runtime.
2. Produce a polished, visual, post-session PDF report.
3. Keep the system local and operational without cloud dependencies.
4. Maintain predictable runtime performance on commodity hardware.

## 4. Current Tech Stack

- Language: **Java 21**
- UI: **JavaFX**
- Audio Capture: **javax.sound.sampled** (`TargetDataLine`)
- Report Engine: **iText PDF**
- Charts: **JFreeChart**
- Build Tool: **Maven**

## 5. High-Level System Architecture

MeetPulse follows a layered flow:

1. **Capture Layer**
- `AudioCaptureService`
- Pulls PCM frames from microphone input using `TargetDataLine`.

2. **Signal Processing Layer**
- `EnergyCalculator`
- Converts PCM frames to RMS energy values.
- Applies smoothed behavior in capture service via EMA.

3. **Activity Classification Layer**
- `SilenceDetector`
- Uses threshold + hysteresis (speech threshold and silence threshold) to avoid rapid state toggles.

4. **Session Analytics Layer**
- `MeetingAnalyzer`
- Stores frame sequence and constructs speaking segments.
- Computes live and final metrics (speaking %, peak RMS, segment counts, per-second timeline).

5. **Presentation Layer (Live UI)**
- `MeetPulseUI`
- Renders waveform + KPI cards and session state transitions.

6. **Report Layer**
- `PdfReportGenerator`
- Builds multi-page visual report with KPIs, charts, speaking dynamics, timeline moments, technical details, insight narrative, and action plan.

## 6. Runtime Flow (Detailed)

### 6.1 Session Start
- UI initializes `AudioCaptureService` callbacks.
- Capture service enters `CALIBRATING` phase.

### 6.2 Calibration Window
- Audio frames are read for ~3.5 seconds.
- Raw RMS is smoothed with EMA (`EMA_ALPHA`).
- Calibration sample distribution is collected.

### 6.3 Threshold Derivation
Threshold is computed using robust statistics:
- `p20` / `p75` and median-based floor logic
- adaptive threshold formula with floor anchoring and clamping
- detector configured with hysteresis (`speechThreshold`, `silenceThreshold = 0.82 * speechThreshold`)

### 6.4 Recording Phase
- Continuous frame capture
- Raw RMS + smoothed RMS tracked live
- Slowly adaptive noise-floor updates when signal appears near-silent
- frame classified as speaking/silent through hysteresis detector
- frame sent to analyzer and UI callbacks

### 6.5 Stop + Summary
- Analyzer finalizes open speaking segments
- Session statistics are summarized
- PDF report can be generated from accumulated timeline + stats

## 7. Core Data Models

### 7.1 `EnergyFrame`
Represents one analyzed moment:
- timestamp
- RMS value
- silence flag

### 7.2 `SpeakingSegment`
Represents contiguous non-silent intervals:
- start time
- end time
- duration

### 7.3 `MeetingStats`
Session summary model containing:
- total/silent frame counts
- average and peak RMS
- total duration
- immutable speaking segment list

## 8. UI/UX Design Synopsis

The current UI is a real-time monitoring console with:

- status lifecycle indicator (`IDLE`, `CALIBRATING`, `RECORDING`, `STOPPED`)
- animated waveform panel with threshold overlay
- live cards:
  - duration
  - frame count
  - speaking %
  - segment count
  - peak RMS
  - threshold
  - raw RMS
  - smoothed RMS
  - noise floor
- session logs with state-change events
- one-click PDF report export

Design language:
- dark operational dashboard style
- monospaced typography emphasis for metric readability
- color-coded state semantics (teal/amber/red)

## 9. Reporting System Synopsis

The PDF report is the project’s core deliverable artifact.

### 9.1 Report Sections
1. **Cover Page**
- light visual theme with dark readable text
- session score gauge
- tier badge
- top metrics tiles

2. **Session Overview**
- KPI strip for fast executive reading

3. **Energy Timeline**
- per-second RMS over time
- threshold marker overlay

4. **Audio Distribution**
- speaking vs silence ring chart
- RMS histogram

5. **Engagement Profile**
- phase energy chart (opening/middle/closing)
- momentum trend chart (raw + rolling baseline)

6. **Speaking Dynamics**
- talk coverage
- segment rate
- median segment duration
- longest burst

7. **Key Timeline Moments**
- peak energy moment
- active range
- longest quiet window
- threshold-crossing diagnostics

8. **Technical Details**
- capture format, threshold values, frame stats, duration

9. **Insight + Executive Action Plan**
- narrative explanation based on session behavior
- actionable recommendations in prioritized cards

### 9.2 Reporting Value
The report is designed for two audiences:
- **Operator/Engineer**: diagnostics and threshold confidence
- **Meeting owner/manager**: engagement interpretation and actionable guidance

## 10. Privacy Model

Privacy posture is based on **signal-level analytics**:
- no transcript generation pipeline
- no semantic content extraction
- no LLM dependence for core measurement
- local computation architecture

Important practical note:
- Audio frames are processed live for metric computation; report stores aggregated analysis, not text transcript.

## 11. Current Strengths

1. Strong end-to-end architecture from capture to polished PDF.
2. Robust adaptive thresholding compared to static single-threshold logic.
3. Good practical dashboard for live debugging and confidence checks.
4. Report quality now significantly above “basic charts + raw table” level.
5. Clear separation of concerns in code organization.

## 12. Current Limitations

1. **Input source scope**
- Current active capture path is mic-based `TargetDataLine` flow.
- System/loopback capture may require platform-specific extension or selectable device strategy.

2. **No speaker diarization**
- Segments are activity segments, not speaker-attributed entities.

3. **No semantic insight**
- Intentional design choice, but means no action-item extraction from language.

4. **No multi-session trend store**
- Report is per-session; longitudinal analytics are not yet first-class.

5. **Calibration sensitivity**
- Real environments can still vary; setup quality strongly influences metrics.

## 13. Suggested Next Iteration Priorities

1. Add explicit input-device manager and source testing wizard.
2. Introduce cross-session baseline comparison mode.
3. Add configurable profiles (quiet room, shared office, conference room).
4. Add report templates by meeting type (standup, 1:1, review, interview).
5. Add optional local persistence layer for trend dashboards.

## 14. Build and Run

From project root:

```bash
mvn clean package
mvn javafx:run
```

Generate report from UI after stopping a session.

## 15. Codebase Map

- Entry point: `src/main/java/com/meetpulse/MainApp.java`
- UI: `src/main/java/com/meetpulse/ui/MeetPulseUI.java`
- Capture: `src/main/java/com/meetpulse/audio/AudioCaptureService.java`
- Processing: `src/main/java/com/meetpulse/processing/EnergyCalculator.java`, `SilenceDetector.java`
- Analytics: `src/main/java/com/meetpulse/service/MeetingAnalyzer.java`
- Models: `src/main/java/com/meetpulse/model/*.java`
- Reporting: `src/main/java/com/meetpulse/report/PdfReportGenerator.java`

## 16. Synopsis Conclusion

MeetPulse is a focused, privacy-oriented meeting analytics application that emphasizes **interaction signal quality over conversational semantics**. It already provides a complete operational loop (capture → classify → summarize → visualize → recommend), and its strongest differentiator is generating actionable engagement intelligence without relying on transcript-centered workflows.

