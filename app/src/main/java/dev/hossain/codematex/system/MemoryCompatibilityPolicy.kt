package dev.hossain.codematex.system

/**
 * Shared byte-based policy for RAM compatibility decisions.
 *
 * Model requirements are expressed as marketed gigabytes (decimal, 1 GB = 1_000_000_000 bytes).
 * Devices typically report less physical memory than the marketed amount because the kernel,
 * baseband, and other reserved blocks consume a portion before the OS boots. The policy applies a
 * fixed reservation allowance so that an N-GB model is usable when roughly 90 % of N GB is
 * visible to the OS.
 */
object MemoryCompatibilityPolicy {
    /**
     * Number of bytes in a marketed (decimal) gigabyte.
     */
    const val BYTES_PER_GB: Long = 1_000_000_000L

    /**
     * Reserved-memory allowance. A device is considered compatible with an N-GB model when it
     * reports at least N * 0.9 GB of total RAM.
     */
    private const val ALLOWANCE_NUMERATOR = 9L
    private const val ALLOWANCE_DENOMINATOR = 10L

    /**
     * Minimum total memory bytes required for a model marketed at [marketedMemoryGb].
     */
    fun minimumRequiredBytes(marketedMemoryGb: Int): Long = marketedMemoryGb * BYTES_PER_GB * ALLOWANCE_NUMERATOR / ALLOWANCE_DENOMINATOR

    /**
     * Returns true when [totalMemoryBytes] satisfies the requirement for [modelMinRamGb],
     * or when the model has no explicit requirement.
     */
    fun isCompatible(
        modelMinRamGb: Int,
        totalMemoryBytes: Long,
    ): Boolean = modelMinRamGb == 0 || totalMemoryBytes >= minimumRequiredBytes(modelMinRamGb)

    /**
     * Converts [bytes] to decimal gigabytes for display.
     */
    fun toDecimalGigabytes(bytes: Long): Double = bytes / BYTES_PER_GB.toDouble()
}
