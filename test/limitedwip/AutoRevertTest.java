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

import limitedwip.AutoRevert.SettingsUpdate;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;


public class AutoRevertTest {
	private static final int CHANGE_TIMEOUT_IN_SECS = 2;

	private final IdeNotifications ideNotifications = mock(IdeNotifications.class);
	private final IdeActions ideActions = mock(IdeActions.class);
	private final AutoRevert autoRevert = new AutoRevert(ideNotifications, ideActions, CHANGE_TIMEOUT_IN_SECS);


	@Test public void sendsUIStartupNotification() {
		autoRevert.start();

		verify(ideNotifications).onAutoRevertStarted(eq(CHANGE_TIMEOUT_IN_SECS));
		verifyZeroInteractions(ideActions);
	}

	@Test public void sendsUINotificationOnTimer_OnlyWhenStarted() {
		InOrder inOrder = inOrder(ideNotifications);

		autoRevert.onTimer(); inOrder.verify(ideNotifications, times(0)).onTimeTillRevert(anyInt());
		autoRevert.start();
		autoRevert.onTimer(); inOrder.verify(ideNotifications).onTimeTillRevert(anyInt());
	}

	@Test public void revertsChanges_WhenReceivedEnoughTimeUpdates() {
		autoRevert.start();

		autoRevert.onTimer();
		autoRevert.onTimer();
		autoRevert.onTimer();
		autoRevert.onTimer();

		verify(ideActions, times(2)).revertCurrentChangeList();
		verifyNoMoreInteractions(ideActions);
	}

	@Test public void doesNotRevertChanges_WhenStopped() {
		autoRevert.start();
		autoRevert.onTimer();
		autoRevert.stop();
		autoRevert.onTimer();

		verify(ideNotifications).onAutoRevertStarted(anyInt());
		verify(ideNotifications).onAutoRevertStopped();
		verifyZeroInteractions(ideActions);
	}

	@Test public void doesNotRevertChanges_WhenDisabled() {
		autoRevert.start();
		autoRevert.onTimer();
		autoRevert.on(new SettingsUpdate(false, 2));
		autoRevert.onTimer();

		verifyZeroInteractions(ideActions);
	}

	@Test public void resetsTimeTillRevert_WhenStopped() {
		InOrder inOrder = inOrder(ideNotifications);

		autoRevert.start();
		autoRevert.onTimer(); inOrder.verify(ideNotifications).onTimeTillRevert(eq(2));
		autoRevert.stop();
		autoRevert.start();
		autoRevert.onTimer(); inOrder.verify(ideNotifications).onTimeTillRevert(eq(2));
		autoRevert.onTimer(); inOrder.verify(ideNotifications).onTimeTillRevert(eq(1));
	}

	@Test public void resetsTimeTillRevert_WhenCommitted() {
		InOrder inOrder = inOrder(ideNotifications);

		autoRevert.start();
		autoRevert.onTimer();  inOrder.verify(ideNotifications).onTimeTillRevert(eq(2));
		autoRevert.onCommit(); inOrder.verify(ideNotifications).onCommit(CHANGE_TIMEOUT_IN_SECS);
		autoRevert.onTimer();  inOrder.verify(ideNotifications).onTimeTillRevert(eq(2));
		autoRevert.onTimer();  inOrder.verify(ideNotifications).onTimeTillRevert(eq(1));
	}

	@Test public void sendsUINotificationOnCommit_OnlyWhenStarted() {
		InOrder inOrder = inOrder(ideNotifications);

		autoRevert.onCommit(); inOrder.verify(ideNotifications, times(0)).onCommit(anyInt());
		autoRevert.start();
		autoRevert.onCommit(); inOrder.verify(ideNotifications).onCommit(anyInt());
	}

	@Test public void appliesRevertTimeOutChange_AfterStart() {
		autoRevert.on(new SettingsUpdate(1));
		autoRevert.start();
		autoRevert.onTimer();
		autoRevert.onTimer();

		verify(ideActions, times(2)).revertCurrentChangeList();
	}

	@Test public void appliesRevertTimeoutChange_AfterEndOfCurrentTimeOut() {
		autoRevert.start();
		autoRevert.on(new SettingsUpdate(1));
		autoRevert.onTimer();
		autoRevert.onTimer(); // reverts changes after 2nd time event
		autoRevert.onTimer(); // reverts changes after 1st time event
		autoRevert.onTimer(); // reverts changes after 1st time event

		verify(ideActions, times(3)).revertCurrentChangeList();
	}

	@Test public void appliesRevertTimeoutChange_AfterCommit() {
		autoRevert.start();
		autoRevert.on(new SettingsUpdate(1));
		autoRevert.onTimer();
		autoRevert.onCommit();
		autoRevert.onTimer(); // reverts changes after 1st time event
		autoRevert.onTimer(); // reverts changes after 1st time event
		autoRevert.onTimer(); // reverts changes after 1st time event

		verify(ideActions, times(3)).revertCurrentChangeList();
	}

}
