# Graph Report - /home/mofa/MeetPluse/MeetPulse  (2026-04-26)

## Corpus Check
- 11 files · ~25,330 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 184 nodes · 389 edges · 11 communities detected
- Extraction: 73% EXTRACTED · 27% INFERRED · 0% AMBIGUOUS · INFERRED: 104 edges (avg confidence: 0.8)
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

## God Nodes (most connected - your core abstractions)
1. `MeetPulseUI` - 34 edges
2. `PdfReportGenerator` - 34 edges
3. `AudioCaptureService` - 27 edges
4. `MeetingStats` - 10 edges
5. `MeetingAnalyzer` - 10 edges
6. `SpeakingSegment` - 8 edges
7. `Privacy-First Principle` - 6 edges
8. `SilenceDetector` - 5 edges
9. `EnergyFrame` - 5 edges
10. `Graphify Knowledge Graph` - 4 edges

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
Nodes (4): EnergyFrame, MeetingStats, ReportExporter, SpeakingSegment

### Community 1 - "Community 1"
Cohesion: 0.13
Nodes (1): MeetPulseUI

### Community 2 - "Community 2"
Cohesion: 0.16
Nodes (1): PdfReportGenerator

### Community 3 - "Community 3"
Cohesion: 0.16
Nodes (3): AudioCaptureService, TriConsumer, EnergyCalculator

### Community 4 - "Community 4"
Cohesion: 0.18
Nodes (1): MeetingAnalyzer

### Community 5 - "Community 5"
Cohesion: 0.22
Nodes (11): Adaptive Noise Floor Calibration, Relative Silence Detection, RMS Energy Calculation, Microphone Capture via TargetDataLine, Audio Format 44.1kHz Mono 16-bit PCM, Energy Timeline (RMS per Second), Meeting Analysis Report, Session Insight: Very Low Speaking Activity (+3 more)

### Community 6 - "Community 6"
Cohesion: 0.28
Nodes (9): Design Rationale: Privacy-Friendly, Lightweight, Scalable, Design Rationale: Signal Behavior Instead of Semantics, 100% Local Processing, Meeting Energy and Quality Analysis, MeetPulse Application, No Audio Recording Policy, No Transcription Policy, Privacy-First Principle (+1 more)

### Community 7 - "Community 7"
Cohesion: 0.25
Nodes (8): Graph Report (God Nodes and Community Structure), Graphify Update Command, Graphify Knowledge Graph, Graphify Wiki Index Navigation, Graph Report (God Nodes and Community Structure), Graphify Update Command, Graphify Knowledge Graph, Graphify Wiki Index Navigation

### Community 8 - "Community 8"
Cohesion: 0.4
Nodes (1): SilenceDetector

### Community 9 - "Community 9"
Cohesion: 0.5
Nodes (1): MainApp

### Community 10 - "Community 10"
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
- **Why does `MeetPulseUI` connect `Community 1` to `Community 4`?**
  _High betweenness centrality (0.164) - this node is a cross-community bridge._
- **Why does `AudioCaptureService` connect `Community 3` to `Community 1`, `Community 4`?**
  _High betweenness centrality (0.146) - this node is a cross-community bridge._
- **Why does `PdfReportGenerator` connect `Community 2` to `Community 0`, `Community 10`?**
  _High betweenness centrality (0.102) - this node is a cross-community bridge._
- **What connects `Graph Report (God Nodes and Community Structure)`, `Graphify Wiki Index Navigation`, `Graphify Update Command` to the rest of the system?**
  _11 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.11 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.13 - nodes in this community are weakly interconnected._