# 📦 Your Inventory - Professional Android Management App

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Java](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.oracle.com/java/)
[![Room](https://img.shields.io/badge/Database-Room-blue.svg)](https://developer.android.com/training/data-storage/room)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-yellow.svg)](https://firebase.google.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A robust and modern Android application designed for high-performance inventory and warehouse management. This app follows an **offline-first** philosophy, using local persistence with Room DB and secure cloud synchronization for authentication via Firebase.

---

## 🏛 Architecture: MVVM

The project is built using the **MVVM (Model-View-ViewModel)** architectural pattern, ensuring a clean separation of concerns and high maintainability.

- **Model**: Data layer representing Room entities (`Item`, `User`, `Transaction`).
- **View**: UI layer (`MainActivity`, `DashboardActivity`) observing `LiveData` for real-time updates.
- **ViewModel**: Acts as a bridge, maintaining state and handling logic.
- **Repository**: Single source of truth coordinating between different DAOs and background threads.

---

## 🚀 Key Features

### 📊 Advanced Dashboard & Analytics
- **Live Inventory Summary**: Real-time calculation of total stock and financial value (optimized with SQL `SUM` and `COUNT`).
- **Activity Log**: Comprehensive tracking of every `ADD`, `DELETE`, and `UPDATE` action.
- **Smart Filtering**:
    - 🔍 **Free Text Search**: Instantly find specific items within the history.
    - 📅 **Date Range Picker**: Filter logs using a modern calendar interface.
- **Maintenance**: Auto-cleanup of logs older than 30 days using `java.time.Instant` for precision.

### 📦 Modern Inventory Control
- **Intuitive UI**: Material 3 design with `CardView` layouts and smooth animations.
- **One-Tap Actions**: Quick `+/-` buttons for instant quantity adjustments.
- **Safety First**: Deletion guarded by confirmation dialogs to prevent data loss.

### 🔐 Security & Identity
- **Firebase Auth**: Secure cloud-based authentication.
- **Strong Validation**: Strict password requirements for user safety.
- **User Isolation**: Secure data partitioning ensures users only access their own private inventory.

---

## 🛠 Tech Stack

| Category | Technology |
| --- | --- |
| **Language** | Java 11 |
| **Database** | Room Persistence Library (SQLite) |
| **Backend** | Firebase Auth & Analytics |
| **UI Framework** | Material Components, RecyclerView |
| **Reactive UI** | LiveData & ViewModel (Jetpack) |
| **Concurrency** | ExecutorService |

---

## 📂 Project Structure

```text
com.example.myapplication/
├── model/           # Data Entities & Room DAOs
├── repository/      # Data handling & logic
├── view/            # Activities, Adapters & UI
└── viewmodel/       # UI State management
```

---

## ⚙️ Setup Instructions

1. **Clone the repository**:
   ```bash
   git clone https://github.com/AlonLyubarov/Inventory_management_android_app.git
   ```
2. **Firebase Configuration**:
   - Register your app in the [Firebase Console](https://console.firebase.google.com/).
   - Place your `google-services.json` in the `app/` folder.
3. **Build & Run**:
   - Open in Android Studio Giraffe or newer.
   - Minimum SDK: 26.

---

## 📄 License
Licensed under the **MIT License**. Feel free to use and adapt for your needs!
