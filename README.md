# Smart Inventory & Warehouse Management System

A production-grade Android application designed for efficient warehouse logistics, featuring bidirectional cloud synchronization, multi-user team collaboration, and role-based access control.

## 🚀 Key Features

### 1. Advanced Inventory Management
*   **SKU-Based Merging:** Automatically detects existing SKUs during entry to update quantities and prices instead of creating duplicates.
*   **Low Stock Alerts:** Customizable thresholds per item. Items below the limit are highlighted in red with warning icons.
*   **Master Catalog (Product Templates):** The system "learns" from your entries, providing smart Autocomplete for names, brands, and prices to speed up workflow.

### 2. Multi-User Team Collaboration
*   **Organization Model:** Supports a "Warehouse Owner" structure where multiple employees work on a single shared inventory.
*   **Team Linking:** Managers can invite workers to join their warehouse team simply by entering their email address.
*   **Real-Time Updates:** Uses Firestore Snapshot Listeners to ensure every team member sees inventory changes the second they happen.

### 3. Role-Based Access Control (RBAC)
*   **WORKER:** Can view stock, add items, and update quantities. Restricted from deleting items, managing the catalog, or viewing reports.
*   **SHIFT LEADER:** Full inventory and catalog management. Restricted from sensitive financial reports and user management.
*   **MANAGER:** Unrestricted access to all features, including team management, role assignments, and professional reporting.

### 4. Professional Reporting & Analytics
*   **PDF Export:** Generates A4-formatted PDF reports for inventory status and transaction history.
*   **CSV/Excel Export:** High-compatibility CSV files with UTF-8 BOM support for perfect display in Excel (including Hebrew support).
*   **Interactive Dashboard:** Visualizes total inventory value and item counts with date-range filtering for transactions.

### 5. Data Integrity & Security
*   **Offline-First Architecture:** Full functionality without internet using Room DB, with background synchronization to Firebase Firestore.
*   **Secure Deep Reset:** A "Nuclear Option" to wipe local and cloud profiles, protected by account password re-authentication.
*   **Stable UI:** Implements `DiffUtil` logic to ensure the list stays perfectly in place during updates (no jumping items).

---

## 🛠 Tech Stack

*   **Language:** Java / Android SDK
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **Database:** Room (Local Persistence v17)
*   **Cloud:** Firebase (Firestore, Authentication, Analytics)
*   **UI Components:** Material Design 3, SwipeRefreshLayout, Google Material DatePicker
*   **Reporting:** Android PdfDocument API, FileProvider for secure sharing

---

## 🏗 Architecture Overview

The app follows the **Single Source of Truth** principle:
1.  **UI** observes **LiveData** from the **ViewModel**.
2.  **ViewModel** communicates with a centralized **Repository**.
3.  **Repository** manages the flow between **Room (SQLite)** for immediate local responsiveness and **Firestore** for global consistency across devices.

---

## 📋 Installation & Setup

1.  Clone the repository.
2.  Add your `google-services.json` from the Firebase Console.
3.  Ensure **Firestore** and **Email/Password Authentication** are enabled.
4.  Build the project (The app uses Room Destructive Migration for easy development sync).

## 🔐 Role Setup (First Run)
To initialize as a Manager:
1.  Register a new account.
2.  The app will automatically detect the first login and create a **MANAGER** profile.
3.  Use the **Manage Team** menu to add staff members by their registered emails.

---
*Developed as a comprehensive solution for modern logistics and inventory tracking.*
