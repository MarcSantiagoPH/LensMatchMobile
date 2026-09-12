# Solutions: Scanning Other Users Without Saving to Account History

## Problem Overview
Currently, an account belongs to a single user. When someone else (such as a friend, family member, or optical store customer) scans their face on the device, the app permanently overwrites the account owner’s face shape, photo, and scan history. 

The goal is to allow scanning other people’s faces for real-time recommendations and AR try-on, **without saving or polluting the account owner's personal history**.

---

## Solution 1: "Quick Guest Scan" Mode (Lightweight & Temporary)

### The Concept
A simple toggle on the Camera or Home screen that switches between **"Myself"** and **"Other Person / Guest"**.

### How It Works:
1. **Target Selection**:
   * **👤 Myself (Account Owner)**: Default mode. The scan result is saved to the owner's profile and permanently added to the Scan History.
   * **👥 Other Person (Guest)**: Temporary mode. The scan runs in memory only.
2. **Analysis & Recommendations**:
   * The other person immediately sees their detected face shape, styling advice, and can test glasses in 3D AR.
   * A clear banner indicates: *"Guest Scan — Not saved to your history"*.
3. **Data Safety**:
   * The other person's scan is **never written to disk or scan history**.
   * Once the results tab is closed or the app restarts, the account owner’s saved scan and profile remain 100% untouched.

### Best For:
Casual users, family members, or friends trying out the app on the owner's phone.

---

## Solution 2: "Multi-Profile / Client Manager" System

### The Concept
A single account acts as an umbrella that manages separate profiles (e.g., *"Owner (Me)"*, *"Sarah"*, *"David"*).

### How It Works:
1. **Profile Switcher**:
   * A profile selector chip at the top of the app shows who is currently active (e.g., `[ 👤 John (Me) ▾ ]`).
   * Tapping it allows choosing an existing profile or selecting `[ + Scan as Guest (No History) ]`.
2. **Strict History Segregation**:
   * Scans made under "Guest" are discarded after viewing.
   * Only scans explicitly assigned to the "Account Owner" appear in the owner's personal Scan History tab.
   * Other people's scans either disappear (guest) or stay confined to their own separate profile cards.

### Best For:
Shared family devices or optical boutique staff who want to organize distinct profiles without cluttering the primary account.

---

## Solution 3: "Optical Store / Consultation" Mode

### The Concept
Treats the logged-in account as a **Store Specialist / Consultant**, separating the app into "Staff Tools" and "Customer Sessions".

### How It Works:
1. **Consultation Sessions**:
   * The staff member taps *"Start Customer Scan"*.
   * The customer’s face is scanned, analyzed, and eyeglasses are previewed in AR.
2. **No Personal Account Overwrite**:
   * Because it is a customer session, it does not touch the staff member's personal facial profile.
   * At the end of the session, the customer can either save frames to a temporary order or tap *"End Session"*, which completely clears the scan without adding it to the owner's personal history.

### Best For:
Commercial retail stores, optical clinics, and pop-up eyewear kiosks.

---

## Summary Comparison

| Solution | How Other Scans Are Handled | Impact on Owner's History | User Effort |
| :--- | :--- | :--- | :--- |
| **1. Quick Guest Scan** | Discarded automatically after the session ends | **Zero impact** (Owner data preserved) | Lowest (1 tap toggle) |
| **2. Multi-Profile** | Kept in separate profiles or discarded if Guest | **Zero impact** (Profiles strictly isolated) | Medium (profile switcher) |
| **3. Consultation Mode** | Cleared upon ending the customer session | **Zero impact** (Owner data untouched) | Medium (session workflow) |
