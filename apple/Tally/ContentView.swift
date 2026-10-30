import SwiftUI
import WatchKit

/// Smallest useful watch app: a counter you can tap (or double tap on watchOS 11+).
struct ContentView: View {
    @AppStorage("count") private var count = 0

    var body: some View {
        VStack(spacing: 8) {
            Text("\(count)")
                .font(.system(size: 56, weight: .semibold, design: .rounded))
                .contentTransition(.numericText())
            Button("+1") {
                count += 1
                WKInterfaceDevice.current().play(.click)
            }
            .tint(.orange)
            .modifier(PrimaryGesture())
            Button("Reset", role: .destructive) { count = 0 }
                .font(.footnote)
        }
    }
}

/// Maps the Apple Watch double tap gesture to the +1 button where the OS supports it.
private struct PrimaryGesture: ViewModifier {
    func body(content: Content) -> some View {
        if #available(watchOS 11.0, *) {
            content.handGestureShortcut(.primaryAction)
        } else {
            content
        }
    }
}
