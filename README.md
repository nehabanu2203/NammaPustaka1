# Namma Pustaka (ನಮ್ಮ ಪುಸ್ತಕ) 📚

Namma Pustaka is a comprehensive Library Management System built with modern Android development practices. It aims to streamline the process of managing books, students, and transactions in a library environment, with a special focus on ease of use and AI-powered assistance.

## ✨ Features

- **AI Assistant**: Integrated Gemini AI to help users with book recommendations and library-related queries.
- **Admin Dashboard**: Real-time overview of library statistics, including total books, registered students, overdue items, and pending fines.
- **Student Management**: Register and track student reading progress and borrowing history.
- **Book Inventory**: Manage the library's collection with detailed book information.
- **Barcode/QR Scanner**: Quickly issue or return books using the integrated scanner.
- **Leaderboard**: Encourage reading by ranking students based on total pages read.
- **PDF Reports**: Generate and export detailed library reports in PDF format.
- **UPI Integration**: Collect fines easily through UPI payment links.

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Local Database**: Room DB for offline storage and persistence.
- **Asynchronous Programming**: Kotlin Coroutines and Flow.
- **Navigation**: Compose Navigation.
- **AI Integration**: Google Gemini AI (Generative AI SDK).
- **Backend Services**: Firebase (Authentication and Analytics).
- **Build System**: Kotlin DSL (build.gradle.kts).

## 🚀 Getting Started

### Prerequisites

- Android Studio Koala or newer.
- JDK 17 or higher.
- A Gemini API Key (for the AI Assistant).

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/nehabanu2203/NammaPustaka1.git
   ```
2. Open the project in Android Studio.
3. Add your `google-services.json` to the `app/` directory.
4. Replace the placeholders in `AIAssistantScreen.kt` with your Gemini API Key.
5. Sync the project with Gradle files.
6. Run the app on an emulator or a physical device.

## 📁 Project Structure

- `data/`: Contains Room database entities, DAOs, and database configuration.
- `ui/`: Contains Compose screens and themes.
  - `screens/`: Individual feature screens (Home, Dashboard, AI Assistant, etc.).
  - `theme/`: App color schemes, typography, and shapes.
- `viewmodel/`: LibraryViewModel for managing state and business logic.

## 📸 Screenshots

*(Add your screenshots here)*

## 🔮 Future Improvements

- Cloud synchronization for data across multiple devices.
- Support for multiple library branches.
- Advanced analytics for reading trends.
- Notification system for overdue books.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
