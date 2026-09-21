import AppIntents
import Foundation

struct OpenMiyuIntent: AppIntent {
    static var title: LocalizedStringResource = "Open Miyu"
    static var description = IntentDescription("Open Miyu companion")
    static var openAppWhenRun = true
    
    func perform() async throws -> some IntentResult {
        return .result()
    }
}

struct StartFocusIntent: AppIntent {
    static var title: LocalizedStringResource = "Start Focus Session"
    static var description = IntentDescription("Start a focus session with Miyu")
    
    @Parameter(title: "Minutes", default: 25)
    var minutes: Int
    
    @Parameter(title: "Task")
    var task: String?
    
    static var parameterSummary: some ParameterSummary {
        Summary("Start focus for \(\.$minutes) minutes") {
            \.$task
        }
    }
    
    func perform() async throws -> some IntentResult & ProvidesDialog {
        return .result(dialog: "Starting focus for \(minutes) minutes. Miyu is here with you ✨")
    }
}

struct PlayToyIntent: AppIntent {
    static var title: LocalizedStringResource = "Play with Toy"
    static var description = IntentDescription("Ask Miyu to hold a toy")
    
    @Parameter(title: "Toy", default: "car")
    var toy: String
    
    func perform() async throws -> some IntentResult & ProvidesDialog {
        let toyMap: [String: String] = [
            "car": "🚗 عربية لعبة",
            "ball": "⚽ كرة",
            "teddy": "🧸 دبدوب",
            "book": "📚 كتاب"
        ]
        let display = toyMap[toy] ?? "🎈 لعبة"
        return .result(dialog: "Miyu is holding \(display) — wiggle wiggle! 🎈")
    }
}

struct MiyuShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: OpenMiyuIntent(),
            phrases: ["Open Miyu", "افتحي ميو", "كلميني يا ميو"]
        )
        AppShortcut(
            intent: StartFocusIntent(),
            phrases: ["Start focus with Miyu", "شغلي وقت التركيز", "عايز أذاكر"]
        )
        AppShortcut(
            intent: PlayToyIntent(),
            phrases: ["Play with Miyu", "امسكي عربية لعبة", "امسكي دبدوب"]
        )
    }
}
