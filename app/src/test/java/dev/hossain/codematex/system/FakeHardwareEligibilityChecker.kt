package dev.hossain.codematex.system

/**
 * Fake implementation of [HardwareEligibilityChecker] for unit testing.
 */
class FakeHardwareEligibilityChecker(
    var result: HardwareEligibility = HardwareEligibility.Eligible,
) : HardwareEligibilityChecker {
    override fun checkEligibility(): HardwareEligibility = result
}
