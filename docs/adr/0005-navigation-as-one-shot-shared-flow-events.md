# Navigation is a one-shot SharedFlow, state is a StateFlow

Each screen exposes one `StateFlow<UiState>` and one `SharedFlow<NavEvent>` with no replay, and the ViewModel never touches the back stack. An event emitted while nothing is collecting can be dropped, for example while the app is in the background. We accept that because navigation is triggered by user taps on a visible screen, and a replayed event would navigate again after rotation or return.
