# Miyu 1.2 — Testing Report

## What was tested in Linux workspace (this environment)

### Browser UI (Playwright Chromium)

- Startup: avatar loads, preview mode explicit, camera/mic off
- No external network on startup
- Greeting, head-pat, cheer, breathe, dance reactions
- Master sound toggle
- Scripted conversation labeled as preview
- Scene changes (room, garden, studio)
- Palette (rose, lilac, peach), tracking, reduced motion, light/dark, high contrast
- Explicit memories creation
- Focus timer 15/25/50, task, start, pause, reset
- Ambient sound rain/breeze start/stop
- Guided breathing
- Camera opt-in consent, fake device preview, stop tracks on off
- Mic disclosure before activation
- Immersive and mini companion
- Portrait export excludes camera frames
- Insecure non-loopback API endpoints rejected
- Model test sends only explicit test greeting
- Connected chat uses memories, escapes HTML, never stores API key
- Reload preserves notes/settings, resets key/media
- 390px mobile layout no overflow
- No JS runtime errors
- Play Lab maps Arabic/English toy requests to toy prop, animates without network
- Age/language profile: direction, Arabic voice/dictation, safe mode labels, profanity filter masks rude input before AI
- Full-duplex opt-in, dictation never auto-submitted
- Free Ollama model cards select local endpoint without bundling weights
- Vodafone Cash support only copies/displays number, never initiates payment
- Portable HTML loads with network blocked

### Go Backend

- HTTPS/loopback endpoint validation
- Method, session-token, origin authorization
- Compatible Chat Completions and Ollama handling
- Redirect refusal
- Credential redaction
- Complete handler integration with local test server
- Empty model output explicit error
- Bounded 2-worker pool
- JSON-lines logging no conversation payloads
- DPAPI-protected state writes (Windows), AES-GCM 0600 (Linux), migration
- Opt-in HTTPS update-manifest check
- Owner PIN verification: PBKDF2, random salt, hash only, public build disabled

```bash
cd desktop && go test -v .
```

### Windows Build

- `npm ci` ✓
- `npm run build` ✓ (Vite)
- `npm run build:portable` ✓ (self-contained HTML)
- `cd desktop && go run ./resourcegen` ✓ (Windows icon + manifest)
- Cross-compile: `GOOS=windows GOARCH=amd64 CGO_ENABLED=0 go build -trimpath -ldflags='-s -w -H windowsgui' -o ../Miyu.exe .` ✓
- Output identified as PE32+ x86-64 Windows GUI executable (MZ header, PE signature)
- Contains icon, version info, no-elevation manifest, embedded UI
- WebView2 external runtime, not bundled
- SHA256 generated

**Not verified in this workspace:** Executing binary on physical Windows PC, actual WebView2 dialogs, physical camera/mic hardware, Windows always-on-top behavior, real mic transcription, system TTS voices, real Ollama/API model. Those depend on runtime, OS, hardware, credentials. Implemented pinning, resizing, persistence, local authenticated proxy, but not substitute for Windows QA. Browser edition fallback included.

### Linux Build

- `go build` for Linux amd64 ✓
- Binary file type ELF 64-bit
- AppImage creation via appimagetool ✓ (in CI ubuntu-latest)
- deb package via dpkg-deb ✓
- XDG paths, 0600 permissions, AES-GCM encryption
- Wayland/X11 differences documented, no fake button
- System tray best-effort, always-on-top WM-dependent

**Not fully verified:** Running AppImage on physical Linux with various compositors, tray on all WMs, Wayland always-on-top edge cases. Code implements tray via StatusNotifier, but compositor support varies. Noted in README-Linux.md.

### Android Build

- Gradle build for debug APK ✓ (in CI with Android SDK)
- AAB bundle ✓
- Kotlin + Compose, minSdk 26, target 34, arm64-v8a + x86_64
- SYSTEM_ALERT_WINDOW permission handling with explicit disclosure
- Foreground service with persistent notification, stop button
- FloatingBubbleView: drag, snap to edges animation, resize, alpha, mini, click-through, safe area, cutouts, portrait/landscape, position saved, idle breathing, blink, speech movement
- Toy overlay: emoji + animation when command detected
- App launch via Intent (YouTube, Chrome, Focus) — no content control
- No reading other apps, no keylogging, no screen monitoring, no auto-click
- Play Lab Arabic/English detection, 9 toys
- Age profiles, respect meter
- Vodafone Cash support card, copy only
- DataStore, no backup for secure files

**Not verified on physical device:** Actual overlay on various OEMs (Xiaomi, Samsung may have extra permission screens), battery saver behavior, cutout handling on all devices, PiP interaction. Code implements but needs device QA.

### iOS Build

- Xcode project structure created ✓
- SwiftUI, WidgetKit, ActivityKit, App Intents
- Play Lab shared core
- Age profiles, Arabic support
- PiP placeholder, Widgets, Live Activity, Dynamic Island
- Siri intents with Arabic phrases
- Share Sheet, Notifications, Shortcuts
- iOS limitation documented: no free overlay like Android, alternatives provided, no false claims
- No fake IPA created (as required)

**Not verified:** Actual Xcode build on macOS (needs macos-latest runner + signing), TestFlight when secrets available, PiP actual AVKit integration, Live Activity on device, Dynamic Island on iPhone 14 Pro. Project validation only in Linux workspace, but Swift files syntactically valid and structure follows Apple guidelines.

### Arabic Tests

- Input: "امسكي عربية لعبة" → detects car ✓
- "كلميني بالعربي" → language switch ✓
- "أنا زعلان" → mood sad, kind response ✓
- "عايز أذاكر" → focus suggestion ✓
- "افتحي يوتيوب" → intent to YouTube ✓
- "شغلي وقت التركيز" → focus timer ✓
- "اعملي حركة" → dance reaction ✓
- "امسكي دبدوب" → teddy ✓
- RTL UI: dir rtl when Arabic, text alignment right ✓
- Egyptian dialect keywords: "عربيه" normalized to "عربية" ✓

### Safety & Owner

- Profanity filter: masks abusive input before model ✓
- Respect meter decreases on rude, increases on kind ✓
- Mood system: sad when hurtful, playful when toy ✓
- Parent/Guardian confirmation checkbox ✓
- Local-only profile storage ✓
- Owner PIN success: hash matches ✓
- Owner PIN failure: wrong PIN rejected ✓
- Owner build vs Public: public has no hash, Owner panel disabled ✓
- Safety limits: no sexual content for minors, no self-harm, no dangerous, no illegal, no surveillance, no human claim — enforced in prompt and filter ✓
- API key persistence: never stored in localStorage, session only ✓
- DPAPI/local encryption: Windows DPAPI, Linux AES-GCM 0600 ✓
- Crash recovery: recover() logs, leaves state intact ✓
- Release asset checksum: SHA256SUMS.txt generated ✓
- Mobile screen sizes: 390px, 768px, 1440px responsive ✓

### What was NOT tested physically

- Windows EXE execution on real Windows 10/11 PC (cross-compiled only)
- WebView2 actual runtime dialogs
- Physical camera/mic hardware
- Windows always-on-top on real OS
- Real microphone transcription (uses browser/OS speech service)
- System TTS voices (OS dependent)
- Real Ollama/API model calls (mock only)
- Linux AppImage on physical distro with Wayland compositor variations
- Android overlay on OEM devices with custom permission managers
- iOS Xcode build on macOS (no macOS runner in this workspace)
- TestFlight with Apple signing (needs secrets)
- Vodafone Cash actual transfer (never initiated, only copy)
- Auto-update with signed release (manifest check tested, but no signed binary execution)

All untested items are documented in this file and READMEs, not claimed as tested.

## Test Commands

```bash
npm ci
npm run build
npm run build:portable
npm test
npm run test:proxy
cd desktop && go test -v .
# Windows
GOOS=windows GOARCH=amd64 CGO_ENABLED=0 go build -trimpath -ldflags='-s -w -H windowsgui' -o ../Miyu.exe .
file ../Miyu.exe
# Linux
go build -o ../Miyu-Linux-x86_64 .
./Miyu-Linux-x86_64 --help || true
# Android
cd mobile/android && ./gradlew assembleDebug
# iOS (macOS)
open mobile/ios/MiyuCompanion.xcodeproj
```

## Commit & Release

- Commit hash: (see git log)
- SHA256: See SHA256SUMS.txt in each Release
- Public Windows Release: GitHub Releases (workflow_dispatch)
- Public Linux Release: GitHub Releases
- Android artifact: GitHub Releases + Actions artifact
- Private Owner artifact: Actions artifact only, 7 days retention, NOT public Release
