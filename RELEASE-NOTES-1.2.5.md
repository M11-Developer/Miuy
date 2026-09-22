# Miyu 1.2.5 — release notes

**Miyu is no longer a browser-only project.** 1.2.5 is the first release where every platform is a
real build produced by CI, on real runners, with verification recorded in the release itself.

| Platform | What ships | How it is verified |
| --- | --- | --- |
| Windows (primary) | `Miyu.exe` (Go + WebView2, UI embedded), `Miyu-Windows.zip` | MZ header + PE magic `0x20B` (PE32+ 64-bit) + size gate, `PE32-VERIFICATION.txt` in the release, zip contents asserted to contain the exe |
| Linux | `Miyu-Linux-x86_64.AppImage`, `.deb`, raw binary (arm64 best effort) | `file` output, ELF + AppImage `AI\x02` magic check, `dpkg-deb --info` version gate 1.2.5, per-arch report |
| macOS | `Miyu-macOS-<arch>.zip` (`Miyu.app`), raw binary | Go + cgo Cocoa/WebKit build on `macos-latest`, app bundle assembled, `file` + signature report |
| Android | `Miyu-Android-1.2.5.apk` (release), debug APK, `Miyu-Android-1.2.5.aab` | Anti-stub gate: dex magic, total dex size > 1 MB, binary manifest size, `resources.arsc`, entry count; `aapt` badging shows `com.miyu.companion` 1.2.5 |
| iOS | Xcode project + Swift sources + validation report (**no fake IPA**) | every Swift file parsed with `swiftc -parse`, feature-surface checks, compile attempt, explicit "no IPA without Apple signing" statement |
| Portable fallback | `Miyu-Portable.html` (~4 MB, single file) | Playwright suite runs it with **all network requests blocked** |

## What is new in 1.2.5

### Android — real, native floating companion
- Kotlin + Jetpack Compose app, `com.miyu.companion`, `minSdk 26`, `targetSdk 34`, `versionCode 15`, `versionName 1.2.5`.
- `FloatingCompanionService` runs as a foreground service (`foregroundServiceType="specialUse"`) with a
  persistent notification and **Stop / Hide / Controls** actions.
- `FloatingBubbleView` draws the companion in a `TYPE_APPLICATION_OVERLAY` window: drag, resize
  (0.5x–1.5x), transparency, animated snap-to-edge, mini mode, hide, and an opt-in click-through mode
  (`FLAG_NOT_TOUCHABLE`).
- Toy overlay plus an Arabic speech bubble when a toy is grabbed; idle breathing, eye blinking and a
  stop-motion switch that freezes every animation.
- Placement respects status bar / notch and navigation bar, in portrait and landscape.
- The overlay is always user-granted through system settings; the app never enables it silently.
- JVM unit tests (`testDebugUnitTest`) cover the Arabic/English toy parser, the 5 age bands and the
  bubble safety defaults.

### Build system fixes that unblocked CI
- The Compose Gradle plugin id was pinned to Kotlin `1.9.22`, which does not exist for
  `org.jetbrains.kotlin.plugin.compose`. The project now uses Kotlin **2.0.21** + AGP **8.5.2** +
  Gradle **8.7**, lock-step.
- `androidx.savedstate:2.1.1`-era APIs and `androidx.lifecycle` owners are now supplied to the
  overlay window so `ComposeView.setContent()` cannot throw on a WindowManager view.
- Android SDK provisioning is deterministic: preinstalled SDK first, otherwise cmdline-tools
  `11076708` with retries, explicit license acceptance and `platform-tools`,
  `platforms;android-34`, `build-tools;34.0.0`.
- Launcher icons (adaptive + monochrome, plus PNG mipmaps for every density) and `gradle.properties`
  were missing and are now part of the project.
- `kotlinx.serialization` annotations were removed from the models so the build no longer depends on a
  plugin that was not configured.

### Owner Edition
- Public binaries contain **no** owner hash: `VerifyOwnerPIN` returns an error and the Owner Panel is
  disabled. Public workflows never receive, print or store the PIN.
- Private owner workflows inject only PBKDF2-SHA256 (120 000 iterations, 32-byte key, random 32-byte
  build-time salt) from the `MIYU_OWNER_PIN` secret, upload a 7-day private artifact and create no
  release.
- Android owner builds now inject the same hash through `BuildConfig` fields and gate the panel on
  18+ **and** a private build.
- Go tests assert: correct PIN unlocks, wrong PIN does not, public build refuses everything.

### Workflows
- Windows, Linux, Android, macOS, iOS validation, private owner Windows/Android and a new **Public
  Tests** workflow (npm ci → build → portable → Playwright → Go tests on Windows and Ubuntu → owner
  PIN handling → repository hygiene).
- Every public workflow refuses to publish when verification fails. No release is created without
  assets, and no release ever contains a stub.

## Honest limitations (also listed in `TESTING.md`)
- The Android overlay, Quick Settings tile, notification actions, share target and PiP behaviour are
  compiled and gate-verified in CI, but **not** exercised on a physical device by CI. Manual smoke
  testing on a real phone is still required, and the vendor-specific battery settings differ per OEM.
- The macOS build is ad-hoc signed: no Apple Developer certificate is used for public builds.
- iOS ships no `.ipa` unless Apple signing secrets are configured — this is deliberate.
- Windows and Linux releases are built on GitHub-hosted runners, which is the closest thing to a real
  machine CI can offer; interactive behaviours (window positioning, tray, hotkeys) are not click-tested
  by CI.
- macOS always-on-top is requested but not guaranteed; Miyu reports the real capability instead of
  pretending.

## Requirements
- Windows 10/11 with the Edge WebView2 Runtime (preinstalled on nearly all machines).
- Linux with GTK 3 and WebKitGTK 4.0 **or** 4.1.
- Android 8.0+ (API 26).
- macOS 11+.
- Free local AI is optional: Ollama or any OpenAI-compatible endpoint on localhost or HTTPS. The API
  key is session-only and never written to disk.

## Privacy and safety (unchanged, and enforced by tests)
No reading of other applications, no keystroke logging, no password collection, no screen monitoring,
no auto-clicking inside other apps, no background microphone or camera, nothing hidden without a
notification. Safety limits can never be switched off: no sexual content for minors, no self-harm
encouragement, no dangerous or illegal instructions, no surveillance or theft, and Miyu never claims
to be human. The Vodafone Cash support card (01027653109) only copies the number or opens the dialer —
there is no payment API and no wallet PIN, ever.
