

```
# 📂 Academic Analytics (Source)

Source code for the Linux standalone academic archival system.

---

## 🏗 Build & Setup

### 1. Acquire Binaries
Before building the installer, you must place these external components into your distribution folder:

* **JRE 17 (Linux x64):** Download from [Adoptium](https://adoptium.net/temurin/releases/?os=linux&arch=x64&package=jre) and extract to `./jre/`.
* **Chromium (Linux x64):** Download from [Chromium Linux](https://download-chromium.appspot.com/?platform=Linux_x64) and extract to `./browsers/linux/chrome-linux/`.

### 2. Build the Application
Compile the project and prepare the JAR file:
```bash
# Generate the build artifact
mvn clean package

# Rename and move to the root distribution folder
cp target/*.jar ./mainapplication.jar

```

### 3. Generate Standalone Package (.deb)

Ensure your `DEBIAN` scripts and `/opt` folder structure are ready, then run:

```bash
dpkg-deb --build academic-pkg

```

---

## 🚀 Quick Launch

To run the source build directly:

```bash
./start_app.sh

```

