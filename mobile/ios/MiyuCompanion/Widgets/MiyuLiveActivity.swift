import ActivityKit
import SwiftUI
import WidgetKit

/// Live Activity for focus sessions and toy moments.
///
/// Lock Screen + Dynamic Island are the Apple-approved replacements for a free floating overlay,
/// which iOS does not allow. The activity is started by the app (`MiyuViewModel.startLiveActivity`)
/// and shown here.
@available(iOS 16.1, *)
struct MiyuLiveActivityWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: MiyuFocusAttributes.self) { context in
            // Lock Screen / notification banner presentation
            HStack(spacing: 12) {
                Text(context.attributes.taskName == "Play" ? "🧸" : "🌸")
                    .font(.title2)
                VStack(alignment: .leading, spacing: 2) {
                    Text(context.attributes.taskName)
                        .font(.headline)
                    Text("\(context.state.minutesLeft) min · \(context.state.task)")
                        .font(.caption)
                }
                Spacer()
                Text(context.state.mood)
                    .font(.caption2)
                    .padding(6)
                    .background(Color.pink.opacity(0.15))
                    .clipShape(Capsule())
            }
            .padding()
            .activityBackgroundTint(Color(red: 1.0, green: 0.96, blue: 0.96))
            .activitySystemActionForegroundColor(Color(red: 0.54, green: 0.37, blue: 0.38))
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Text("Miyu").font(.caption).bold()
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text("\(context.state.minutesLeft)m").font(.caption).bold()
                }
                DynamicIslandExpandedRegion(.center) {
                    Text(context.state.task).font(.caption2).lineLimit(1)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    HStack {
                        Text(context.state.mood).font(.caption2)
                        Spacer()
                        Text(context.attributes.taskName).font(.caption2)
                    }
                }
            } compactLeading: {
                Text("🌸")
            } compactTrailing: {
                Text("\(context.state.minutesLeft)m").font(.caption2)
            } minimal: {
                Text("🌸")
            }
            .keylineTint(Color.pink)
        }
    }
}

/// Widget bundle published by the Miyu widget extension.
@main
struct MiyuWidgetsBundle: WidgetBundle {
    var body: some Widget {
        MiyuWidget()
        if #available(iOS 16.1, *) {
            MiyuLiveActivityWidget()
        }
    }
}
