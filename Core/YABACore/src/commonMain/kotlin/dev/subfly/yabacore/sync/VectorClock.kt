package dev.subfly.yabacore.sync

import kotlinx.serialization.Serializable

/**
 * A vector clock for tracking causality across multiple devices.
 *
 * Each device maintains its own counter. The clock maps device IDs to their
 * respective sequence numbers. Vector clocks enable:
 * - Determining if one event happened before another
 * - Detecting concurrent (conflicting) events
 * - Merging clocks during sync
 *
 * @property clocks Map of deviceId to sequence number
 */
@Serializable
data class VectorClock(
    val clocks: Map<String, Long> = emptyMap(),
) {
    /**
     * Increments the counter for the given device and returns a new clock.
     */
    fun increment(deviceId: String): VectorClock {
        val currentValue = clocks[deviceId] ?: 0L
        return VectorClock(clocks + (deviceId to currentValue + 1))
    }

    /**
     * Merges this clock with another, taking the maximum value for each device.
     */
    fun merge(other: VectorClock): VectorClock {
        val allDevices = clocks.keys + other.clocks.keys
        val merged = allDevices.associateWith { deviceId ->
            maxOf(clocks[deviceId] ?: 0L, other.clocks[deviceId] ?: 0L)
        }
        return VectorClock(merged)
    }

    /**
     * Returns true if this clock is strictly newer than the other clock.
     *
     * A clock A is newer than B if:
     * - For all devices, A[device] >= B[device]
     * - For at least one device, A[device] > B[device]
     */
    fun isNewerThan(other: VectorClock): Boolean {
        val allDevices = clocks.keys + other.clocks.keys
        var hasGreater = false

        for (deviceId in allDevices) {
            val thisValue = clocks[deviceId] ?: 0L
            val otherValue = other.clocks[deviceId] ?: 0L

            if (thisValue < otherValue) {
                return false
            }
            if (thisValue > otherValue) {
                hasGreater = true
            }
        }

        return hasGreater
    }

    /**
     * Returns true if this clock and the other are concurrent (neither is newer).
     *
     * Concurrent clocks indicate conflicting edits that need resolution.
     */
    fun isConcurrentWith(other: VectorClock): Boolean {
        return !isNewerThan(other) && !other.isNewerThan(this) && this != other
    }

    /**
     * Returns true if this clock is newer than or equal to the other clock.
     */
    fun isNewerOrEqual(other: VectorClock): Boolean {
        val allDevices = clocks.keys + other.clocks.keys
        return allDevices.all { deviceId ->
            (clocks[deviceId] ?: 0L) >= (other.clocks[deviceId] ?: 0L)
        }
    }

    /**
     * Gets the sequence number for a specific device.
     */
    fun getSeq(deviceId: String): Long = clocks[deviceId] ?: 0L

    /**
     * Returns true if this clock is empty (no device has made any changes).
     */
    fun isEmpty(): Boolean = clocks.isEmpty() || clocks.values.all { it == 0L }

    /**
     * Converts this VectorClock to a plain Map for JSON serialization.
     */
    fun toMap(): Map<String, Long> = clocks

    companion object {
        /**
         * Creates a VectorClock from a plain Map.
         */
        fun fromMap(map: Map<String, Long>): VectorClock = VectorClock(map)

        /**
         * Creates an empty VectorClock.
         */
        fun empty(): VectorClock = VectorClock()

        /**
         * Creates a VectorClock with a single device at the given sequence.
         */
        fun of(deviceId: String, seq: Long): VectorClock = VectorClock(mapOf(deviceId to seq))

        /**
         * Deterministic tie-breaker for concurrent clocks.
         *
         * When two clocks are concurrent, we need a deterministic way to choose
         * a "winner". This uses lexicographic comparison of the device IDs
         * with the highest sequence numbers.
         *
         * @return negative if a wins, positive if b wins, 0 if equal
         */
        fun deterministicCompare(a: VectorClock, b: VectorClock): Int {
            // Sum of all sequences as a quick comparison
            val sumA = a.clocks.values.sum()
            val sumB = b.clocks.values.sum()
            if (sumA != sumB) return sumA.compareTo(sumB)

            // If sums are equal, compare by the highest device ID alphabetically
            val maxDeviceA = a.clocks.maxByOrNull { it.value }?.key ?: ""
            val maxDeviceB = b.clocks.maxByOrNull { it.value }?.key ?: ""
            return maxDeviceA.compareTo(maxDeviceB)
        }
    }
}
