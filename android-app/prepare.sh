#!/usr/bin/env bash
# Prepara il progetto Android (Capacitor) dell'app "Dati Huawei" a partire dalla web app in ../huawei.
# Uso: cd android-app && npm ci && VERSION_CODE=12 bash prepare.sh && cd android && ./gradlew assembleDebug
set -euo pipefail
cd "$(dirname "$0")"
VERSION_CODE="${VERSION_CODE:-1}"

# 1. Pagina web: la stessa app pubblicata su GitHub Pages
rm -rf www && mkdir -p www
cp ../huawei/index.html www/index.html
cp -r ../huawei/vendor www/vendor
cp ../icon-180.png ../icon-192.png ../icon-512.png www/

# 2. Progetto Android generato da Capacitor (non salvato nel repository)
[ -d android ] || npx cap add android
SRC=android/app/src/main
cp native/MainActivity.java native/TcpModbusPlugin.java "$SRC/java/it/sunengo/datihuawei/"

# 3. Permessi, tema senza schermata Capacitor, versione
M="$SRC/AndroidManifest.xml"
grep -q ACCESS_NETWORK_STATE "$M" || sed -i 's#<uses-permission android:name="android.permission.INTERNET" />#&\n    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />#' "$M"
sed -i 's#@style/AppTheme.NoActionBarLaunch#@style/AppTheme.NoActionBar#' "$M"
grep -q 'fileprovider' "$M" || { echo "FileProvider mancante nel manifest" >&2; exit 1; }
sed -i -E "s/versionCode [0-9]+/versionCode ${VERSION_CODE}/; s/versionName \"[^\"]*\"/versionName \"1.0.${VERSION_CODE}\"/" android/app/build.gradle

# 4. Icona SunEnGo
RES="$SRC/res"
rm -rf "$RES/mipmap-anydpi-v26"
for d in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
  for n in ic_launcher ic_launcher_round ic_launcher_foreground; do cp ../icon-192.png "$RES/mipmap-$d/$n.png"; done
done

# 5. Copia la pagina web nel progetto Android
npx cap sync android
echo "Progetto Android pronto (versione 1.0.${VERSION_CODE})"
