import SwiftUI
import sharedUI

@main
struct iOSApp: App {
    init() {
        IosKoinKt.startRentManKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
