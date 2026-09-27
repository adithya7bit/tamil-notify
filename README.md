# Tamil Notify • அறிவிப்பு மொழிபெயர்ப்பாளர்

An end-to-end production solution for intercepting, protecting sensitive tokens (OTPs, currencies, URLs, dates), and translating notifications to Tamil.

Available as both a **Native Android Application** (for automatic background interception across third-party apps like WhatsApp, SMS, and Telegram) and a **Mobile Progressive Web App (PWA)** (for cross-platform translation, notification simulation, and device push alerts).

---

## 🚀 Live Public Deployments

* **Mobile PWA (Vercel)**: [https://frontend-psi-ecru-71.vercel.app](https://frontend-psi-ecru-71.vercel.app)
* **Python FastAPI Backend (Vercel)**: [https://backend-phi-lemon-83.vercel.app](https://backend-phi-lemon-83.vercel.app/api/health)
* **PWA Mirror (Netlify)**: [https://tamil-notify-6929.netlify.app](https://tamil-notify-6929.netlify.app)
* **Cloud Database & Auth**: Supabase

---

## 📱 1. Native Android Application

Built with Kotlin, Jetpack Compose (Material3), and Google ML Kit on-device translation.

### Features
* **System Notification Interception**: Runs as a background `NotificationListenerService` that captures incoming notifications without requiring the app to stay open.
* **On-Device Offline Translation**: Uses Google ML Kit translation weights (~30MB) locally on the device with zero cloud API latency or privacy exposure.
* **Sensitive Token Protection**: Regex masking preserves URLs, OTPs, currency amounts (`$4,500`, `Rs.`), dates, phone numbers, and emails verbatim.
* **Safe Reposting & Attribution**: Reposts translated notifications showing the source app's color launcher icon, sender attribution, and dual text (Tamil translation + original English).
* **Self-Loop Prevention**: Explicit flags (`com.tamilnotify.app.IS_REPOSTED`) and tags prevent infinite recursion.
* **DataStore Settings**: Per-app translation toggles, target language selection, display modes, and offline model management.

### Build & Run
```bash
# Open in Android Studio or build via Gradle
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🌐 2. Mobile Progressive Web App (PWA)

Built with React 18, Vite, and modern mobile-first styling.

### Features
* **PWA Standalone Mode**: Installable directly onto mobile home screens (iOS Safari & Android Chrome) via `manifest.json` and service worker (`sw.js`).
* **Notification Simulator**: Interactive test presets for WhatsApp, Bank OTPs, Food Delivery, and Flight alerts.
* **Native Device Alerts**: Uses the Web Notifications API to trigger actual native notifications on your phone status bar or desktop.
* **Voice Speech-to-Text & Text-to-Speech**: Speak notifications directly using speech recognition and listen to Tamil pronunciations with Web Speech API.
* **Supabase Cloud Sync**: Real-time cross-device sync and history.

---

## ⚡ 3. Python FastAPI Backend

Serverless API hosted on Vercel East (iad1).

* `GET /api/health`: Health status endpoint.
* `POST /api/translate`: Token protection and multi-engine fallback translation pipeline.

---

## 📁 Repository Structure

```text
├── app/                  # Native Android Kotlin + Jetpack Compose app
│   ├── src/main/java/    # Service, Pipeline, TokenProtector, ML Kit, UI
│   └── AndroidManifest.xml
├── frontend/             # Mobile React PWA (Vercel & Netlify)
│   ├── src/              # App.jsx, tokenProtector.js, supabaseClient.js
│   └── public/           # manifest.json, sw.js, icon.svg
├── backend/              # Python FastAPI serverless backend (Vercel)
│   ├── main.py           # FastAPI routes & token masking
│   └── vercel.json       # Vercel serverless configuration
├── gradle/               # Gradle wrapper & libs.versions.toml
└── settings.gradle.kts   # Gradle settings
```

---

## 🔒 Security & Privacy
Sensitive credentials (`API/api.md`, `.env`, build caches) are excluded from version control via `.gitignore`.
