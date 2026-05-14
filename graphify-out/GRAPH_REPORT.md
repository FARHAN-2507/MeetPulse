# Graph Report - /home/mofa/MeetPluse/MeetPulse  (2026-05-14)

## Corpus Check
- 18 files · ~43,798 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 352 nodes · 781 edges · 14 communities detected
- Extraction: 64% EXTRACTED · 36% INFERRED · 0% AMBIGUOUS · INFERRED: 283 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]

## God Nodes (most connected - your core abstractions)
1. `MeetPulseUI` - 41 edges
2. `AudioCaptureService` - 35 edges
3. `PdfReportGenerator` - 34 edges
4. `MeetingSession` - 32 edges
5. `PreferencesManager` - 26 edges
6. `SpeakerTurnDetector` - 17 edges
7. `VoiceActivityDetector` - 16 edges
8. `SessionHistoryManager` - 14 edges
9. `ExportService` - 14 edges
10. `MeetingStats` - 10 edges

## Surprising Connections (you probably didn't know these)
- `Relative Silence Detection` --semantically_similar_to--> `Silence Threshold (4373.25 RMS)`  [INFERRED] [semantically similar]
  README.md → meetpulse_report.pdf
- `Microphone Capture via TargetDataLine` --conceptually_related_to--> `Session Insight: Very Low Speaking Activity`  [AMBIGUOUS]
  README.md → meetpulse_report.pdf
- `Graphify Knowledge Graph` --semantically_similar_to--> `Graphify Knowledge Graph`  [INFERRED] [semantically similar]
  AGENTS.md → CLAUDE.md
- `RMS Energy Calculation` --shares_data_with--> `Energy Timeline (RMS per Second)`  [INFERRED]
  README.md → meetpulse_report.pdf

## Hyperedges (group relationships)
- **Graphify Governance Loop** — agents_graphify_knowledge_graph, agents_graph_report, agents_wiki_index, agents_graph_update_command [EXTRACTED 1.00]
- **MeetPulse Privacy-Preserving Signal Architecture** — readme_targetdataline_capture, readme_rms_energy_calculation, readme_adaptive_noise_floor, readme_relative_silence_detection, readme_privacy_first_principle [INFERRED 0.87]
- **Low Activity Interpretation Bundle** — report_speaking_ratio_2_2_percent, report_speaking_segments_table, report_session_insight_low_speaking, report_silence_threshold [INFERRED 0.84]

## Communities

### Community 0 - "Community 0"
Cohesion: 0.11
Nodes (3): MeetingStats, PdfReportGenerator, ReportExporter

### Community 1 - "Community 1"
Cohesion: 0.08
Nodes (2): MeetingSession, SessionHistoryManager

### Community 2 - "Community 2"
Cohesion: 0.09
Nodes (4): AudioCaptureService, TriConsumer, EnergyCalculator, SilenceDetector

### Community 3 - "Community 3"
Cohesion: 0.13
Nodes (1): MeetPulseUI

### Community 4 - "Community 4"
Cohesion: 0.18
Nodes (2): ExportService, MeetingAnalyzer

### Community 5 - "Community 5"
Cohesion: 0.1
Nodes (1): PreferencesManager

### Community 6 - "Community 6"
Cohesion: 0.09
Nodes (3): VoiceMetrics, DetectionResult, VoiceActivityDetector

### Community 7 - "Community 7"
Cohesion: 0.13
Nodes (3): SpeakerAnalysis, SpeakerTurn, SpeakerTurnDetector

### Community 8 - "Community 8"
Cohesion: 0.16
Nodes (3): MainApp, ThemeColors, ThemeManager

### Community 9 - "Community 9"
Cohesion: 0.18
Nodes (2): EnergyFrame, SpeakingSegment

### Community 10 - "Community 10"
Cohesion: 0.22
Nodes (11): Adaptive Noise Floor Calibration, Relative Silence Detection, RMS Energy Calculation, Microphone Capture via TargetDataLine, Audio Format 44.1kHz Mono 16-bit PCM, Energy Timeline (RMS per Second), Meeting Analysis Report, Session Insight: Very Low Speaking Activity (+3 more)

### Community 11 - "Community 11"
Cohesion: 0.28
Nodes (9): Design Rationale: Privacy-Friendly, Lightweight, Scalable, Design Rationale: Signal Behavior Instead of Semantics, 100% Local Processing, Meeting Energy and Quality Analysis, MeetPulse Application, No Audio Recording Policy, No Transcription Policy, Privacy-First Principle (+1 more)

### Community 12 - "Community 12"
Cohesion: 0.25
Nodes (8): Graph Report (God Nodes and Community Structure), Graphify Update Command, Graphify Knowledge Graph, Graphify Wiki Index Navigation, Graph Report (God Nodes and Community Structure), Graphify Update Command, Graphify Knowledge Graph, Graphify Wiki Index Navigation

### Community 13 - "Community 13"
Cohesion: 0.5
Nodes (1): FooterEvent

## Ambiguous Edges - Review These
- `Microphone Capture via TargetDataLine` → `Session Insight: Very Low Speaking Activity`  [AMBIGUOUS]
  meetpulse_report.pdf · relation: conceptually_related_to

## Knowledge Gaps
- **11 isolated node(s):** `Graph Report (God Nodes and Community Structure)`, `Graphify Wiki Index Navigation`, `Graphify Update Command`, `Graph Report (God Nodes and Community Structure)`, `Graphify Wiki Index Navigation` (+6 more)
  These have ≤1 connection - possible missing edges or undocumented components.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Microphone Capture via TargetDataLine` and `Session Insight: Very Low Speaking Activity`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `MeetPulseUI` connect `Community 3` to `Community 8`, `Community 1`, `Community 4`, `Community 5`?**
  _High betweenness centrality (0.158) - this node is a cross-community bridge._
- **Why does `AudioCaptureService` connect `Community 2` to `Community 3`, `Community 4`?**
  _High betweenness centrality (0.108) - this node is a cross-community bridge._
- **Why does `PreferencesManager` connect `Community 5` to `Community 8`?**
  _High betweenness centrality (0.090) - this node is a cross-community bridge._
- **What connects `Graph Report (God Nodes and Community Structure)`, `Graphify Wiki Index Navigation`, `Graphify Update Command` to the rest of the system?**
  _11 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.11 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.08 - nodes in this community are weakly interconnected._