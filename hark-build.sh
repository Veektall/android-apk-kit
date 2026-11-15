#!/usr/bin/env bash
# One-command native APK build inside an AI workspace (Hark, ChatGPT, any Linux box).
# Usage: bash hark-build.sh [app_dir]     (default: app/)
# Gets Java 17 if javac is missing (once, ~190 MB), picks x86-64 or arm64 tools, then runs build.sh.
set -euo pipefail
KIT="$(cd "$(dirname "$0")" && pwd)"
if ! command -v javac >/dev/null 2>&1; then
  if [ ! -x "$KIT/.jdk/bin/javac" ]; then
    A=x64; [ "$(uname -m)" = "aarch64" ] && A=aarch64
    echo "Getting Java 17 ($A)..."
    curl -sfL -o "$KIT/jdk.tgz" "https://api.adoptium.net/v3/binary/latest/17/ga/linux/$A/jdk/hotspot/normal/eclipse"
    mkdir -p "$KIT/.jdk"; tar xzf "$KIT/jdk.tgz" -C "$KIT/.jdk" --strip-components=1; rm -f "$KIT/jdk.tgz"
  fi
  export PATH="$KIT/.jdk/bin:$PATH"
fi
chmod +x "$KIT"/tools/aapt2 "$KIT"/tools/zipalign "$KIT"/tools-arm64/aapt2 "$KIT"/tools-arm64/zipalign 2>/dev/null || true
exec bash "$KIT/build.sh" "${1:-$KIT/app}"
