package limitedwip.limbo

import limitedwip.limbo.Limbo.Amount.Companion.zero
import limitedwip.limbo.components.Ide

class Limbo(private val ide: Ide, private var settings: Settings) {
    private var amountOfTestsRun = zero
    private var allowOneCommitWithoutChecks = false

    fun onUnitTestSucceeded() {
        if (settings.disabled) return
        amountOfTestsRun += 1
    }

    fun onUnitTestFailed() {
        if (settings.disabled) return
        ide.revertCurrentChangeList()
        ide.notifyThatChangesWereReverted()
        amountOfTestsRun = zero
    }

    fun allowOneCommitWithoutChecks() {
        allowOneCommitWithoutChecks = true
    }

    fun isCommitAllowed(): Boolean {
        if (allowOneCommitWithoutChecks || settings.disabled) return true

        return if (amountOfTestsRun == zero) {
            ide.notifyThatCommitWasCancelled()
            false
        } else {
            true
        }
    }

    fun onSuccessfulCommit() {
        amountOfTestsRun = zero
        allowOneCommitWithoutChecks = false
    }

    fun onSettings(settings: Settings) {
        this.settings = settings
    }

    data class Settings(val enabled: Boolean, val notifyOnRevert: Boolean) {
        val disabled = !enabled
    }

    @Suppress("unused") // IDE is wrong
    private data class Amount(val n: Int) {
        operator fun plus(n: Int) = Amount(this.n + n)

        companion object {
            val zero = Amount(0)
        }
    }
}