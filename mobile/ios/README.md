# Miyu iOS Companion

iOS لا يسمح بفقاعة حرة فوق كل التطبيقات مثل Android. هذا قيد من نظام iOS نفسه.

## البدائل الحقيقية على iOS:

1. **Picture-in-Picture (PiP)**: Miyu تظهر في نافذة عائمة صغيرة فوق التطبيقات أثناء استخدامها، لكن بقيود PiP الرسمية.
2. **Home Screen Widget**: ويدجت يعرض حالة Miyu، المزاج، واللعبة الحالية.
3. **Lock Screen Widget**: ويدجت شاشة القفل.
4. **Live Activity / Dynamic Island**: نشاط مباشر يعرض جلسة التركيز أو تفاعل Miyu.
5. **Siri / App Intents**: "افتحي Miyu"، "شغلي وقت التركيز".
6. **Share Sheet**: إرسال نص إلى Miyu بإذن المستخدم.
7. **Interactive Notifications**: إشعارات تفاعلية.
8. **Shortcuts Integration**: تكامل مع تطبيق Shortcuts.

## المشروع:

- SwiftUI
- App Intents
- WidgetKit
- ActivityKit for Live Activities
- AVKit for PiP

## البناء:

يتطلب macOS مع Xcode و Apple Developer account للتوقيع.

```bash
open MiyuCompanion.xcodeproj
```

TestFlight يحتاج Apple signing secrets.

لا يوجد IPA وهمي في هذا المستودع. إذا لم تتوفر Apple signing credentials، يتم بناء المشروع فقط والتحقق منه.

## الفرق بين Android و iOS:

| الميزة | Android | iOS |
|--------|---------|-----|
| فقاعة حرة فوق كل التطبيقات | ✅ نعم، بإذن SYSTEM_ALERT_WINDOW | ❌ لا، النظام لا يسمح |
| PiP | ✅ | ✅ بديل رسمي |
| Widgets | ✅ | ✅ |
| Live Activity / Dynamic Island | ❌ (بدائل مختلفة) | ✅ |
| Foreground Service | ✅ | ❌ (Background Modes محدودة) |
| فتح تطبيقات أخرى | ✅ عبر Intent | ✅ عبر URL Schemes / App Intents |
| قراءة محتوى التطبيقات | ❌ ممنوع | ❌ ممنوع |

Miyu تحترم حدود كل نظام ولا تدعي إمكانيات غير موجودة.
