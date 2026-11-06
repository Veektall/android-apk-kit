#!/usr/bin/env bash
# Native Android APK builder (Java + Android SDK). No Gradle, no internet.
# Usage: bash build.sh [app_dir]   (default: ./app next to this script)
# app_dir holds: AndroidManifest.xml, java/**.java, optional res/ and assets/, app.env (APP_NAME=...)
# Output: out/<APP_NAME>.apk and out/app.apk, signed with keys/debug.keystore (same key every build,
# so a new version installs over the old one).
# Needs: Linux x86-64, Java 17+ (javac), python3.
set -euo pipefail
KIT="$(cd "$(dirname "$0")" && pwd)"
APP="$(cd "${1:-$KIT/app}" && pwd)"
APP_NAME=App; [ -f "$APP/app.env" ] && . "$APP/app.env"
T="$KIT/tools"; AJ="$KIT/android.jar"
export LD_LIBRARY_PATH="$T/lib64:${LD_LIBRARY_PATH:-}"
chmod +x "$T/aapt2" "$T/zipalign"
B="$KIT/build"; rm -rf "$B"; mkdir -p "$B/classes" "$B/dex" "$B/gen" "$B/res"
echo "[1/6] resources"
RES_ARGS=()
if [ -d "$APP/res" ]; then "$T/aapt2" compile --dir "$APP/res" -o "$B/res/res.zip"; RES_ARGS=("$B/res/res.zip"); fi
ASSET_ARGS=(); if [ -d "$APP/assets" ]; then ASSET_ARGS=(-A "$APP/assets"); fi
"$T/aapt2" link -o "$B/base.apk" -I "$AJ" --manifest "$APP/AndroidManifest.xml" --java "$B/gen" --auto-add-overlay ${ASSET_ARGS[@]+"${ASSET_ARGS[@]}"} ${RES_ARGS[@]+"${RES_ARGS[@]}"}
echo "[2/6] compile java"
if ! javac -nowarn -encoding UTF-8 -source 8 -target 8 -bootclasspath "$AJ" -classpath "$AJ" -d "$B/classes" $(find "$APP/java" "$B/gen" -name '*.java') 2> "$B/javac.log"; then
  cat "$B/javac.log"; echo "ERROR: Java compile failed (see errors above)"; exit 1
fi
echo "[3/6] dex"
java -cp "$T/lib/d8.jar" com.android.tools.r8.D8 --lib "$AJ" --min-api 21 --release --output "$B/dex" $(find "$B/classes" -name '*.class')
echo "[4/6] package"
python3 - "$B/base.apk" "$B/dex/classes.dex" <<'PY'
import sys, zipfile
z = zipfile.ZipFile(sys.argv[1], 'a', zipfile.ZIP_DEFLATED); z.write(sys.argv[2], 'classes.dex'); z.close()
PY
echo "[5/6] align"
"$T/zipalign" -f -p 4 "$B/base.apk" "$B/aligned.apk"
echo "[6/6] sign"
mkdir -p "$KIT/out"
java -jar "$T/lib/apksigner.jar" sign --ks "$KIT/keys/debug.keystore" --ks-pass pass:android --key-pass pass:android --ks-key-alias key --out "$KIT/out/$APP_NAME.apk" "$B/aligned.apk"
java -jar "$T/lib/apksigner.jar" verify "$KIT/out/$APP_NAME.apk"
cp "$KIT/out/$APP_NAME.apk" "$KIT/out/app.apk"
ls -la "$KIT/out"
echo "DONE: out/$APP_NAME.apk"
