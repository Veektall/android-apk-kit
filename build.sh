#!/usr/bin/env bash
# Usage: bash build.sh <path/to/game.html> [AppName] [package.id]
# Produces out/<AppName>.apk (signed, installable) and out/<AppName>-source.zip
set -euo pipefail
KIT="$(cd "$(dirname "$0")" && pwd)"
HTML="${1:?give the path to your single-file HTML game}"
NAME="${2:-CandyPop}"
PKG="${3:-com.pdent.candypop}"
LABEL="${LABEL:-$NAME}"
T="$KIT/tools"; AJ="$KIT/android.jar"
export LD_LIBRARY_PATH="$T/lib64:${LD_LIBRARY_PATH:-}"
chmod +x "$T/aapt2" "$T/zipalign"
W="$KIT/work/$NAME"; rm -rf "$W"; mkdir -p "$W"; cp -r "$KIT/template/." "$W/"
cp "$HTML" "$W/assets/index.html"
# rename package + label
PKGDIR="$W/src/$(echo "$PKG" | tr . /)"
if [ "$PKG" != "com.pdent.candypop" ]; then
  mkdir -p "$PKGDIR"; mv "$W/src/com/pdent/candypop/MainActivity.java" "$PKGDIR/"; rm -rf "$W/src/com/pdent/candypop"
fi
sed -i "s/com\.pdent\.candypop/$PKG/g" "$W/AndroidManifest.xml" "$PKGDIR/MainActivity.java"
sed -i "s/android:label=\"[^\"]*\"/android:label=\"$LABEL\"/" "$W/AndroidManifest.xml"
B="$W/build"; mkdir -p "$B/classes" "$B/dex"
echo "[1/5] aapt2 link"
"$T/aapt2" link -o "$B/base.apk" -I "$AJ" --manifest "$W/AndroidManifest.xml" -A "$W/assets" --auto-add-overlay
echo "[2/5] javac"
javac -nowarn -source 8 -target 8 -bootclasspath "$AJ" -classpath "$AJ" -d "$B/classes" $(find "$W/src" -name '*.java') 2>&1 | grep -v "warning" || true
echo "[3/5] d8"
java -cp "$T/lib/d8.jar" com.android.tools.r8.D8 --lib "$AJ" --min-api 21 --output "$B/dex" $(find "$B/classes" -name '*.class')
python3 - "$B/base.apk" "$B/dex/classes.dex" <<'PY'
import sys, zipfile
z = zipfile.ZipFile(sys.argv[1], 'a', zipfile.ZIP_DEFLATED); z.write(sys.argv[2], 'classes.dex'); z.close()
PY
echo "[4/5] zipalign"
"$T/zipalign" -f -p 4 "$B/base.apk" "$B/aligned.apk"
echo "[5/5] sign"
KS="$KIT/debug.keystore"
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias key -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=$NAME" >/dev/null 2>&1
mkdir -p "$KIT/out"
java -jar "$T/lib/apksigner.jar" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --ks-key-alias key --out "$KIT/out/$NAME.apk" "$B/aligned.apk"
java -jar "$T/lib/apksigner.jar" verify --verbose "$KIT/out/$NAME.apk" | head -5
(cd "$W" && python3 -c "import shutil; shutil.make_archive('$KIT/out/$NAME-source','zip','.')" )
rm -f "$KIT/out/$NAME-source.zip.tmp"
ls -la "$KIT/out"
echo "DONE: $KIT/out/$NAME.apk"
