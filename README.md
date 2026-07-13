# 🚗 Garage

**Garage** is a modern, offline-first vehicle maintenance tracking app built for Android. It helps you manage your personal fleet by tracking vehicle service history, maintenance schedules, and calculating tasks that are due soon.

This project is a ground-up rebuild of an older app, leveraging the latest Android development practices and technologies: **Kotlin**, **Jetpack Compose**, **Hilt**, **Room**, and **Firebase**. 

---

## 📱 Features & Status (Phase 1)

This project is currently in **Phase 1**—a functioning prototype intended to demonstrate a scalable, offline-first architecture. 

### 🔧 What's Built & Working
* **Authentication**: Seamless Sign in / Register / Sign out powered by Firebase Auth.
* **Home Dashboard**: View your vehicles, tasks due soon, and recent activity.
* **Offline-First Sync**: Room acts as the single source of truth for the UI, with background synchronization to Firestore via a custom `SyncCoordinator`.
* **AI Recommendations**: Leverages Firebase GenAI to smartly suggest maintenance tasks based on your vehicle's make and model.
* **Navigation**: Single-activity architecture using Navigation-Compose with a Bottom Navigation Shell (Home / Vehicles / Checkups / Profile).
* **Background Tasks**: Daily WorkManager checks for due maintenance tasks to trigger notifications.
* **Vehicle Detail**: Archive/Delete vehicles, track odometer, and view AI-suggested maintenance tasks.

### 🚧 Coming Soon
* **Add Vehicle Screen**: Adding vehicles currently requires manual Firestore document insertion (or UI completion in Phase 2).
* **Log Service Record**: Natural language "describe it" parsing logic exists (`ParseServiceEntryUseCase.kt`), waiting to be wired to the UI.
* **Maintenance & Profile Tabs**: Profile tab UI is now integrated with the app version display, while full maintenance features remain for future development.

### 🗺️ Suggested Roadmap
1. ~~Login/Register + Home~~ ✅ *(Current)*
2. Vehicles tab + Add Vehicle screen
3. Log Record screen (with natural language parser) + record detail
4. Maintenance tab + Add Task screen
5. Real Profile screen, Polish & UI Tests

---

## 🏗️ Architecture & Tech Stack

This project strictly adheres to modern Android best practices:

* **UI Framework**: **Jetpack Compose** + **Material 3** for fully declarative and reactive UI elements.
* **State Management**: `StateFlow` and Hilt `ViewModel`s—no `LiveData`.
* **Database & Offline-First Strategy**:
  * **Room** is the single source of truth that UI components observe. 
  * **Firestore** only talks to the repository layer. 
  * Each entity features an `isSynced` and `isDeleted` flag, enabling instant offline writes that quietly sync to the cloud when online (`SyncCoordinator` handles this automatically per user).
* **Dependency Injection**: **Hilt** (`di/` modules available for Room, Firebase, and process-lifetime `CoroutineScope`).
* **Domain Layer**: Clean Kotlin models fully decoupled from both Room entities and Firestore documents. Contains unit-testable use-cases like `TaskUrgencyCalculator` and `ParseServiceEntryUseCase`.
* **Background Processing**: **WorkManager** integrated with Hilt-Work (`notifications/`) for checking scheduled checkups.

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Koala (or newer)
* A Firebase Project

### Setup Instructions

1. **Clone the Repository** and open the project root in Android Studio.
2. **Configure Firebase**:
   * Create a Firebase project.
   * Add an Android app with the package name `com.example.garage` *(change this ID before publishing)*.
   * Download `google-services.json`.
3. **Add Google Services**: 
   * Place your `google-services.json` inside the `app/` directory (replace `app/google-services.json.example`).
   * *Note: `google-services.json` is `.gitignore`d.*
4. **Enable Firebase Services**:
   * **Authentication**: Enable **Email/Password** sign-in.
   * **Firestore Database**: Create a database. Start in "Test Mode" for local development.
   * **Firebase GenAI**: Enable for AI-driven task suggestions.
5. **Build & Run**:
   * Sync Gradle and run the app. Let Android Studio's "Sync Project" handle wrapper generation.

---

## 🔒 Recommended Firestore Security Rules
Before sharing your app or moving to production, tighten your Firestore rules so users can only access their own data:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```
