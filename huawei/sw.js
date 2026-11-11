// Cache per uso offline dell'app Huawei. Aumentare VERSION quando si aggiorna index.html.
const VERSION = 'dati-huawei-v7';
const FILES = ['./', 'index.html', 'manifest.webmanifest', 'vendor/firebase.js', '../icon-180.png', '../icon-192.png', '../icon-512.png'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(VERSION).then(c => c.addAll(FILES)).then(() => self.skipWaiting()));
});

// Elimina solo le vecchie cache di questa app (le cache "dati-ksenia-*" appartengono all'app Ksenia)
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys()
    .then(keys => Promise.all(keys.filter(k => k.startsWith('dati-huawei-') && k !== VERSION).map(k => caches.delete(k))))
    .then(() => self.clients.claim()));
});

// Rete prima (così gli aggiornamenti arrivano subito), cache se offline.
// Solo i file dell'app: le connessioni all'archivio condiviso (Firebase) non passano dalla cache.
self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET' || new URL(e.request.url).origin !== self.location.origin) return;
  e.respondWith(
    fetch(e.request)
      .then(r => { const copy = r.clone(); caches.open(VERSION).then(c => c.put(e.request, copy)); return r; })
      .catch(() => caches.match(e.request).then(r => r || caches.match('index.html')))
  );
});
