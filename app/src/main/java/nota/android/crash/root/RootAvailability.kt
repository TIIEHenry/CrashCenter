package nota.android.crash.root

enum class RootAvailability {
    AVAILABLE,
    DENIED,      // su exists but permission denied
    UNAVAILABLE  // no su binary
}
