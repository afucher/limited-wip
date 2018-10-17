package limitedwip.limbo

import limitedwip.expect
import limitedwip.limbo.components.Ide
import limitedwip.shouldEqual
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never

class LimboTests {
    private val ide = mock(Ide::class.java)
    private val settings = Limbo.Settings(enabled = true, notifyOnRevert = true)
    private val limbo = Limbo(ide, settings)

    @Test fun `allow commits only after running a unit test`() {
        limbo.isCommitAllowed() shouldEqual false
        ide.expect().notifyThatCommitWasCancelled()
        limbo.onUnitTestSucceeded()
        limbo.isCommitAllowed() shouldEqual true
    }

    @Test fun `after commit need to run a unit test to be able to commit again`() {
        limbo.onUnitTestSucceeded()
        limbo.isCommitAllowed() shouldEqual true

        limbo.onSuccessfulCommit()
        limbo.isCommitAllowed() shouldEqual false

        limbo.onUnitTestSucceeded()
        limbo.isCommitAllowed() shouldEqual true
    }

    @Test fun `revert changes on failed unit test`() {
        limbo.onUnitTestFailed()
        ide.expect().revertCurrentChangeList()
        ide.expect().notifyThatChangesWereReverted()
    }

    @Test fun `can do one-off commit without running a unit test`() {
        limbo.isCommitAllowed() shouldEqual false
        limbo.allowOneCommitWithoutChecks()
        limbo.isCommitAllowed() shouldEqual true
        limbo.onSuccessfulCommit()
        limbo.isCommitAllowed() shouldEqual false
    }

    @Test fun `if disabled, always allow commits`() {
        limbo.onSettings(settings.copy(enabled = false))
        limbo.isCommitAllowed() shouldEqual true
    }

    @Test fun `if disabled, don't revert changes on failed unit test`() {
        limbo.onSettings(settings.copy(enabled = false))
        limbo.onUnitTestFailed()
        ide.expect(never()).revertCurrentChangeList()
        ide.expect(never()).notifyThatChangesWereReverted()
    }

    @Test fun `if disabled, don't count successful test runs`() {
        limbo.onSettings(settings.copy(enabled = false))
        limbo.onUnitTestSucceeded()
        limbo.onSettings(settings.copy(enabled = true))
        limbo.isCommitAllowed() shouldEqual false
    }
    
    // TODO open commit dialog after running a test
}