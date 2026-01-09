
# 📂 Academic Analytics (Standalone Linux)

Source code for a local-first academic data management and archival system. This application is optimized for **Debian/Ubuntu** environments and features a bundled JRE and Chromium engine for a zero-dependency, standalone experience.

## 📥 Download Standalone App
[<img src="https://img.shields.io/badge/Download-Linux_DEB-FCC624?style=for-the-badge&logo=linux&logoColor=black" width="200">](https://github.com/raunakranjann/Academic-Analytics_and_Archival-System/releases/download/v1.0.0/academic-pkg.deb)

*Current Version: v1.0.0*

---

## 🚀 Quick Start (Development)
1. **Build JAR:** Run `mvn clean package` to generate the application artifact.
2. **Setup Structure:** Place the generated JAR in `/opt/academic-analytics/`.
3. **Run:** Execute `./start_app.sh` to launch the server and the standalone UI applet.

---

## 🏗 Build & Distribution Recipe

To keep the repository lightweight, binary components are excluded. Follow these steps to reconstruct the standalone installer.

### 1. Component Acquisition
* **Java Runtime:** Download the [Adoptium Temurin Linux x64 JRE](https://adoptium.net/temurin/releases/?os=linux&arch=x64&package=jre) and extract it into the `./jre/` folder.
* **Browser:** Download [Chromium Linux x64](https://download-chromium.appspot.com/?platform=Linux_x64) and extract the binaries to `./browsers/linux/chrome-linux/`.
* **Application:** Compile the Spring Boot project and rename the resulting file to `mainapplication.jar`.

### 2. Linux Package Structure (.deb)
The distribution folder must follow the standard Debian layout:



```text
academic-pkg/
├── DEBIAN/
│   ├── control         <-- Package metadata (Dependencies: libnss3, libgbm1, etc.)
│   ├── postinst        <-- Permission & Sandbox sticky bit configuration
│   └── prerm           <-- Background process cleanup (Java & Chromium)
└── opt/academic-analytics/
    ├── jre/            <-- Bundled Java Runtime
    ├── browsers/       <-- Bundled Chromium Engine
    ├── mainapplication.jar
    └── start_app.sh    <-- Launcher script with port-mutex logic

```

### 3. Build Command

Generate the final installer using the Debian package manager:

```bash
dpkg-deb --build academic-pkg

```

---

## 🛠 Technical Features

* **Standalone Applet:** The UI launches in a dedicated window using the `--app` flag, removing browser tabs and address bars for a native feel.
* **Security Optimization:** Automatic configuration of the `--no-sandbox` flag and `chrome_sandbox` permissions to prevent `SIGTRAP` fatal errors.
* **Local Data Persistence:** SQLite database and application logs are stored in `~/AcademicAnalytics` to ensure full write permissions without requiring root access during runtime.
* **Port Mutex:** Built-in detection ensures only one instance runs on port 2006; secondary launches simply trigger the UI applet.

---

## ⚠️ Troubleshooting

* **UI opens in normal browser:** This occurs if the browser launches before the Spring Boot server is ready. The included `start_app.sh` uses a port-check loop to prevent this.
* **SIGTRAP / Browser Crash:** Ensure the `postinst` script has correctly applied `root` ownership and `4755` permissions to the `chrome_sandbox` binary.
* **Database Read-Only:** Verify the `AcademicAnalytics` folder exists in your home directory with proper user permissions.

---

