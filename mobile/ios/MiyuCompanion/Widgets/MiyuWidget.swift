import WidgetKit
import SwiftUI

struct MiyuEntry: TimelineEntry {
    let date: Date
    let toy: String
    let mood: String
    let respect: Int
}

struct MiyuProvider: TimelineProvider {
    func placeholder(in context: Context) -> MiyuEntry {
        MiyuEntry(date: Date(), toy: "🚗", mood: "cozy", respect: 100)
    }
    func getSnapshot(in context: Context, completion: @escaping (MiyuEntry) -> Void) {
        completion(MiyuEntry(date: Date(), toy: "🧸", mood: "playful", respect: 95))
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<MiyuEntry>) -> Void) {
        let entry = MiyuEntry(date: Date(), toy: "🌸", mood: "cozy", respect: 100)
        let timeline = Timeline(entries: [entry], policy: .atTimeEnd)
        completion(timeline)
    }
}

struct MiyuWidgetEntryView: View {
    var entry: MiyuProvider.Entry
    var body: some View {
        VStack {
            Text("Miyu").font(.headline)
            Text(entry.toy).font(.largeTitle)
            Text(entry.mood).font(.caption)
            Text("Kindness \(entry.respect)%").font(.caption2)
        }
        .padding()
    }
}

struct MiyuWidget: Widget {
    let kind = "MiyuWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: MiyuProvider()) { entry in
            MiyuWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Miyu Companion")
        .description("See Miyu's mood and current toy")
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryCircular, .accessoryRectangular])
    }
}

struct MiyuWidgetBundle: WidgetBundle {
    var body: some Widget {
        MiyuWidget()
    }
}
