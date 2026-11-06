NATIVE ANDROID APP KIT (Java, plain Android SDK, free, no ads)

Your app lives in app/ :
  app/AndroidManifest.xml          app name, package, launcher activity
  app/java/<package>/*.java        the app code (Activity + Views)
  app/app.env                      APP_NAME=MyApp  (APK file name)
  app/res/, app/assets/            optional

ROUTE A - build on GitHub: change anything in app/ and push. "Build APK" runs and puts the APK at
  Releases > latest > app.apk   (and the "apk" artifact on the run).
ROUTE B - build in ChatGPT's workspace: download the "toolchain" artifact/release (toolchain.zip),
  unzip, add app/, run  bash build.sh app.  See TOOLCHAIN.txt.
