import Foundation
import SwiftUI
import ActivityKit
import Combine

enum ToyType: String, CaseIterable, Identifiable {
    case car = "car"
    case ball = "ball"
    case teddy = "teddy"
    case book = "book"
    case rocket = "rocket"
    case flower = "flower"
    case puzzle = "puzzle"
    case balloon = "balloon"
    case musical = "musical"
    
    var id: String { rawValue }
    var emoji: String {
        switch self {
        case .car: return "🚗"
        case .ball: return "⚽"
        case .teddy: return "🧸"
        case .book: return "📚"
        case .rocket: return "🚀"
        case .flower: return "🌸"
        case .puzzle: return "🧩"
        case .balloon: return "🎈"
        case .musical: return "🎵"
        }
    }
    var labelAr: String {
        switch self {
        case .car: return "عربية لعبة"
        case .ball: return "كرة"
        case .teddy: return "دبدوب"
        case .book: return "كتاب"
        case .rocket: return "صاروخ"
        case .flower: return "وردة"
        case .puzzle: return "بازل"
        case .balloon: return "بالونة"
        case .musical: return "لعبة موسيقية"
        }
    }
    var labelEn: String {
        switch self {
        case .car: return "Toy car"
        case .ball: return "Ball"
        case .teddy: return "Teddy bear"
        case .book: return "Story book"
        case .rocket: return "Rocket"
        case .flower: return "Flower"
        case .puzzle: return "Puzzle"
        case .balloon: return "Balloon"
        case .musical: return "Musical toy"
        }
    }
    var keywords: [String] {
        switch self {
        case .car: return ["car", "سيارة", "عربية"]
        case .ball: return ["ball", "كرة"]
        case .teddy: return ["teddy", "bear", "دبدوب"]
        case .book: return ["book", "كتاب"]
        case .rocket: return ["rocket", "صاروخ"]
        case .flower: return ["flower", "وردة"]
        case .puzzle: return ["puzzle", "بازل"]
        case .balloon: return ["balloon", "بالونة"]
        case .musical: return ["music", "موسيقى"]
        }
    }
    
    static func fromInput(_ input: String) -> ToyType? {
        let lower = input.lowercased()
        return allCases.first { toy in
            toy.keywords.contains { lower.contains($0) }
        }
    }
}

enum AgeProfile: Int, CaseIterable, Identifiable {
    case child3_5 = 4
    case child6_8 = 7
    case child9_12 = 10
    case teen13_17 = 15
    case adult = 18
    
    var id: Int { rawValue }
    var displayAr: String {
        switch self {
        case .child3_5: return "٣-٥ سنوات"
        case .child6_8: return "٦-٨ سنوات"
        case .child9_12: return "٩-١٢ سنة"
        case .teen13_17: return "١٣-١٧ سنة"
        case .adult: return "١٨+"
        }
    }
}

struct MiyuFocusAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        var minutesLeft: Int
        var task: String
        var mood: String
    }
    var taskName: String
}

class MiyuViewModel: ObservableObject {
    @Published var currentToy: ToyType? = nil
    @Published var age: AgeProfile? = nil
    @Published var language: String = "ar"
    @Published var respectMeter: Int = 100
    @Published var mood: String = "cozy"
    @Published var isOwnerBuild: Bool = false
    @Published var focusMinutes: Int = 25
    @Published var isFocusRunning: Bool = false
    
    @Published var toyInput: String = ""
    
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        loadState()
    }
    
    func loadState() {
        if let data = UserDefaults.standard.data(forKey: "miyu_state"),
           let decoded = try? JSONDecoder().decode([String: String].self, from: data) {
            if let toyKey = decoded["toy"] {
                currentToy = ToyType(rawValue: toyKey)
            }
            language = decoded["language"] ?? "ar"
        }
    }
    
    func saveState() {
        var dict: [String: String] = [:]
        dict["toy"] = currentToy?.rawValue
        dict["language"] = language
        if let data = try? JSONEncoder().encode(dict) {
            UserDefaults.standard.set(data, forKey: "miyu_state")
        }
    }
    
    func handleToyInput(_ input: String) {
        if let toy = ToyType.fromInput(input) {
            currentToy = toy
            mood = "playful"
            saveState()
        }
    }
    
    func startLiveActivity() {
        if #available(iOS 16.1, *) {
            let attributes = MiyuFocusAttributes(taskName: "Focus Session")
            let contentState = MiyuFocusAttributes.ContentState(minutesLeft: focusMinutes, task: "قراءة", mood: mood)
            
            do {
                let activity = try Activity<MiyuFocusAttributes>.request(
                    attributes: attributes,
                    contentState: contentState,
                    pushType: nil
                )
                print("Live Activity started: \(activity.id)")
                isFocusRunning = true
            } catch {
                print("Failed to start Live Activity: \(error)")
            }
        }
    }
    
    func stopLiveActivity() {
        if #available(iOS 16.1, *) {
            Task {
                for activity in Activity<MiyuFocusAttributes>.activities {
                    await activity.end(dismissalPolicy: .immediate)
                }
                await MainActor.run {
                    isFocusRunning = false
                }
            }
        }
    }
}

struct PlayLabView: View {
    @ObservedObject var viewModel: MiyuViewModel
    
    var body: some View {
        CardView(title: "Miyu Play Lab 🎮") {
            VStack(alignment: .leading, spacing: 10) {
                Text("قولي: امسكي عربية لعبة أو Hold a toy car")
                    .font(.caption)
                
                TextField("ماذا تمسك Miyu؟", text: $viewModel.toyInput)
                    .textFieldStyle(.roundedBorder)
                
                Button("اعرض اللعبة") {
                    viewModel.handleToyInput(viewModel.toyInput)
                }.buttonStyle(.borderedProminent)
                
                if let toy = viewModel.currentToy {
                    HStack {
                        Text(toy.emoji).font(.largeTitle)
                        VStack(alignment: .leading) {
                            Text(toy.labelAr).bold()
                            Text(toy.labelEn).font(.caption)
                            Text("Miyu ماسكة \(toy.labelAr) — حركة مرحة!").font(.caption2)
                        }
                    }
                    .padding()
                    .background(Color.pink.opacity(0.1))
                    .cornerRadius(8)
                }
                
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(ToyType.allCases) { toy in
                            Button(toy.emoji) {
                                viewModel.currentToy = toy
                                viewModel.saveState()
                            }
                            .buttonStyle(.bordered)
                        }
                    }
                }
            }
        }
    }
}

struct AgeProfileView: View {
    @ObservedObject var viewModel: MiyuViewModel
    
    var body: some View {
        CardView(title: "العمر واللغة") {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    ForEach(AgeProfile.allCases) { profile in
                        Button("\(profile.rawValue)") {
                            viewModel.age = profile
                        }
                        .buttonStyle(viewModel.age == profile ? .borderedProminent : .bordered)
                    }
                }
                HStack {
                    Button("العربية") { viewModel.language = "ar" }
                        .buttonStyle(viewModel.language == "ar" ? .borderedProminent : .bordered)
                    Button("English") { viewModel.language = "en" }
                        .buttonStyle(viewModel.language == "en" ? .borderedProminent : .bordered)
                }
                Text("Kindness meter: \(viewModel.respectMeter)%")
                    .font(.caption2)
                ProgressView(value: Float(viewModel.respectMeter) / 100.0)
            }
        }
    }
}

struct PiPView: View {
    var body: some View {
        VStack {
            Text("Miyu PiP Mode")
                .font(.headline)
            Text("✨｡◕‿◕｡")
                .font(.largeTitle)
            Text("هذه نافذة PiP الرسمية من iOS")
                .font(.caption)
            Text("تظهر فوق التطبيقات بحدود PiP")
                .font(.caption2)
        }
        .padding()
    }
}
