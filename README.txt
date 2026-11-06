ANDROID APK KIT (free, no ads) - for ChatGPT's code workspace
Needs: Java 17+ (javac, keytool), python3, Linux x86-64. No internet needed.

1. Unzip this kit:  unzip android-apk-kit.zip -d kit
2. Save your single-file HTML game, e.g. game.html
3. Run:  bash kit/build.sh game.html CandyPop com.pdent.candypop
4. Get: kit/out/CandyPop.apk (install on Android) and kit/out/CandyPop-source.zip

Contents: tools/aapt2, tools/zipalign, tools/lib/d8.jar, tools/lib/apksigner.jar (Android build-tools 34),
android.jar (API 34), template/ (WebView app: AndroidManifest.xml, MainActivity.java, assets/index.html).
The app opens assets/index.html full screen, offline. Signed with a debug key (fine for sharing APKs, not Play Store).
If zipalign fails on missing libc++, LD_LIBRARY_PATH is already set to tools/lib64 by build.sh.
