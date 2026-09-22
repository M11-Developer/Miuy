import Foundation
import UserNotifications

/// Interactive notifications: Miyu can ask a gentle question and the child/teen/parent answers from
/// the notification itself. This is one of the few ways iOS lets a companion app act outside the
/// foreground, and unlike an overlay it is always visible and always user-initiated.
enum MiyuNotifications {

    static let focusCategory = "MIYU_FOCUS"
    static let toyCategory = "MIYU_TOY"
    static let waterCategory = "MIYU_BREAK"

    static func requestAuthorizationIfNeeded(completion: ((Bool) -> Void)? = nil) {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
            completion?(granted)
        }
    }

    static func registerCategories() {
        let startFocus = UNNotificationAction(
            identifier: "MIYU_START_FOCUS",
            title: "ابدأي التركيز · Start focus",
            options: [.foreground]
        )
        let later = UNNotificationAction(
            identifier: "MIYU_LATER",
            title: "بعدين · Later",
            options: []
        )
        let playCar = UNNotificationAction(
            identifier: "MIYU_PLAY_CAR",
            title: "امسكي عربية لعبة",
            options: [.foreground]
        )
        let breathe = UNNotificationAction(
            identifier: "MIYU_BREATHE",
            title: "نفس عميق · Breathe",
            options: [.foreground]
        )

        let categories: Set<UNNotificationCategory> = [
            UNNotificationCategory(identifier: focusCategory, actions: [startFocus, later], intentIdentifiers: [], options: []),
            UNNotificationCategory(identifier: toyCategory, actions: [playCar, later], intentIdentifiers: [], options: []),
            UNNotificationCategory(identifier: waterCategory, actions: [breathe, later], intentIdentifiers: [], options: [])
        ]
        UNUserNotificationCenter.current().setNotificationCategories(categories)
    }

    /// Schedules a local notification. Never remote, never tracking: nothing leaves the device.
    static func schedule(after seconds: TimeInterval, title: String, body: String, category: String) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.categoryIdentifier = category
        content.sound = nil
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(5, seconds), repeats: false)
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }
}
