# Miyu — a little company, a little magic

Miyu is an animated, local-first companion and safe play lab. This repository contains the multi-platform product: **Native Desktop (Windows, Linux)** as primary, **Native Mobile (Android, iOS)** and **Portable HTML fallback**.

> **Product principle**: Native Desktop is the primary product. Portable HTML is fallback and testing only.

## Quick Start

### Windows (Primary Product)

1. Extract `Miyu-Windows.zip`
2. Run `Miyu.exe` (requires Microsoft Edge WebView2 Runtime, normally present on Win10/11)
3. If WebView2 missing: https://developer.microsoft.com/en-us/microsoft-edge/webview2/
4. Try Play Lab: "امسكي عربية لعبة" or "Hold a toy car"

The EXE embeds UI via Go `embed`, no external UI files after build.

Features:
- Microsoft WebView2 + Go backend
- WebView2 Bridge with token + origin validation
- Always on top, Mini Companion, System Tray (Windows)
- Global hotkeys where possible
- Local encrypted state (DPAPI on Windows)
- Bounded worker pool, structured logging, crash recovery

### Linux (Native)

- `Miyu-Linux-x86_64.AppImage` - AppImage, works on most distros
- `Miyu-Linux-x86_64.deb` - Debian package

Requirements: GTK3, WebKitGTK 4.1

```bash
chmod +x Miyu-Linux-x86_64.AppImage
./Miyu-Linux-x86_64.AppImage
# or
sudo dpkg -i Miyu-Linux-x86_64.deb
```

- Supports Wayland and X11 (WebKitGTK handles it, differences noted in README-Linux.md)
- System Tray, Always-on-top where WM supports
- Drag & drop character, keyboard shortcuts
- XDG_DATA_HOME / XDG_CONFIG_HOME storage
- AES-GCM encrypted secrets with 0600 permissions
- Local logs with secure permissions

See `README-Linux.md` for Wayland vs X11 details.

### Android (Native Mobile)

- `Miyu-Android.apk` - Test build
- `Miyu-Android.aab` - Play Store bundle

**Floating Companion** (key feature):
- Requests `SYSTEM_ALERT_WINDOW` explicitly, never auto-enabled
- Bubble can be dragged, resized, transparency changed, snapped to edge, hidden, mini mode, click-through, stop motion, open controls
- Respects Safe Area, Cutouts, Notches, portrait/landscape
- Saves position locally, snap to edges, animation on drag
- Idle breathing, blink, speech mouth movement
- Toy overlay with animation when you say "امسكي عربية لعبة"
- Foreground Service with persistent notification and stop button
- Respects Battery Saver
- No mic in background without permission, no camera auto-start
- No reading other apps, no keylogging, no screen monitoring, no auto-clicking
- "Open other apps" means Android Intent to launch YouTube/Chrome/Focus, not controlling content

See `README-Android.md` for permission flow.

### iOS (Alternative Companion)

iOS **does NOT** allow free overlay above all apps like Android. This is iOS system limitation, not Miyu's.

Real alternatives provided:
- Picture-in-Picture for Miyu screen
- Home Screen Widget
- Lock Screen Widget
- Live Activity
- Dynamic Island (where supported)
- Siri / App Intents ("افتحي Miyu", "شغلي وقت التركيز")
- Share Sheet to send text to Miyu
- Interactive notifications
- Shortcuts integration

No fake IPA. If Apple signing secrets available, TestFlight build can be created. Otherwise Xcode project validation only.

See `README-iOS.md`.

## Play Lab

Say or type:

- امسكي عربية لعبة / Hold a toy car
- امسكي كرة / Hold a ball
- امسكي دبدوب / Hold a teddy bear
- امسكي كتاب / Hold a story book
- امسكي صاروخ / Hold a rocket

Miyu:
- Understands Arabic (Egyptian dialect + MSA) and English
- Detects toy, shows toy in scene with animation
- Character moves with it, expression changes, speech bubble
- Activity log in Play Lab

Toys: Toy Car, Ball, Teddy Bear, Story Book, Rocket, Flower, Puzzle, Balloon, Musical Toy

Experimental toy generator is local sprite/emoji/vector, not claimed as real image generation.

## Arabic-First

- Full Arabic UI when Arabic selected
- RTL support
- Natural Arabic messages
- Egyptian dialect understanding (best effort)
- Toy commands in Egyptian + MSA
- Arabic Speech Recognition (OS provides)
- Arabic TTS (OS voices)
- Arabic voice selection
- Language level per age
- Short, kind replies for children
- No forced English in Arabic mode

Test phrases:
- امسكي عربية لعبة
- كلميني بالعربي
- أنا زعلان
- عايز أذاكر
- افتحي يوتيوب
- شغلّي وقت التركيز
- اعملي حركة
- امسكي دبدوب

## Age Profiles & Safety

Profiles:
- 3-5 years
- 6-8 years
- 9-12 years
- 13-17 years
- 18+

Parent/guardian chooses. Changes per age:
- Speech style, vocabulary, toys, advice, filter strength, session length, guardian suggestion, notifications, voice

Features:
- Parent/Guardian confirmation
- Parent Dashboard
- Reset Profile
- Local-only storage
- Respect Meter
- Mood System

When abusive speech:
- Filtered in normal mode
- Miyu becomes sad/upset (role-play)
- Guidance to kind speech
- Not sent to external model

## Owner Edition (Private)

**Security rules:**
- Never put raw password in source, README, logs, public Release, Actions logs
- Use GitHub Actions Secret `MIYU_OWNER_PIN`
- Build-time PBKDF2 or Argon2, random salt, hash only
- Owner Edition separate from Public

Owner Panel contains:
- Owner Lab Mode
- Experimental Toy Library
- Diagnostics
- Voice Controls / Full Duplex
- Adult Profile Controls
- Kind-language Filter Control
- Local Debug Status
- Export non-sensitive settings
- Lock Panel
- Build ID / Version

Owner Lab only works if:
- Correct PIN entered
- 18+ mode
- Private build
- Build config allows

Public Edition:
- Owner Panel disabled
- No active Owner PIN
- No private hash
- Cannot activate Owner Edition from public build

Safety limits never disabled:
- No sexual content for minors
- No self-harm encouragement
- No dangerous instructions
- No illegal guidance
- No theft/surveillance
- No claiming Miyu is real human

Build Owner Edition:
```bash
# Set secret in GitHub Actions
# Workflow private-owner-windows.yml uses MIYU_OWNER_PIN
# Local test (don't commit):
MIYU_OWNER_PIN=yourpin go run ./tools/gen-owner-hash.go
```

## Voice & Duplex

- Listen with user permission
- Speak via device TTS
- Listen while speaking (full duplex) if enabled
- Stop on Mic Off
- No auto-send text
- Arabic & English
- Audio Focus
- Mic stops when app closed or overlay hidden (if user chooses)
- No raw audio files saved
- No background mic without notification + permission

## Free AI Providers

- Qwen 3 4B
- Gemma 3 4B
- Llama 3.2 3B
- Phi-4 mini

Support:
- Ollama
- OpenAI-compatible local endpoints
- Localhost
- HTTPS remote only
- API key session-only
- Connection test separate
- No Camera/Screen/App data sent
- No memories sent except when chatting

## Go Backend

- Goroutines, Channels, Bounded Worker Pool (2 workers)
- HTTP client timeouts (85s), Context cancellation, Request queue
- Structured logging (JSON lines, no secrets), Crash recovery
- Memory limits (4MB state, 2MB response, 1MB request), Response size limits
- API key redaction, Redirect blocking, Loopback bridge (127.0.0.1 only)
- Origin validation, Session token (32 random bytes hex)
- Native WebView2 routing
- DPAPI on Windows, AES-GCM + 0600 on Linux, Keychain-like on Android (EncryptedSharedPreferences)
- Atomic file writes (.tmp + rename), Backup/Restore via export

## Themes, Plugins, UI

- Light, Dark, System, High Contrast, Reduced Motion
- Child-friendly, Teen, Adult Owner themes
- Plugin interface safe, local only, permissions
- Plugins cannot read API keys, Camera/Mic without permission
- Plugin Manager, Theme Manager pages

Phone design:
- Floating bubble, Radial quick menu, Drag handles, Mood ring, Play/Mic/Open/Focus/Hide/Settings/Parent lock

Desktop design:
- Sidebar, Companion Stage, Chat, Play Lab, Focus Room, Memories, Settings, Owner Admin, System Tray

## Vodafone Cash Support

Support card shows `01027653109` with:
- No Payment API
- No auto-transfer
- No Wallet PIN storage
- No payment data storage
- Copy button, Open Phone button
- Verify number alert
- Children need guardian permission
- No financial data in logs/exports

## Auto Update

- HTTPS manifest only
- SHA256 verification
- Release Notes
- Version comparison
- Redirect blocking
- No execution of unsigned file
- Windows signed release when cert available
- Android Play Store flow, Linux package notes, iOS TestFlight/App Store flow

If no real signature, update shows need for signed Release, not claiming full auto-updater.

## Releases

1. Public Windows: Miyu-Windows.zip, Miyu.exe, Miyu-Portable.html, README, LICENSES, SHA256SUMS.txt
2. Public Linux: AppImage, .deb, README-Linux.md, SHA256SUMS.txt
3. Public Android: APK (test), AAB (store), README-Android.md
4. iOS: No fake IPA, Xcode project, TestFlight needs signing
5. Private Owner Windows: Miyu-Owner-Windows.zip, Owner-Portable.html, OWNER-ADMIN-GUIDE.txt (NOT public)
6. Private Owner Android: APK/AAB separate, PIN from Secret, NOT public

## GitHub Actions Matrix

- public-windows-release.yml (windows-latest, Node, Go, build exe, zip, checksum, artifact, Release)
- public-linux-release.yml (ubuntu-latest, AppImage, deb, checksum)
- public-android-release.yml (ubuntu-latest, Java, Gradle, APK/AAB)
- private-owner-windows.yml (windows-latest, uses MIYU_OWNER_PIN secret, PBKDF2 hash, private artifact)
- private-owner-android.yml (ubuntu-latest, owner build)

Public workflows never use Owner secret.

## Testing

```bash
npm ci
npm run build
npm run build:portable
npm test
npm run test:proxy
# Go tests
cd desktop && go test -v .
# Windows native build
# Linux AppImage build
# Android debug APK
# iOS project validation
```

Covered:
- Arabic text, Arabic speech fallback
- Floating overlay permission, drag/resize, hide/show
- App launch Intent, Mic permission, Camera remains off
- Owner PIN success/failure, Owner vs Public
- API key persistence, DPAPI/local encryption, Crash recovery
- Release checksum, Mobile sizes, Tablet, Desktop responsive

If Windows/Android not tested physically, noted in TESTING.md.

## Documentation

- README.md (this)
- START HERE.txt
- TESTING.md
- THIRD-PARTY-NOTICES.txt
- README-Windows.md
- README-Linux.md
- README-Android.md
- README-iOS.md
- OWNER-ADMIN-GUIDE.txt (private)
- Release Notes

## Final Rules

- Don't just edit browser version
- Must build Miyu.exe really
- Don't use old Miyu.exe in new Release
- No Release without Assets
- No Owner PIN in source/logs
- No Owner ZIP to public repo
- No password repetition in response
- No GitHub tokens requested
- No Merge before Release complete
- No closing PR early
- Don't claim Release done if not in GitHub Releases

## Build & Modify Source

```bash
npm ci
npm run dev
npm run build
npm run build:portable
# Windows (needs Go, on Windows)
npm run package:win
# Cross-compile from Linux
npm run build:portable
cd desktop
go run ./resourcegen
GOOS=windows GOARCH=amd64 CGO_ENABLED=0 go build -trimpath -ldflags='-s -w -H windowsgui' -o ../Miyu.exe .
```

## Licenses

Fonts: Manrope, Lora (OFL)
Icons: Lucide (ISC)
Windows wrapper: go-webview2, go-winloader, Go sys
WebView2 runtime by Microsoft, not bundled
See licenses/ and THIRD-PARTY-NOTICES.txt
