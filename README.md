# 🏠 Edge AI Smart Home Voice Assistant

A high-performance, entirely on-device voice-controlled home automation system. This project demonstrates how to implement a complete, low-latency, and privacy-first IoT solution using large language models (LLMs) and speech recognition models deployed directly on mobile hardware.

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Proof--of--Concept-orange.svg)]()

---

## ✨ Key Features

- **🔒 Zero-Cloud Operation:** All voice processing and command generation happen locally on the device
- **🛡️ Ultimate Privacy:** No voice data or commands ever leave your device
- **⚡ Low Latency:** Real-time inference thanks to optimized on-device deployment
- **🗣️ Natural Language Control:** Interpret complex commands using a local LLM (Phi-3-mini)
- **📱 Platform:** Developed for Android using Qualcomm Edge AI tools

---

## 💡 How It Works

This system eliminates the need for cloud APIs by chaining local models and communication protocols:

### The Zero-Cloud Pipeline

```
🎤 Voice Input
    ↓
🎙️ Speech-to-Text (Whisper)
    ↓
🧠 LLM Interpretation (Phi-3-mini)
    ↓
💬 MQTT Command Generation
    ↓
🏡 Home Automation Action
```

1. **Speech-to-Text (STT):** A local implementation of **OpenAI Whisper** converts spoken commands into text in real-time
2. **Natural Language Interpretation:** The text is fed into the **Microsoft Phi-3-mini-4k-instruct LLM** running locally. This model interprets the user's intent and identifies necessary parameters
3. **Command Generation:** The LLM generates a precise, structured **MQTT command** payload
4. **Home Automation Action:** The command is sent via the local MQTT protocol to your home server (e.g., Home Assistant, Mosquitto broker) to control devices

---

## 🛠️ Tech Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| **LLM** | Microsoft Phi-3-mini-4k-instruct | Command interpretation & generation |
| **STT** | OpenAI Whisper | Local voice command transcription |
| **Inference Engine** | Qualcomm SNPE SDK | Converts models for optimal execution on Snapdragon |
| **Development Kit** | Qualcomm QIDK | Comprehensive toolkit for on-device AI development |
| **IoT Protocol** | MQTT | Lightweight, reliable messaging for device communication |
| **Platform** | Android (Java) | Mobile application logic and model interfacing |

---

## 🚀 Getting Started

### Prerequisites

- Android Studio (latest version recommended)
- Android device with **Qualcomm Snapdragon** processor (for optimal SNPE execution)
- **Qualcomm SNPE SDK** and **QIDK** installed
- Local **MQTT Broker** running on your network (e.g., Mosquitto)
- Basic knowledge of Android development and MQTT protocol

### Repository Layout

```
vox/
├── app/                          # Android application module
│   └── src/main/
│       ├── java/com/edgeai/vox/
│       │   ├── audio/            # PCM capture & preprocessing
│       │   ├── stt/              # Whisper Tiny (SNPE)
│       │   ├── llm/              # Phi-3-mini (SNPE)
│       │   ├── mqtt/             # Eclipse Paho MQTT client
│       │   ├── pipeline/         # Voice → STT → LLM → MQTT orchestration
│       │   ├── snpe/             # SNPE runtime Kotlin/JNI façade
│       │   └── ui/               # MainActivity & ViewModel
│       └── cpp/                  # JNI bridge to SNPE native runtime
├── models/
│   ├── whisper/                  # Place whisper_tiny.dlc here (not in repo)
│   └── phi3/                     # Place phi3_mini_4k_instruct.dlc here
├── snpe-sdk/                     # Extract Qualcomm SNPE SDK here (not in repo)
└── scripts/
    ├── convert_models.sh         # Model conversion workflow reference
    └── deploy_models.sh          # adb push helper for on-device models
```

### Build & Run

1. Clone the repository and open it in Android Studio
2. Extract the [Qualcomm SNPE SDK](https://developer.qualcomm.com/software/qualcomm-neural-processing-sdk) into `snpe-sdk/`
3. Convert and place model artifacts (see `scripts/convert_models.sh`)
4. Deploy models to device: `./scripts/deploy_models.sh`
5. Build the `app` module and install on a Snapdragon device
6. Enter your MQTT broker URI, tap **Connect**, then tap the mic button to speak a command

> **Note:** Without the SNPE SDK and converted `.dlc` models, the app compiles and runs using native stub inference that returns demo output — useful for verifying the MQTT pipeline.

---

## 📝 Example Usage

### Voice Command
> *"Turn on the kitchen lights and set the thermostat to 23 degrees"*

### LLM Processing

**System Prompt:**
```
You are a Home Automation Command Generator. Your task is to output a single 
JSON object containing MQTT topics and payloads based on the user's command. 
DO NOT output any other text or explanation.
```

**Generated MQTT Payload:**
```json
{
  "commands": [
    {
      "topic": "home/kitchen/light/set",
      "payload": "ON"
    },
    {
      "topic": "home/thermostat/temperature/set",
      "payload": "23"
    }
  ]
}
```

---

## 🏗️ Architecture

### System Components

```
┌─────────────────────────────────────────┐
│         Android Application             │
├─────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐            │
│  │ Whisper  │  │  Phi-3   │            │
│  │  (STT)   │→ │  (LLM)   │            │
│  └──────────┘  └──────────┘            │
│         ↓            ↓                  │
│  ┌──────────────────────────┐          │
│  │    MQTT Client            │          │
│  └──────────────────────────┘          │
└─────────────┬───────────────────────────┘
              │ MQTT Protocol
              ↓
┌─────────────────────────────────────────┐
│      Home Automation Server             │
│  (Home Assistant / Mosquitto)           │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│      Smart Home Devices                 │
│  (Lights, Thermostats, Switches, etc.)  │
└─────────────────────────────────────────┘
```

## 🎯 Performance Metrics

- **STT Latency:** ~200-500ms (depending on audio length)
- **LLM Inference:** ~1-2 seconds (on Snapdragon 8 Gen 2)
- **End-to-End Response:** ~2-3 seconds from voice input to device action
- **Memory Usage:** ~800MB RAM during active inference
- **Battery Impact:** Minimal when idle, moderate during active use

---

## 👥 Project Team

This project was a collaborative effort by:

- **Navey Puri** 
- **Shreyash Lohare**
- **Arpit Mahtele**

---

## 🙏 Acknowledgments

- **Qualcomm** for the SNPE SDK and QIDK development tools
- **OpenAI** for the Whisper speech recognition model
- **Microsoft** for the Phi-3-mini language model
- The open-source home automation community

---

## 📧 Contact

For questions or feedback, please open an issue or reach out to the project maintainers.

---

## 🔮 Future Roadmap

- [ ] Support for additional languages
- [ ] Integration with more home automation platforms
- [ ] Voice feedback system using local TTS
- [ ] Multi-user voice recognition
- [ ] Enhanced natural language understanding
- [ ] iOS platform support

---

Home Team

</div>
