import SwiftUI

@main
struct MiyuApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    @StateObject private var viewModel = MiyuViewModel()
    @State private var showingPiP = false
    // Text handed over from the Share Sheet extension through the miyu:// URL scheme.
    @State private var sharedText: String?
    @State private var notificationsReady = false
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    // Header
                    VStack {
                        Text("Miyu — a little company")
                            .font(.title2).bold()
                        Text("A little magic ✨")
                            .foregroundColor(.secondary)
                    }
                    
                    // Safety
                    SafetyCard()
                    
                    // iOS Limitation Notice
                    CardView(title: "iOS Companion البديل") {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("iOS لا يسمح بفقاعة حرة فوق كل التطبيقات مثل Android. هذه قيود النظام نفسه.")
                                .font(.caption)
                            Text("البدائل المتاحة:")
                                .font(.caption).bold()
                            ForEach([
                                "• Picture-in-Picture لشاشة Miyu",
                                "• Home Screen Widget",
                                "• Lock Screen Widget",
                                "• Live Activity & Dynamic Island",
                                "• Siri / App Intents",
                                "• Share Sheet",
                                "• Notifications تفاعلية"
                            ], id: \.self) { item in
                                Text(item).font(.caption2)
                            }
                        }
                    }
                    
                    // PiP
                    CardView(title: "Picture-in-Picture") {
                        VStack {
                            Text("Miyu يمكنها الظهور في نافذة PiP الرسمية")
                                .font(.caption)
                            Button("تفعيل PiP") {
                                showingPiP = true
                            }.buttonStyle(.borderedProminent)
                        }
                    }
                    
                    // Play Lab
                    PlayLabView(viewModel: viewModel)
                    
                    // Age Profile
                    AgeProfileView(viewModel: viewModel)
                    
                    // Widgets info
                    CardView(title: "Widgets") {
                        Text("أضيفي ويدجت Miyu إلى الشاشة الرئيسية أو شاشة القفل لرؤية مزاجها وألعابها.")
                            .font(.caption)
                    }
                    
                    // Live Activity
                    CardView(title: "Live Activity & Focus") {
                        VStack {
                            Text("ابدأي جلسة تركيز وستظهر في Dynamic Island")
                                .font(.caption)
                            Button("بدء Focus 25 دقيقة") {
                                viewModel.startLiveActivity()
                            }.buttonStyle(.bordered)
                        }
                    }
                    
                    // Share Sheet (send Miyu state out, receive text from other apps)
                    CardView(title: "Share Sheet") {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("شاركي حالة Miyu، أو أرسلي نصاً إليها من أي تطبيق عبر زر المشاركة.")
                                .font(.caption)
                            ShareLink(item: "Miyu حاليًا: \(viewModel.currentToy?.emoji ?? "🌸") — a little company, a little magic") {
                                Label("مشاركة حالة Miyu", systemImage: "square.and.arrow.up")
                            }
                            if let sharedText {
                                Text("وصل من تطبيق آخر: \(sharedText)")
                                    .font(.caption2)
                            }
                        }
                    }

                    // Interactive notifications
                    CardView(title: "إشعارات تفاعلية") {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Miyu يمكنها تذكيرك بلطف (تركيز، راحة، لعبة) بأزرار داخل الإشعار نفسه. كل شيء محلي على الجهاز.")
                                .font(.caption)
                            Button(notificationsReady ? "الإشعارات مسموحة ✅" : "تفعيل الإشعارات") {
                                MiyuNotifications.registerCategories()
                                MiyuNotifications.requestAuthorizationIfNeeded { granted in
                                    DispatchQueue.main.async { notificationsReady = granted }
                                }
                            }.buttonStyle(.bordered)
                        }
                    }

                    // Support
                    SupportCard()
                }
                .padding()
            }
            .navigationTitle("Miyu")
        }
        .sheet(isPresented: $showingPiP) {
            PiPView()
        }
        .onOpenURL { url in
            // miyu://share?text=... arrives from the Share Sheet extension.
            guard url.scheme == "miyu", url.host == "share" else { return }
            let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
            if let text = components?.queryItems?.first(where: { $0.name == "text" })?.value {
                sharedText = text
                viewModel.handleToyInput(text)
            }
        }
        .task {
            MiyuNotifications.registerCategories()
            MiyuNotifications.requestAuthorizationIfNeeded { granted in
                DispatchQueue.main.async { notificationsReady = granted }
            }
        }
    }
}

struct CardView<Content: View>: View {
    let title: String
    let content: Content
    init(title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title).font(.headline)
            content
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .cornerRadius(12)
    }
}

struct SafetyCard: View {
    var body: some View {
        HStack {
            Image(systemName: "lock.shield")
            VStack(alignment: .leading) {
                Text("Safe Play Mode").font(.caption).bold()
                Text("Miyu لا تقرأ التطبيقات الأخرى").font(.caption2)
            }
            Spacer()
        }
        .padding()
        .background(Color.green.opacity(0.1))
        .cornerRadius(8)
    }
}

struct SupportCard: View {
    var body: some View {
        VStack {
            Text("ادعمي Miyu 💖").bold()
            Text("Vodafone Cash").font(.caption)
            Text("01027653109").font(.title3).bold()
            Text("زر Copy فقط، لا يبدأ تحويل تلقائي").font(.caption2)
        }
        .padding()
        .background(Color.pink.opacity(0.1))
        .cornerRadius(12)
    }
}
