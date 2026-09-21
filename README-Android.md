# Miyu Android — Floating Companion

## Installation

- Miyu-Android.apk (test, sideload)
- Miyu-Android.aab (Play Store)

Enable "Install unknown apps" for APK sideload.

## Floating Companion — Key Feature

### Permission Request

1. Open Miyu app
2. Tap "طلب إذن الظهور فوق التطبيقات"
3. System shows security disclosure:
   - Miyu uses overlay ONLY to show character bubble
   - Does NOT read other apps' content
   - Does NOT record keystrokes/passwords
   - Does NOT monitor screen
   - Does NOT auto-click in apps
   - Does NOT work hidden
4. Tap "فتح الإعدادات" → Enable "Allow display over other apps" for Miyu
5. Return to app, tap "إظهار الفقاعة"

Overlay is NEVER enabled automatically without user consent.

### Bubble Features

- **Drag**: Touch and drag bubble anywhere
- **Resize**: Settings slider 50% to 150%
- **Transparency**: 30% to 100%
- **Snap to edges**: When enabled, bubble snaps to screen edge on release, with animation
- **Hide**: Hide bubble temporarily, notification remains to re-show
- **Mini Mode**: Smaller bubble, only character face
- **Click-through Mode**: When enabled, touches pass through bubble to app below (useful when bubble covers buttons)
- **Stop motion**: Disable breathing animation
- **Open controls**: Tap bubble to open control panel

### Safe Area & Display

- Respects Safe Area, Cutouts, Notches (WindowManager FLAG_LAYOUT_NO_LIMITS but with insets handling)
- Portrait and landscape supported
- Saves position locally (DataStore)
- Snap to edges with DecelerateInterpolator animation
- Idle breathing (sin wave scale), blink (every 3-5s), speech mouth movement (when speaking)
- When talking, bubble pulses
- When touched, reacts (pat)

### Toy Overlay

When you say:
- "امسكي عربية لعبة"
- "Hold a toy car"

- Toy overlay appears next to bubble with emoji (🚗, ⚽, 🧸, etc.)
- Animation: float/bounce/wiggle
- Character expression changes
- Speech bubble: "شوفي! أنا ماسكة عربية لعبة 🎈"
- Saved in Play Lab history

### Foreground Service

When floating enabled:
- Persistent notification: "Miyu is floating ✨"
- Shows: Tap to open controls, Drag to move
- Buttons: Stop, Hide, Controls
- Cannot be dismissed while service running
- Must have stop button

Respects Battery Saver: Uses WorkManager low importance, not wake lock heavy.

### Microphone & Camera

- Microphone: Only when user taps Mic button, shows consent dialog "may send audio to speech service online"
- No background mic without explicit permission + notification
- Camera: Never auto-started, opt-in only, local preview, never sent
- When overlay hidden, mic can be set to stop (user choice)

### No Surveillance

**FORBIDDEN and NOT implemented:**
- Reading other apps' content via overlay or accessibility
- Keylogging, password stealing
- Screen monitoring
- Auto-clicking inside other apps
- Bypassing app protections
- Hidden operation

If you see any app claiming Miyu does this, it's fake.

### Opening Other Apps

"الدخول إلى تطبيقات أخرى" means:
- Open app via Android Intent (e.g., YouTube, Chrome, Focus)
- Example: "افتحي يوتيوب" → Intent to com.google.android.youtube or https://youtube.com
- Does NOT control app content, does NOT read data, does NOT do hidden automation

### Play Lab

- Arabic and English toy detection
- 9 base toys: Car, Ball, Teddy, Book, Rocket, Flower, Puzzle, Balloon, Musical
- Local sprite/emoji, not claimed as image generation model
- Mood changes, speech bubble

### Age Profiles

- 3-5, 6-8, 9-12, 13-17, 18+
- Parent/guardian confirmation
- Local-only storage
- Respect Meter, Mood System

### Vodafone Cash Support

- Card shows 01027653109
- Copy button, Open Phone button
- No auto-transfer, no Wallet PIN, no payment data in logs
- Children need guardian permission

### Build

```bash
cd mobile/android
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew bundleRelease
# AAB at app/build/outputs/bundle/release/
```

Requires Android SDK, Java 17.

### Testing

- Overlay permission test: grant/revoke, bubble shows only when granted
- Drag/resize: drag to edges, snap animation, resize slider
- Hide/show: hide bubble, notification stays, re-show
- App launch Intent: YouTube, Chrome
- Mic permission: request, deny, consent dialog
- Camera remains off: check no camera usage when not enabled
- Toy commands Arabic/English

### iOS Difference

Android allows free floating bubble. iOS does NOT (system limitation). iOS alternatives: PiP, Widgets, Live Activities, Dynamic Island.

Documented clearly, no false claims.

### Security

- No API keys in logs
- EncryptedSharedPreferences for secrets (future)
- DataStore with no backup (excluded)
- Foreground service type specialUse with disclosure

### SHA256

Check SHA256SUMS-Android.txt
