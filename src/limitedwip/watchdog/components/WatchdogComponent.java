/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package limitedwip.watchdog.components;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import limitedwip.common.settings.LimitedWIPSettings;
import limitedwip.common.LimitedWipCheckin;
import limitedwip.common.settings.LimitedWipConfigurable;
import limitedwip.common.TimerComponent;
import limitedwip.watchdog.ChangeSizeWatchdog;

public class WatchdogComponent extends AbstractProjectComponent implements LimitedWipConfigurable.Listener, LimitedWipCheckin.Listener {
	private ChangeSizeWatchdog changeSizeWatchdog;
	private IdeNotifications ideNotifications;
	private TimerComponent timer;


	public WatchdogComponent(Project project) {
		super(project);
		timer = ApplicationManager.getApplication().getComponent(TimerComponent.class);
	}

	@Override public void projectOpened() {
		super.projectOpened();

		LimitedWIPSettings settings = ServiceManager.getService(LimitedWIPSettings.class);
		ideNotifications = new IdeNotifications(myProject, settings);
		IdeActions ideActions = new IdeActions(myProject);
		changeSizeWatchdog = new ChangeSizeWatchdog(ideNotifications, ideActions, new ChangeSizeWatchdog.Settings(
				settings.watchdogEnabled,
				settings.maxLinesInChange,
				settings.notificationIntervalInSeconds()
		));

		onSettingsUpdate(settings);

		timer.addListener(new TimerComponent.Listener() {
			@Override public void onUpdate(int seconds) {
				changeSizeWatchdog.onTimer(seconds);
			}
		}, myProject);

		LimitedWipConfigurable.registerSettingsListener(myProject, this);
		LimitedWipCheckin.registerListener(myProject, this);
	}

	@Override public void onSettingsUpdate(LimitedWIPSettings settings) {
		ideNotifications.onSettingsUpdate(settings);
		changeSizeWatchdog.onSettings(new ChangeSizeWatchdog.Settings(
				settings.watchdogEnabled,
				settings.maxLinesInChange,
				settings.notificationIntervalInSeconds()
		));
	}

    public void toggleSkipNotificationsUntilCommit() {
        boolean value = changeSizeWatchdog.toggleSkipNotificationsUntilCommit();
        ideNotifications.onSkipNotificationUntilCommit(value);
    }

	public void skipNotificationsUntilCommit(boolean value) {
		changeSizeWatchdog.skipNotificationsUntilCommit(value);
        ideNotifications.onSkipNotificationUntilCommit(value);
	}

	@Override public void onSuccessfulCheckin(boolean allFileAreCommitted) {
		changeSizeWatchdog.onCommit();
	}
}