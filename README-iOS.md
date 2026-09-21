# Miyu iOS — Companion Alternatives

## Important: iOS System Limitation

**iOS does NOT allow a free-floating character above all apps like Android.** This is an iOS system restriction, not a Miyu limitation.

- Android: Can use SYSTEM_ALERT_WINDOW to show bubble over other apps (with permission)
- iOS: Cannot. No public API for free overlay over all apps.

Miyu does NOT claim iOS supports free overlay. That would be false.

## Real Alternatives on iOS

Miyu iOS provides genuine alternatives within iOS rules:

### 1. Picture-in-Picture (PiP)
- Miyu screen can appear in small PiP window while using other apps
- Uses AVKit PiP, official iOS PiP behavior
- User can move PiP, resize, close
- Not free drawing anywhere, but system PiP

### 2. Home Screen Widget
- WidgetKit
- Shows Miyu mood, current toy, respect meter
- Small, medium sizes
- Updates via timeline

### 3. Lock Screen Widget
- Accessory circular/rectangular
- Shows mini Miyu status

### 4. Live Activity / Dynamic Island
- ActivityKit
- Focus session shows in Dynamic Island (iPhone 14 Pro+)
- Example: "Focus 25 min - Reading"
- Updates minutes left, task, mood

### 5. Siri / App Intents
- "Open Miyu" → Opens app
- "Start focus with Miyu" → Starts focus session
- "Play with Miyu" / "امسكي عربية لعبة" → Toy interaction
- App Shortcuts: Phrases in English and Arabic
- Works with Shortcuts app

### 6. Share Sheet
- Send text to Miyu via share sheet
- User must explicitly share text
- Example: Share article text to Miyu to discuss
- Permission via user action, not auto-read

### 7. Interactive Notifications
- Notifications with actions
- e.g., "Focus done! 🎉" with "Start new" button

### 8. Shortcuts Integration
- Shortcuts app can call Miyu intents
- Automation possible via Shortcuts (user-controlled)

## Project Structure

```
mobile/ios/
  MiyuCompanion/
    MiyuApp.swift
    MiyuViewModel.swift
    Widgets/MiyuWidget.swift
    Intents/MiyuIntents.swift
```

- SwiftUI for UI
- WidgetKit for widgets
- ActivityKit for Live Activities
- App Intents for Siri/Shortcuts
- AVKit for PiP (future)

## Build

Requires macOS + Xcode 15+ + Apple Developer account for signing.

```bash
open mobile/ios/MiyuCompanion.xcodeproj
# Select team, bundle ID
# Build
```

### TestFlight

If Apple signing secrets available in GitHub Actions (APPLE_CERTIFICATE, APPLE_PROVISIONING_PROFILE, etc.), workflow can build signed archive.

No fake IPA in repo. If no signing credentials, only project validation, not IPA.

Workflow `ios` (future) would use `macos-latest`, `actions/setup-xcode`, `apple-actions/import-codesign-certs`, `xcodebuild archive`.

## Play Lab (iOS)

Same shared core as Android/Desktop:
- Toy detection Arabic/English
- 9 base toys
- Local animation, not image generation

## Age Profiles & Safety

Same as Android/Desktop, local storage via UserDefaults, no iCloud backup for sensitive.

## Vodafone Cash Support

Same card, 01027653109, Copy button, no auto-transfer.

## Difference Table

| Feature | Android | iOS |
|---------|---------|-----|
| Free bubble over all apps | ✅ Yes, with SYSTEM_ALERT_WINDOW permission | ❌ No, iOS doesn't allow |
| PiP | ✅ (via overlay or system) | ✅ Official PiP |
| Widgets | ✅ | ✅ |
| Live Activity / Dynamic Island | ❌ (different) | ✅ |
| Foreground Service | ✅ | ❌ (Background Modes limited) |
| Open other apps | ✅ Intent | ✅ URL Schemes / App Intents |
| Read other apps content | ❌ Forbidden | ❌ Forbidden |

## Security

- No reading other apps
- No surveillance
- Share sheet only with user explicit share
- No background mic without permission

## Future

- TestFlight build when secrets available
- App Store submission notes
- Push notifications (opt-in)

## Why no overlay on iOS?

iOS sandbox: Apps cannot draw over other apps freely. Only system-provided PiP, CallKit, etc. Apple restricts for security. Miyu respects this and provides alternatives instead of claiming false capability.
