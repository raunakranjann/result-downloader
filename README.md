### 📥 Download & Install

[<img src="https://img.shields.io/badge/Download-Windows_Installer-0078D4?style=for-the-badge&logo=windows&logoColor=white" width="250">](https://github.com/raunakranjann/Academic-Analytics_and_Archival-System/releases/download/v1.0.0/AcademicAnalytics.exe)

[<img src="https://img.shields.io/badge/Download-Linux_DEB-FCC624?style=for-the-badge&logo=linux&logoColor=black" width="250">](https://github.com/raunakranjann/Academic-Analytics_and_Archival-System/releases/download/v1.0.0/academic-pkg.deb)

*Current Version: v1.0.0*














# 🏗 Academic Analytics: Build & Distribution Recipe

This document provides instructions for reconstructing the build environment and generating standalone installers for Windows and Linux. 

> **Note:** Binary components (JRE and Browser) are excluded from the repository to keep it lightweight. Follow the steps below to re-bundle them.

---

## 📦 1. Component Acquisition

Before building, download and place these components into your distribution folder.

### A. Java Runtime Environment (JRE)
The application requires a Windows-compatible JRE to run inside Wine or on a native Windows machine.
* **Version:** Java 17 (LTS)
* **Source:** [Adoptium Temurin Releases](https://adoptium.net/temurin/releases/?os=windows&arch=x64&package=jre)
* **Action:** Extract the `.zip` and rename the folder to `jre` inside your build directory.

### B. Chromium Browser Binaries
To ensure the web-view works without requiring the user to have a specific browser installed.
* **Source:** [Chromium Win64 Snapshots](https://commondatastorage.googleapis.com/chromium-browser-snapshots/index.html?prefix=Win_x64/)
* **Action:** Place the binaries in the `browsers/` folder.

### C. The Application JAR
* **Command:** `mvn clean package`
* **Target:** Rename the resulting JAR to `mainapplication.jar` and place it in the root build folder.

---

## 🛠 2. Windows Build Process (.exe)

We use **Inno Setup** running via Wine to compile the Windows installer.

1.  **Verify Structure:**
    Ensure your directory looks like this:
    ```text
    AcademicAnalytics-Win/
    ├── jre/                 <-- (Extracted Windows JRE)
    ├── browsers/            <-- (Chromium Binaries)
    ├── mainapplication.jar  <-- (Your Spring Boot App)
    ├── start_app.bat        <-- (Launcher Script)
    └── academic_installer.iss
    ```

2.  **Compile via Wine:**
    Run the following command in your terminal:
    ```bash
    wine "C:\Program Files (x86)\Inno Setup 6\ISCC.exe" academic_installer.iss
    ```

3.  **Result:** The `AcademicAnalytics_Setup.exe` will be generated in the `/` folder.

---

## 🐧 3. Linux Build Process (.deb)

To create a Debian package for Ubuntu/Mint users:

1.  **Prepare the Layout:**
    ```bash
    mkdir -p academic-pkg/opt/academic-analytics
    cp mainapplication.jar academic-pkg/opt/academic-analytics/
    # (Copy Linux JRE here as well)
    ```

2.  **Build:**
    ```bash
    dpkg-deb --build academic-pkg
    ```

---

## ⚠️ 4. Troubleshooting
* **Port Conflict:** If the app fails to start during testing, port 2006 is likely locked.
    * *Fix:* `sudo kill -9 $(sudo lsof -t -i:2006)`
* **Bad EXE Format:** Ensure you are using a **64-bit JRE** with a **64-bit Wine prefix**.
    * *Check:* `file jre/bin/java.exe` should return `PE32+ executable (console) x86-64`.

---

## 🔗 Useful Links
* [Inno Setup Documentation](https://jrsoftware.org/ishelp/)
* [Spring Boot Deployment Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)
