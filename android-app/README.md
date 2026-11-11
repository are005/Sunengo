# App Android "Dati Huawei"

È la stessa app della cartella `huawei/` (quella pubblicata su GitHub Pages) impacchettata come app Android, con in più il passo **Verifica inverter**: il telefono si collega al Wi-Fi dell'inverter, legge come è programmato (batteria, fasce orarie, limitazione) e lo confronta con il progetto. **Sola lettura: non modifica nulla sull'inverter.**

Nell'app Android funzionano anche "Salva .json", la distinta `.csv` (si aprono con la condivisione di Android) e le stampe (stampa di sistema, anche "Salva come PDF").

## Scaricare e installare l'APK
1. Su GitHub: repository **Sunengo** → **Actions** → **App Android** → l'ultima esecuzione con la spunta verde.
2. In fondo alla pagina, in **Artifacts**, scarica `DatiHuawei-1.0.N` (è uno .zip che contiene l'APK).
3. Sul telefono apri l'APK e consenti l'installazione da "origini sconosciute" quando Android lo chiede.

Gli aggiornamenti si installano sopra la versione precedente senza disinstallare: tutte le versioni sono firmate con la stessa chiave.

**I cantieri salvati nel browser non passano da soli nell'app** (sono archivi separati): salvali con "Salva .json" e riaprili nell'app con "Apri file". Lo stesso vale per gli articoli (finestra Articoli → Esporta/Importa).

## Usare la verifica in cantiere
1. Collega il telefono al Wi-Fi dell'inverter (rete `SUN2000-…`). Se Android avvisa che la rete non ha Internet, scegli di restare connesso.
2. Apri il passo **Verifica inverter** e premi **Leggi dall'inverter**.
   - Wi-Fi dell'inverter: indirizzo `192.168.200.1`, porta `6607`, ID `0`. Se non risponde, l'app prova da sola anche ID `1` e porta `502`.
   - In alternativa, Smart Dongle nella rete del cliente: IP del dongle, porta `502`, ID = indirizzo RS485 dell'inverter. Serve "Modbus TCP" abilitato nel dongle (FusionSolar).
3. Se un valore risulta "non letto" o strano, premi **Salva lettura (.json)** e mandalo: serve per correggere la mappa dei registri.

La mappa dei registri Modbus è in `huawei/index.html` (costante `REGS`). Va confermata sul documento Huawei *Solar Inverter Modbus Interface Definitions* per ogni modello.

## Come viene compilata
Il workflow `.github/workflows/android.yml` parte a ogni modifica di `huawei/` o `android-app/`:
`npm ci` → `prepare.sh` (copia la web app, genera il progetto Android con Capacitor, aggiunge il codice nativo) → `./gradlew assembleDebug` → APK negli Artifacts.

- `native/TcpModbusPlugin.java`: connessione TCP verso l'inverter (legata al Wi-Fi), condivisione file, stampa.
- `native/MainActivity.java`: registra le funzioni native.
- `keystore/debug.keystore`: chiave di firma interna (password `android`), serve solo a firmare sempre allo stesso modo. Per pubblicare sul Play Store va creata una chiave di release da tenere fuori dal repository.

Compilazione in locale (serve JDK 17 e Android SDK): `cd android-app && npm ci && bash prepare.sh && cd android && ./gradlew assembleDebug`.
