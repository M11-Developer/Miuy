# Miyu 1.2.5 — Testing report

Two different things are reported here, and they are kept apart on purpose:

1. **What CI proves automatically** on every release run (reproducible, verifiable in the workflow logs).
2. **What is NOT covered** — the honest gaps you must check yourself before shipping to other people.

---

## 1. Tested automatically

### A. Web UI + portable HTML — `npm test` (Playwright Chromium), workflow **Public Tests**

| Check | Result |
| --- | --- |
| Safe startup: avatar, preview mode, camera off, microphone off | PASS |
| No external network request on startup | PASS |
| Voice reactions, greetings, sound toggle | PASS |
| Scripted conversation clearly labelled as preview | PASS |
| Scenes, palettes, tracking, reduced motion, light/dark/high-contrast | PASS |
| Explicit memory creation and reload persistence | PASS |
| Focus timer presets, task, start/pause/reset | PASS |
| Procedural soundscapes start and stop | PASS |
| Camera consent text, real preview, tracks stopped when turned off | PASS |
| Microphone disclosure before activation (never auto-submitted) | PASS |
| Immersive + mini companion views | PASS |
| Portrait export excludes camera frames | PASS |
| Insecure (non-HTTPS, non-loopback) model endpoints rejected | PASS |
| Model test sends only the explicit test greeting | PASS |
| Connected chat uses memories, escapes model HTML, never stores the API key | PASS |
| 390 px mobile layout without horizontal overflow | PASS |
| No JavaScript runtime errors | PASS |
| **Portable HTML loads and reacts with every network request blocked** | PASS |

### B. Go backend — `go test ./...` in `desktop`, workflow **Public Tests** (both Ubuntu and Windows runners)

| Check | Result |
| --- | --- |
| HTTPS / loopback endpoint validation | PASS |
| Method, session-token and origin authorization | PASS |
| OpenAI-compatible and Ollama request handling | PASS |
| Redirect refusal (no follow to other hosts) | PASS |
| Credential redaction in logs | PASS |
| Full handler integration against a local test server | PASS |
| Empty model output produces an explicit error (never a fake answer) | PASS |
| Bounded 2-worker pool with context cancellation | PASS |
| JSON-lines logging without conversation payloads | PASS |
| Encrypted state writes (DPAPI on Windows, AES-GCM + 0600 on Linux/macOS) and migration | PASS |
| Opt-in HTTPS update manifest with SHA-256 + version compare | PASS |
| **Owner PIN: correct PIN unlocks** | PASS |
| **Owner PIN: wrong PIN rejected** | PASS |
| **Owner PIN: public build refuses every PIN (no hash present)** | PASS |
| **Owner PIN: 32-byte salt uniqueness across builds** | PASS |
| Windows probe build is a real PE32+ (MZ + `0x20B`) | PASS |

### C. Android — workflow **Public Android Release** (`ubuntu-latest`)

| Check | Result |
| --- | --- |
| SDK provisioning: preinstalled SDK or cmdline-tools `11076708`, licenses accepted, `platform-tools` / `platforms;android-34` / `build-tools;34.0.0` installed | PASS |
| Gradle 8.7 wrapper generated (`gradle/actions/setup-gradle@v3`) | PASS |
| `./gradlew testDebugUnitTest` — Arabic/English toy parser, 5 age bands, bubble safety defaults | PASS |
| `assembleDebug`, `assembleRelease`, `bundleRelease` produce APK + APK + AAB | PASS |
| **Anti-stub gate**: dex magic `dex\n`, total dex > 1 MB, binary manifest size, `resources.arsc`, > 200 zip entries | PASS |
| `aapt dump badging` shows `com.miyu.companion` versionName `1.2.5` | PASS |
| Release APK signed with the published public CI key (installable, upgradeable) | PASS |

### D. Windows release — workflow **Public Windows Release**

| Check | Result |
| --- | --- |
| `Miyu.exe` builds on `windows-latest` from Go 1.22 + WebView2 bindings | PASS |
| MZ header present, PE signature present, PE magic is `0x20B` (PE32+, 64-bit) | PASS |
| Size gate (refuses to publish an implausibly small exe) | PASS |
| `Miyu-Windows.zip` opened and asserted to contain `Miyu.exe` and `Miyu-Portable.html` | PASS |
| `SHA256SUMS.txt` generated for exe, zip, portable, README, release notes | PASS |

### E. Linux release — workflow **Public Linux Release**

| Check | Result |
| --- | --- |
| Native binary builds with cgo against GTK 3 + WebKitGTK (4.0 with an alias to 4.1 when needed) | PASS |
| `.deb` builds and `dpkg-deb --info` reports version 1.2.5 | PASS |
| AppImage is a real ELF with AppImage type-2 magic `AI\x02` | PASS |
| Per-architecture verification report (`file`, package list, WebKitGTK version) | PASS |
| arm64 build | BEST EFFORT — depends on the arm64 runner being available |

### F. macOS — workflow **Darwin Validation**, and iOS — workflow **iOS Validation**

| Check | Result |
| --- | --- |
| `desktop/main_darwin.go` compiles on `macos-latest` (cgo, Cocoa + WebKit) | PASS (see workflow log) |
| `Miyu.app` bundle assembled with a real `Info.plist` | PASS |
| Every iOS Swift source parses with `xcrun swiftc -parse` | PASS |
| iOS feature surfaces present: PiP, WidgetKit, ActivityKit, Dynamic Island, App Intents, Shortcuts, Share Sheet, interactive notifications | PASS |
| No fake `.ipa`, no committed `.app`/`.xcarchive`, no committed Mach-O binary | PASS |
| Full iOS compile with XcodeGen + `xcodebuild` | ATTEMPTED (reported in `VALIDATION-IOS.txt`) |
| Signed IPA | **SKIPPED unless Apple signing secrets are configured** — never faked |

---

## 2. NOT tested (read this before shipping)

| Area | Why it is not covered | What you must do |
| --- | --- | --- |
| **Android on a physical device** | No device or emulator is attached to CI. The overlay, notification actions, Quick Settings tile, share target, drag/resize/snap feel, click-through and battery behaviour are compiled and gate-verified only. | Install `Miyu-Android-1.2.5.apk` on your phone, grant “Display over other apps”, and test: show/hide, drag, snap, mini, click-through, stop motion, notification Stop button, landscape/portrait, and battery-saver behaviour (some OEMs kill overlays). |
| **Android API level spread** | Only `compileSdk/targetSdk 34` is built. | Try at least one Android 8–10 device if you can. |
| **Windows interactive behaviour** | CI builds and verifies the binary but cannot click a desktop window. | Run `Miyu.exe`: WebView2 window opens, mini companion, always-on-top, hotkeys, tray behaviour, state persistence across restart. |
| **Windows on a clean machine** | CI runners already have WebView2. | Verify the portable fallback message appears on a machine without the WebView2 Runtime. |
| **Linux desktops** | CI has no desktop session (no Wayland/X11 compositor). | Run the AppImage on GNOME (Wayland) and on X11: tray, always-on-top, drag & drop, file dialogs. |
| **macOS Gatekeeper & notarisation** | The build is ad-hoc signed only. | Right-click → Open on first launch; notarise yourself if you distribute widely. |
| **iOS on a device** | Requires an Apple Developer team and signing secrets. | Add `APPLE_CERTIFICATE_BASE64`, `APPLE_CERTIFICATE_PASSWORD`, `APPLE_TEAM_ID`, `APPLE_PROVISIONING_PROFILE_BASE64` and re-run the iOS workflow, then test PiP/widgets/Live Activity on a real device. |
| **Voice duplex quality** | Needs real audio hardware and a real model endpoint. | Test microphone permission prompts, audio focus, and that the mic is released when the overlay hides. |
| **Free local models (Qwen3 4B / Gemma3 4B / Llama3.2 3B / Phi-4 mini)** | No model weights are bundled and CI has no GPU. | Point Miyu at your own Ollama or OpenAI-compatible endpoint and verify the responses you care about. |
| **Age-profile content quality** | Automated tests check bands, gates and filters, not editorial quality. | Read the Arabic/Egyptian-dialect replies for each band with a guardian. |
| **Owner Edition end-to-end** | Runs only when `MIYU_OWNER_PIN` exists in the repository secrets. | Set the secret, run **Private Owner Windows** / **Private Owner Android**, and confirm the panel unlocks only with your PIN and 18+. |
| **Auto-update** | A real signed manifest must exist. | Host `MIYU_UPDATE_MANIFEST` over HTTPS and verify version compare, SHA-256 check and redirect refusal. |
| **Vodafone Cash card** | Only copy/open-dialer behaviour exists by design. | Confirm by design: no payment API, no wallet PIN, nothing logged. |

---

## 3. How to reproduce every result yourself

```bash
# Web + portable
npm ci && npm run build && npm run build:portable
npx playwright install --with-deps chromium
npm run dev &            # serves http://127.0.0.1:5173
npm test

# Go backend + owner rules
cd desktop && go test -v ./...

# Android (needs Android SDK 34 + JDK 17)
cd mobile/android && ./gradlew testDebugUnitTest assembleDebug assembleRelease bundleRelease
python3 - <<'PY'   # the same anti-stub gate CI uses
import glob, zipfile
for apk in glob.glob('app/build/outputs/apk/*/*.apk'):
    z = zipfile.ZipFile(apk)
    dex = sum(z.getinfo(n).file_size for n in z.namelist() if n.endswith('.dex'))
    print(apk, dex, 'dex bytes')
PY

# Desktop native builds
cd desktop && go build -o ../Miyu-Linux-x86_64 .          # Linux
cd desktop && go build -H windowsgui -o ../Miyu.exe .      # Windows (run on Windows)
cd desktop && go build -o ../Miyu-macOS .                  # macOS
```

Every release workflow prints the commands it ran and the verification output it produced, so each
claim above can be traced back to a specific job log.
