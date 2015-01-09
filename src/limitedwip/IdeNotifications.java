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
package limitedwip;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.Consumer;
import limitedwip.components.LimitedWIPProjectComponent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.MouseEvent;

public class IdeNotifications {
	private final AutoRevertStatusBarWidget autoRevertWidget = new AutoRevertStatusBarWidget();
	private final Project project;
	private boolean showTimerInToolbar;
	private boolean autoRevertEnabled;

	public IdeNotifications(Project project) {
		this.project = project;

		StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
		if (statusBar != null) {
			autoRevertWidget.showStoppedText();
			statusBar.addWidget(autoRevertWidget);
			statusBar.updateWidget(autoRevertWidget.ID());
		}
	}

	public void onProjectClosed() {
		StatusBar statusBar = statusBarFor(project);
		if (statusBar != null) {
			autoRevertWidget.showStoppedText();
			statusBar.removeWidget(autoRevertWidget.ID());
			statusBar.updateWidget(autoRevertWidget.ID());
		}
	}

	public void onAutoRevertStarted(int timeEventsTillRevert) {
		if (showTimerInToolbar) {
			autoRevertWidget.showTime(formatTime(timeEventsTillRevert));
		} else {
			autoRevertWidget.showStartedText();
		}
		updateStatusBar();
	}

	public void onAutoRevertStopped() {
		autoRevertWidget.showStoppedText();
		updateStatusBar();
	}

	public void onCommit(int timeEventsTillRevert) {
		if (showTimerInToolbar) {
			autoRevertWidget.showTime(formatTime(timeEventsTillRevert));
		} else {
			autoRevertWidget.showStartedText();
		}
		updateStatusBar();
	}

	public void onTimeTillRevert(int secondsLeft) {
		if (showTimerInToolbar) {
			autoRevertWidget.showTime(formatTime(secondsLeft));
		} else {
			autoRevertWidget.showStartedText();
		}
		updateStatusBar();
	}

	public void onNewSettings(boolean showTimerInToolbar, boolean autoRevertEnabled) {
		this.showTimerInToolbar = showTimerInToolbar;
		this.autoRevertEnabled = autoRevertEnabled;
		updateStatusBar();
	}

	private void updateStatusBar() {
		StatusBar statusBar = statusBarFor(project);
		if (statusBar != null) {
			boolean hasWidget = statusBar.getWidget(autoRevertWidget.ID()) != null;
			if (hasWidget && autoRevertEnabled) {
				statusBar.updateWidget(autoRevertWidget.ID());

			} else if (hasWidget) {
				statusBar.removeWidget(autoRevertWidget.ID());

			} else if (autoRevertEnabled) {
				autoRevertWidget.showStoppedText();
				statusBar.addWidget(autoRevertWidget);
				statusBar.updateWidget(autoRevertWidget.ID());
			}
		}
	}

	private static StatusBar statusBarFor(Project project) {
		return WindowManager.getInstance().getStatusBar(project);
	}

	private static String formatTime(int seconds) {
		int min = seconds / 60;
		int sec = seconds % 60;
		return String.format("%02d", min) + ":" + String.format("%02d", sec);
	}

	public void onChangeExceededThreshold(int changeSize, int maxChangeSize) {
		// TODO implement
	}


	public static class AutoRevertStatusBarWidget implements StatusBarWidget {
		private static final String TIME_LEFT_PREFIX_TEXT = "Auto-revert in ";
		private static final String STARTED_TEXT = "Auto-revert started";
		private static final String STOPPED_TEXT = "Auto-revert stopped";

		private String text = "";

		@Override public void install(@NotNull StatusBar statusBar) {
		}

		@Override public void dispose() {
		}

		public void showTime(String timeLeft) {
			text = TIME_LEFT_PREFIX_TEXT + timeLeft;
		}

		public void showStartedText() {
			text = STARTED_TEXT;
		}

		public void showStoppedText() {
			text = STOPPED_TEXT;
		}

		@Override public WidgetPresentation getPresentation(@NotNull PlatformType type) {
			return new StatusBarWidget.TextPresentation() {
				@NotNull @Override public String getText() {
					return text;
				}

				@NotNull @Override public String getMaxPossibleText() {
					return TIME_LEFT_PREFIX_TEXT + "99:99";
				}

				@Override public Consumer<MouseEvent> getClickConsumer() {
					return new Consumer<MouseEvent>() {
						@Override public void consume(MouseEvent mouseEvent) {
							DataContext dataContext = DataManager.getInstance().getDataContext(mouseEvent.getComponent());
							Project project = PlatformDataKeys.PROJECT.getData(dataContext);
							if (project == null) return;

							LimitedWIPProjectComponent limitedWIPProjectComponent = project.getComponent(LimitedWIPProjectComponent.class);
							if (limitedWIPProjectComponent.isAutoRevertStarted()) {
								limitedWIPProjectComponent.stopAutoRevert();
							} else {
								limitedWIPProjectComponent.startAutoRevert();
							}
						}
					};
				}

				@Override public float getAlignment() {
					return Component.CENTER_ALIGNMENT;
				}

				@Override public String getTooltipText() {
					return "Click to start/stop auto-revert";
				}
			};
		}

		@NotNull @Override public String ID() {
			return "LimitedWIP_StatusBarWidget";
		}
	}
}