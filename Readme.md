# 📚 Namma Pustaka (ನಮ್ಮ ಪುಸ್ತಕ) – Smart Library Management App

## 📖 Project Overview
Namma Pustaka is an Android-based Smart Library Management System developed to help schools and colleges manage books digitally. The application simplifies library operations such as book issuing, returning, searching, and recommendations using modern technologies like QR codes and AI-powered suggestions.

The project is specially designed for rural schools and small educational institutions where manual library management is still commonly used.

---

# ❓ Problem Statement
Many schools and libraries still maintain book records manually, which creates problems such as:
- Difficulty in tracking books
- Time-consuming book issue/return process
- Loss of records
- Poor book recommendation systems
- Lack of digital management

Namma Pustaka solves these issues using a smart Android application.

---

# ✨ Features

## 📷 QR Code Based Book Management
- Scan QR codes for issuing and returning books
- Fast and accurate book tracking

## 🤖 AI Book Recommendation
- Integrated **Google Gemini AI** to suggest books based on user interests.
- Smart library assistant for queries.

## 🔍 Book Search & Inventory
- Search books quickly using title or category.
- Real-time inventory tracking.

## 👤 Student & Admin Management
- Separate access for students and administrators.
- Register and track student reading progress.

## 📊 Admin Dashboard
- Visual overview of total books, students, overdue items, and pending fines.
- Export library reports in **PDF format**.
- **UPI Integration** for easy fine collection.

## 📱 Modern UI/UX
- Built with **Jetpack Compose** for a smooth and responsive experience.
- Dark mode support and intuitive navigation.

---

# 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Database**: Room DB (Local Persistence)
- **Backend**: Firebase (Auth & Analytics)
- **AI**: Google Gemini AI (Generative AI SDK)
- **Build System**: Kotlin DSL (build.gradle.kts)
- **Other**: QR Code Scanner, PDF Generation, UPI Integration

---

# 🚀 Getting Started

### Prerequisites
- Android Studio Koala+
- JDK 17
- Gemini API Key

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/nehabanu2203/NammaPustaka1.git
   ```
2. Open in Android Studio.
3. Add your `google-services.json` to the `app/` folder.
4. Replace the Gemini API Key in `AIAssistantScreen.kt`.
5. Sync Gradle and Run.

---

# 📂 Project Structure

```text
NammaPustaka1/
│
├── app/                # Main application module
│   ├── src/main/java/  # Source code (Kotlin)
│   └── src/main/res/   # Resources (Images, Strings, etc.)
├── gradle/             # Gradle wrapper and version catalog
├── README.md           # Project documentation
├── build.gradle.kts    # Project-level build configuration
└── settings.gradle.kts # Project settings
```

---

## 📸 Screenshots

| Home Screen | Login Screen | QR Scanner |
|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/b7a816c0-9036-4254-8e35-bac7f7932e0a" width="250"/> | <img src="https://github.com/user-attachments/assets/f2038f4c-21cb-4eb4-aafb-0024d53e62f2" width="250"/> | <img src="https://github.com/user-attachments/assets/1cc50463-700b-44eb-9dfa-9ca57e5e8733" width="250"/> |

| Dashboard | Lending History | Student Registration |
|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/12d493d3-1a8d-4a04-b4ec-639dbe01fc26" width="250"/> | <img src="https://github.com/user-attachments/assets/2526eddc-34f7-4ad1-b048-ff041d22c479" width="250"/> | <img src="https://github.com/user-attachments/assets/84880049-2ba3-4618-88ed-c010eb79a465" width="250"/> |

| Leaderboard |
|:---:|
| <img src="https://github.com/user-attachments/assets/d0b5fb70-5207-418f-b7a3-dedf2350f2c0" width="250"/> |

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
