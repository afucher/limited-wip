package limitedwip.watchdog

import limitedwip.watchdog.Watchdog.Settings
import limitedwip.watchdog.components.Ide
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.internal.matchers.InstanceOf
import org.mockito.internal.progress.ThreadSafeMockingProgress.mockingProgress
import org.mockito.Mockito.`when` as whenCalled

class WatchdogTests {

    private val ide = mock(Ide::class.java)
    private val settings = Settings(true, maxLinesInChange, notificationIntervalInSeconds, true)
    private val watchdog = Watchdog(ide, settings)

    private var seconds: Int = 0


    @Test fun `don't send notification when change size is below threshold`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(ChangeSize(10))

        watchdog.onTimer(next())

        verify(ide, times(0)).onChangeSizeTooBig(anyChangeSize(), anyInt())
    }

    @Test fun `send notification when change size is above threshold`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))

        watchdog.onTimer(next())

        verify(ide).onChangeSizeTooBig(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `send change size notification only on one of several updates`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))

        watchdog.onTimer(next()) // send notification
        watchdog.onTimer(next())
        watchdog.onTimer(next()) // send notification
        watchdog.onTimer(next())

        verify(ide, times(2)).onChangeSizeTooBig(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `send change size notification after settings change`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))
        val inOrder = inOrder(ide)

        watchdog.onTimer(next())
        inOrder.verify(ide).onChangeSizeTooBig(ChangeSize(200), maxLinesInChange)

        watchdog.onSettings(settingsWithChangeSizeThreshold(150))
        watchdog.onTimer(next())
        inOrder.verify(ide).onChangeSizeTooBig(ChangeSize(200), 150)
    }

    @Test fun `don't send notification when disabled`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))

        watchdog.onSettings(watchdogDisabledSettings())
        watchdog.onTimer(next())
        watchdog.onTimer(next())

        verify(ide, times(2)).onSettingsUpdate(anySettings())
        verifyNoMoreInteractions(ide)
    }

    @Test fun `skip notifications util next commit`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))

        watchdog.skipNotificationsUntilCommit(true)
        watchdog.onTimer(next())
        watchdog.onTimer(next())
        watchdog.onCommit()
        watchdog.onTimer(next())

        verify(ide).onChangeSizeTooBig(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `send change size update`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))
        watchdog.onTimer(next())
        verify(ide).showCurrentChangeListSize(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `still send change size update when notifications are skipped till next commit`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))

        watchdog.skipNotificationsUntilCommit(true)
        watchdog.onTimer(next())

        verify(ide).showCurrentChangeListSize(ChangeSize(200), maxLinesInChange)
    }

    @Test fun `don't send change size update when disabled`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))

        watchdog.onSettings(watchdogDisabledSettings())
        watchdog.onTimer(next())

        verify(ide, times(2)).onSettingsUpdate(anySettings())
        verifyNoMoreInteractions(ide)
    }

    @Test fun `close notification when change size is back within limit`() {
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(ChangeSize(200))
        watchdog.onTimer(next())
        whenCalled(ide.currentChangeListSizeInLines()).thenReturn(ChangeSize(0))
        watchdog.onTimer(next())
        watchdog.onTimer(next())

        verify(ide, times(1)).onChangeSizeTooBig(ChangeSize(200), maxLinesInChange)
        verify(ide, times(2)).onChangeSizeWithinLimit()
    }

    private fun next(): Int = ++seconds

    private fun anySettings(): Watchdog.Settings {
        val type = Watchdog.Settings::class.java
        mockingProgress().argumentMatcherStorage.reportMatcher(InstanceOf.VarArgAware(type, "<any " + type.canonicalName + ">"))
        return Watchdog.Settings(false, 0, 0, false)
    }
    private fun anyChangeSize(): ChangeSize {
        val type = ChangeSize::class.java
        mockingProgress().argumentMatcherStorage.reportMatcher(InstanceOf.VarArgAware(type, "<any " + type.canonicalName + ">"))
        return ChangeSize(0, false)
    }

    companion object {
        private const val maxLinesInChange = 100
        private const val notificationIntervalInSeconds = 2

        private fun watchdogDisabledSettings() = Settings(false, 150, 2, true)

        private fun settingsWithChangeSizeThreshold(maxLinesInChange: Int) =
            Settings(true, maxLinesInChange, 2, true)
    }
}