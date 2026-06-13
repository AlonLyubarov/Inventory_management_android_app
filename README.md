# Smart Inventory & Warehouse Management System

A production-grade Android application designed for efficient warehouse logistics, featuring bidirectional cloud synchronization, multi-user team collaboration, and role-based access control.

## 🚀 Key Features

### 1. Advanced Inventory Management
*   **SKU-Based Merging:** Automatically detects existing SKUs during entry to update quantities and prices instead of creating duplicates.
*   **Low Stock Alerts:** Customizable thresholds per item. Items below the limit are highlighted in red with warning icons and adaptive UI coloring.
*   **Master Catalog (Product Templates):** The system "learns" from your entries, providing smart Autocomplete for names, brands, and prices to speed up workflow.

### 2. Multi-User Team Collaboration
*   **Organization Model:** Supports a "Warehouse Owner" structure where multiple employees work on a single shared inventory context.
*   **Team Linking:** Managers can invite workers to join their warehouse team simply by entering their registered email address.
*   **Real-Time Updates:** Uses Firestore Snapshot Listeners combined with **Delta-Sync** logic to ensure every team member sees inventory changes instantly without refreshing.

### 3. Role-Based Access Control (RBAC)
*   **WORKER:** Can view stock, add items, and update quantities. Restricted from deleting items, managing the catalog, or viewing reports.
*   **SHIFT LEADER:** Full inventory and catalog management. Restricted from sensitive financial reports and user management.
*   **MANAGER:** Unrestricted access to all features, including team management, role assignments, and professional reporting.

### 4. Professional Reporting & Analytics
*   **PDF Export:** Generates A4-formatted PDF reports for inventory status and transaction history.
*   **CSV/Excel Export:** High-compatibility CSV files with UTF-8 BOM support for perfect display in Excel (including full Hebrew RTL support).
*   **Interactive Dashboard:** Visualizes total inventory value and item counts with real-time reactive stats and date-range filtering.

### 5. Data Integrity & Security
*   **Reactive MVVM Architecture:** Pure implementation where the UI observes local Room DB (Single Source of Truth), ensuring 100% offline functionality with seamless cloud background sync.
*   **High-Performance Sync:** Implements structural document change listeners to process only updated data, eliminating UI lag and "jumping" list items.
*   **Secure Deep Reset:** A "Nuclear Option" to completely purge local Room tables and remove cloud profiles, protected by account re-authentication (Password validation or Jetpack Credential Manager OAuth handshakes).

---

## 🛠 Tech Stack

*   **Language:** Java / Android SDK
*   **Architecture:** Reactive MVVM (Model-View-ViewModel)
*   **Database:** Room (Local SQLite Persistence Framework)
*   **Cloud & Auth:** Firebase (Firestore, Cloud Analytics), Jetpack Credential Manager API (Google Identity integration)
*   **UI Components:** Material Design 3 (FilledBox Inputs, CardViews), SwipeRefreshLayout, Material DatePicker
*   **Reporting:** Android PdfDocument API, FileProvider for secure sharing

---

## 🏗 Architecture Overview

The app follows the **Single Source of Truth** principle:
1.  **UI** observes reactive **LiveData** streams from the **ViewModel**.
2.  **ViewModel** triggers logic through a centralized **Repository** layer.
3.  **Repository** manages the data flow between **Room** for immediate local responsiveness and **Firestore** for global consistency across devices.

---

## 📋 Installation & Setup

1.  Clone the repository.
2.  Add your `google-services.json` configuration bundle from the Firebase Console.
3.  Ensure **Firestore** and **Email/Password / Google** Authentication providers are enabled.
4.  Build the project (The app uses Room Destructive Migration for easy development sync).

## 🔐 Role Setup (First Run)
To initialize as a Manager:
1.  Register a new account inside the application.
2.  The system will automatically detect the initial setup state and provision a **MANAGER** user profile context.
3.  Use the **Manage Team** menu to add staff members by their registered emails.

---
*Developed as a comprehensive, scalable solution for modern logistics and enterprise inventory tracking.*
