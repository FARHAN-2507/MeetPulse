# 🎧 MeetPulse

**MeetPulse** is a privacy-first desktop application that analyzes the *energy and quality of meetings* using real-time audio signals — without recording or transcribing any speech.

---

## 🚀 What It Does

MeetPulse runs silently in the background and processes microphone input to measure:

* 🔊 Audio energy (RMS)
* 🔇 Silence gaps (e.g., dead moments)
* 📈 Energy trends over time

It then uses this data to evaluate how engaging or active a meeting was.

---

## 🧠 Key Idea

Unlike tools like Otter or Fireflies (which focus on *what was said*), MeetPulse focuses on:

> 👉 **How the meeting felt**

No speech is recorded. No words are stored. Only signal-level analysis is performed.

---

## ✨ Features (Current)

* 🎤 Real-time microphone capture
* 📊 RMS energy calculation (normalized 0–1)
* 🔧 Noise filtering & signal smoothing
* 🧠 Adaptive noise floor calibration
* 🔇 Intelligent silence detection (relative to environment)

---

## 🛠️ Tech Stack

* **Language:** Java 21
* **Audio Processing:** `javax.sound.sampled`
* **Build Tool:** Maven
* **IDE:** IntelliJ IDEA

---

## ⚙️ How It Works

### 1. Audio Capture

* Captures raw audio from microphone using `TargetDataLine`

### 2. Signal Processing

* Converts byte stream → 16-bit samples
* Computes RMS (Root Mean Square) energy
* Applies smoothing + noise filtering

### 3. Noise Calibration

* Learns background noise dynamically (first few seconds)
* Uses this to define "relative silence"

### 4. Silence Detection

* Detects low-energy periods (>3 sec)
* Ignores background noise and small fluctuations

---

## 📊 Example Output

```
Energy: 0.18
Energy: 0.32
Energy: 0.12
🔇 Silence detected (1)
```

---

## 🔒 Privacy First

* ❌ No audio recording
* ❌ No transcription
* ❌ No cloud / API usage
* ✅ 100% local processing

---

## 🧱 Project Structure

```
com.meetpulse
├── Main.java
├── audio/
│   └── AudioCaptureService.java
├── processing/
│   ├── EnergyCalculator.java
│   └── SilenceDetector.java
├── model/
│   ├── EnergyFrame.java
│   └── MeetingStats.java
└── service/
    └── MeetingAnalyzer.java
```

---

## 🧪 How to Run

1. Clone the repo
2. Open in IntelliJ IDEA
3. Ensure Java 21 is configured
4. Run `Main.java`

---

## ⚠️ Notes

* Stay silent during the first few seconds → allows proper noise calibration
* Works best in a relatively stable audio environment

---

## 🧭 Roadmap

### 🔜 Upcoming Features

* 📈 Energy timeline graph
* 🧮 Meeting Health Score (0–100)
* 📁 Export report (JSON / CSV)
* 🖥️ JavaFX UI dashboard
* 🧑‍🤝‍🧑 Speaker activity estimation

---

## 💡 Why This Project Matters

MeetPulse explores a unique problem:

> Measuring meeting quality without understanding content

It focuses on **signal behavior instead of semantics**, making it:

* privacy-friendly
* lightweight
* scalable

---

## 👨‍💻 Author

Built as a hands-on system design + audio processing project.

---

## ⭐ If you like this idea

Give it a star and follow the journey 🚀
