# AFRC CHAT 2.0

Application Android de messagerie privée avec backend Node.js + Socket.IO.

## Fonctionnalités
- Email + mot de passe, sans numéro de téléphone
- Profil / username
- Discussions privées et groupes (API)
- Texte, médias et fichiers (API upload)
- Messages vocaux / permission microphone côté Android
- Réactions, réponses, modification, suppression
- Statuts envoyé/reçu/lu via Socket.IO
- Présence en ligne / dernière connexion
- Stories 24 h
- Recherche utilisateurs
- Notifications Android permission-ready
- Blocage / signalement
- Mode sombre
- Socket.IO temps réel
- Icône AFRC CHAT incluse

## Règle absolue : aucun appel
Le projet ne contient pas de téléphonie, VoIP, WebRTC, STUN, TURN, SIP ni événements `call:*`.

## Backend local
```bash
cd backend
cp .env.example .env
npm install
npm start
```

## Android
L'URL par défaut pour l'émulateur Android est `http://10.0.2.2:10000`. Pour un téléphone réel, remplace `BuildConfig.API_BASE_URL` par l'URL publique de ton serveur Render ou l'IP locale de ton serveur.

## GitHub Actions
Chaque push sur `main` ou `master` compile `AFRC-CHAT-v2-debug-apk`.
