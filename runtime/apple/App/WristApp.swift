import SwiftUI
import WatchKit

/// The app file (app.json) is copied into the bundle at build time; this one runtime draws any app.
struct Spec: Decodable {
    let name: String
    let color: String
    let blocks: [Block]

    static func load() -> Spec {
        guard let url = Bundle.main.url(forResource: "app", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let spec = try? JSONDecoder().decode(Spec.self, from: data)
        else {
            return Spec(name: "WristBlocks", color: "#F2C46D",
                        blocks: [Block(type: "text", text: "app.json missing", label: nil, goal: nil, step: nil, minutes: nil, gesture: nil)])
        }
        return spec
    }
}

struct Block: Decodable {
    let type: String
    let text: String?
    let label: String?
    let goal: Int?
    let step: Int?
    let minutes: Int?
    let gesture: Bool?
}

extension Color {
    init(hex: String) {
        let v = UInt64(hex.dropFirst(), radix: 16) ?? 0xF2C46D
        self.init(red: Double((v >> 16) & 0xFF) / 255, green: Double((v >> 8) & 0xFF) / 255, blue: Double(v & 0xFF) / 255)
    }
}

@main
struct WristApp: App {
    private let spec = Spec.load()

    var body: some Scene {
        WindowGroup {
            ScrollView {
                VStack(spacing: 10) {
                    ForEach(Array(spec.blocks.enumerated()), id: \.offset) { index, block in
                        BlockView(index: index, block: block)
                    }
                }
                .padding(.horizontal, 4)
            }
            .tint(Color(hex: spec.color))
        }
    }
}

struct BlockView: View {
    let index: Int
    let block: Block

    var body: some View {
        switch block.type {
        case "title":
            Text(block.text ?? "").font(.headline).foregroundStyle(.tint)
        case "counter":
            CounterBlock(key: "counter.\(index)", label: block.label ?? "Counter", goal: block.goal ?? 0,
                         step: block.step ?? 1, gesture: block.gesture ?? false)
        case "timer":
            TimerBlock(label: block.label ?? "Timer", minutes: block.minutes ?? 25, gesture: block.gesture ?? false)
        default:
            Text(block.text ?? "").font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
        }
    }
}

struct CounterBlock: View {
    let label: String
    let goal: Int
    let step: Int
    let gesture: Bool
    @AppStorage private var count: Int

    init(key: String, label: String, goal: Int, step: Int, gesture: Bool) {
        self.label = label
        self.goal = goal
        self.step = step
        self.gesture = gesture
        _count = AppStorage(wrappedValue: 0, key)
    }

    var body: some View {
        VStack(spacing: 4) {
            Text(label).font(.caption2).foregroundStyle(.secondary)
            if goal > 0 {
                Gauge(value: Double(count % goal), in: 0...Double(goal)) {
                    EmptyView()
                } currentValueLabel: {
                    Text("\(count % goal)").font(.title3.monospacedDigit())
                }
                .gaugeStyle(.accessoryCircularCapacity)
                if count >= goal {
                    Text("Rounds: \(count / goal)").font(.caption2).foregroundStyle(.secondary)
                }
            } else {
                Text("\(count)").font(.system(size: 40, weight: .semibold, design: .rounded)).monospacedDigit()
            }
            HStack {
                Button("+\(step)") {
                    count += step
                    let hitGoal = goal > 0 && count % goal < step
                    WKInterfaceDevice.current().play(hitGoal ? .success : .click)
                }
                .modifier(PrimaryGesture(enabled: gesture))
                Button {
                    count = 0
                } label: {
                    Image(systemName: "arrow.counterclockwise")
                }
                .accessibilityLabel("Reset \(label)")
                .frame(width: 44)
            }
        }
    }
}

struct TimerBlock: View {
    let label: String
    let minutes: Int
    let gesture: Bool
    @State private var endDate: Date?
    @State private var pausedLeft: TimeInterval?

    private var total: TimeInterval { TimeInterval(minutes * 60) }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let left = remaining(at: context.date)
            VStack(spacing: 4) {
                Text(label).font(.caption2).foregroundStyle(.secondary)
                Text(Duration.seconds(left).formatted(.time(pattern: .minuteSecond)))
                    .font(.system(size: 34, weight: .semibold, design: .rounded)).monospacedDigit()
                HStack {
                    Button(endDate != nil ? "Pause" : (pausedLeft != nil ? "Resume" : "Start")) { toggle(at: context.date) }
                        .modifier(PrimaryGesture(enabled: gesture))
                    Button {
                        endDate = nil
                        pausedLeft = nil
                    } label: {
                        Image(systemName: "arrow.counterclockwise")
                    }
                    .accessibilityLabel("Reset \(label)")
                    .frame(width: 44)
                }
            }
            .onChange(of: left == 0 && endDate != nil) { _, finished in
                if finished {
                    WKInterfaceDevice.current().play(.notification)
                    endDate = nil
                    pausedLeft = nil
                }
            }
        }
    }

    private func remaining(at now: Date) -> TimeInterval {
        if let endDate { return max(0, endDate.timeIntervalSince(now).rounded(.up)) }
        return pausedLeft ?? total
    }

    private func toggle(at now: Date) {
        if let end = endDate {
            pausedLeft = max(0, end.timeIntervalSince(now))
            endDate = nil
        } else {
            endDate = now.addingTimeInterval(pausedLeft ?? total)
            pausedLeft = nil
        }
        WKInterfaceDevice.current().play(.click)
    }
}

/// Maps the Apple Watch double tap gesture to this button (watchOS 11 and newer).
struct PrimaryGesture: ViewModifier {
    let enabled: Bool

    func body(content: Content) -> some View {
        if #available(watchOS 11.0, *), enabled {
            content.handGestureShortcut(.primaryAction)
        } else {
            content
        }
    }
}
