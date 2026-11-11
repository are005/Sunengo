// Pacchetto Firebase per l'app Dati Huawei: solo le funzioni usate, esposte su window.FB
export { initializeApp } from 'firebase/app';
export { getAuth, initializeAuth, indexedDBLocalPersistence, browserLocalPersistence, signInWithEmailAndPassword, signOut, onAuthStateChanged, connectAuthEmulator } from 'firebase/auth';
export { initializeFirestore, persistentLocalCache, persistentMultipleTabManager, collection, doc, setDoc, onSnapshot, serverTimestamp, connectFirestoreEmulator } from 'firebase/firestore';
