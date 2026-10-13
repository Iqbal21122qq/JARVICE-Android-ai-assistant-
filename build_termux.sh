#!/usr/bin/env bash
# ==============================================================================
# J.A.R.V.I.S. Core - Automated On-Device Build Script for Termux / PRoot
# ==============================================================================
set -e

echo "=== [1/5] Updating Packages & Installing Toolchain ==="
if [ -n "$TERMUX_VERSION" ]; then
    echo "Running inside native Termux..."
    pkg update -y
    pkg install -y openjdk-17 gradle git wget unzip aapt
    ARCH="arm64-v8a"
else
    echo "Running inside PRoot / Linux environment..."
    apt update -y
    apt install -y openjdk-17-jdk-headless git wget unzip curl aapt
    ARCH=$(dpkg --print-architecture)
fi

echo "=== [2/5] Configuring Android SDK Environment ==="
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

mkdir -p "$ANDROID_HOME/cmdline-tools"

# Download Android SDK Command-Line Tools if not present
if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
    echo "Downloading Android Command-Line Tools..."
    CMDLINE_ZIP="commandlinetools-linux-11076708_latest.zip"
    wget -q --show-progress "https://dl.google.com/android/repository/$CMDLINE_ZIP" -O /tmp/cmdline.zip
    unzip -q /tmp/cmdline.zip -d /tmp/cmdline
    mv /tmp/cmdline/cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
    rm -rf /tmp/cmdline*
fi

# Accept Licenses and Install Required SDK Components
echo "Accepting licenses and fetching platforms..."
yes | sdkmanager --licenses > /dev/null 2>&1 || true
sdkmanager "platforms;android-34" "build-tools;34.0.0" > /dev/null 2>&1 || true

echo "=== [3/5] Setting up Project Configuration ==="
PROJECT_DIR="$(pwd)"

if [ ! -f "$PROJECT_DIR/.env" ]; then
    if [ -f "$PROJECT_DIR/.env.example" ]; then
        cp "$PROJECT_DIR/.env.example" "$PROJECT_DIR/.env"
    else
        echo "GEMINI_API_KEY=MY_GEMINI_API_KEY" > "$PROJECT_DIR/.env"
    fi
fi

echo "=== [4/5] Compiling J.A.R.V.I.S. Debug APK ==="
gradle assembleDebug \
    -Dorg.gradle.jvmargs="-Xmx1536m -XX:MaxMetaspaceSize=512m" \
    --no-daemon

echo "=== [5/5] Build Complete! ==="
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_PATH" ]; then
    echo "SUCCESS: APK generated at:"
    echo "$APK_PATH"
    echo ""
    echo "To install directly on this rooted device, run:"
    echo "su -c 'pm install -r $APK_PATH'"
else
    echo "Check output logs in app/build/outputs/."
fi
