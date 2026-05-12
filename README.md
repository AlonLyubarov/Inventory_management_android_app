Inventory Management App 📦
A robust Android application for managing warehouse or personal inventory, featuring real-time database synchronization and a clean MVVM architecture.

🚀 Features
User Authentication: Secure Login and Registration powered by Firebase Auth.

Inventory Management: Add, view, and delete items with specific attributes (Name, Quantity, Price).

Real-time Search: Dynamic filtering of items using a SearchView in the Toolbar.

Local Persistence: Data is stored locally using Room Database, ensuring offline availability.

Modern UI: Built with RecyclerView, CardView, and interactive "Swipe-to-Delete" functionality.

🛠 Tech Stack
Language: Java

Architecture: MVVM (Model-View-ViewModel)

Database: Room (SQLite)

Backend: Firebase Authentication

UI Components: LiveData, ViewModel, ConstraintLayout, Material Design.

📂 Project Structure
model/: Contains the Item entity and Room Database configuration.

view/: Activities (MainActivity, LoginActivity, RegisterActivity) and the ItemAdapter.

viewmodel/: ItemViewModel for managing UI-related data.

repository/: Handles data operations between the ViewModel and the Room DAO.

⚙️ Setup
Clone the repository.

Connect the project to your Firebase Console.

Download the google-services.json and place it in the app/ folder.

Build and Run on an Android Emulator or physical device.
