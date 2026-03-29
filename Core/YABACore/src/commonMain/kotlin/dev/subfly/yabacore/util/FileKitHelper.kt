package dev.subfly.yabacore.util

/**
 * Platform-specific FileKit initialization.
 *
 * Call [init] once at app startup, typically from [CoreRuntime.initialize].
 *
 * - Android: Initializes FileKit core and dialogs with the given Context/Activity.
 * - JVM: Initializes with app ID.
 * - Apple (iOS): No-op; FileKit works without explicit init.
 */
expect object FileKitHelper {
    /**
     * Initializes FileKit for the current platform.
     *
     * @param platformContext Android: [android.content.Context] or [androidx.activity.ComponentActivity].
     *                        JVM: Ignored (uses app ID).
     *                        Apple: Ignored.
     */
    fun init(platformContext: Any?)
}
