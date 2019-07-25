package limitedwip.common.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.Range
import com.intellij.util.xmlb.XmlSerializerUtil
import limitedwip.common.pluginId
import limitedwip.common.settings.CommitMessageSource.LastCommit
import limitedwip.common.settings.TcrAction.*

@State(name = "${pluginId}Settings")
data class LimitedWipSettings(
    var watchdogEnabled: Boolean = true,
    var maxLinesInChange: Int = 80,
    var notificationIntervalInMinutes: Int = 1,
    var noCommitsAboveThreshold: Boolean = false,
    var showRemainingChangesInToolbar: Boolean = true,
    var exclusions: String = "",

    var autoRevertEnabled: Boolean = false,
    var minutesTillRevert: Int = 2,
    var notifyOnRevert: Boolean = true,
    var showTimerInToolbar: Boolean = true,

    var tcrEnabled: Boolean = false,
    var tcrActionOnPassedTest: TcrAction = Commit,
    var commitMessageSource: CommitMessageSource = LastCommit,
    var notifyOnTcrRevert: Boolean = false,
    var doNotRevertTests: Boolean = false,
    var doNotRevertFiles: String = ""
): PersistentStateComponent<LimitedWipSettings> {
    private val listeners = ArrayList<Listener>()

    override fun getState(): LimitedWipSettings? = this

    override fun loadState(state: LimitedWipSettings) {
        XmlSerializerUtil.copyBean(state, this)
        notifyListeners()
    }

    fun addListener(parentDisposable: Disposable, listener: Listener) {
        listeners.add(listener)
        Disposer.register(parentDisposable, Disposable { listeners.remove(listener) })
    }

    fun notifyListeners() {
        listeners.forEach { it.onUpdate(this) }
    }

    interface Listener {
        fun onUpdate(settings: LimitedWipSettings)
    }

    companion object {
        private val minutesTillRevertRange = Range(1, 99)
        private val changedLinesRange = Range(1, 999)
        private val notificationIntervalRange = Range(1, Int.MAX_VALUE)

        fun isValidMinutesTillRevert(minutes: Int) = minutesTillRevertRange.isWithin(minutes)
        fun isValidChangedSizeRange(lineCount: Int) = changedLinesRange.isWithin(lineCount)
        fun isValidNotificationInterval(interval: Int) = notificationIntervalRange.isWithin(interval)

        fun getInstance(project: Project): LimitedWipSettings =
            ServiceManager.getService(project, LimitedWipSettings::class.java)
    }
}

fun Int.toSeconds(): Int = this * 60

enum class TcrAction {
    OpenCommitDialog,
    Commit,
    CommitAndPush
}

enum class CommitMessageSource {
    LastCommit,
    ChangeListName
}