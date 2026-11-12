# Pacchetto Firebase dell'app

`huawei/vendor/firebase.js` contiene solo le parti di Firebase usate dall'app (accesso con email e password, database Firestore con cache offline), raccolte in un unico file che espone `window.FB`. È incluso nell'app invece di essere scaricato da Internet, così l'archivio funziona anche senza rete e nell'app Android.

Per rigenerarlo (per esempio per aggiornare Firebase): `cd tools/firebase-bundle && npm install && npm run build`.

Firebase JavaScript SDK: Apache License 2.0 (testo delle licenze in fondo al file).
