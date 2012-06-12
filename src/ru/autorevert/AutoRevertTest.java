package ru.autorevert;

import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * User: dima
 * Date: 08/06/2012
 */
public class AutoRevertTest {
	private final IdeNotification ideNotification = mock(IdeNotification.class);
	private final IdeActions ideActions = mock(IdeActions.class);
	private final Model model = new Model(ideNotification, ideActions, 2);

	@Test public void whenStarted_ShouldSendNotificationToUI() {
		model.start();

		verify(ideNotification).onAutoRevertStarted();
		verifyZeroInteractions(ideActions);
	}

	@Test public void whenStarted_OnEachTimeEvent_ShouldSentNotificationToUI() {
		model.onTimer();
		model.start();
		model.onTimer();
		model.onTimer();

		verify(ideNotification, times(2)).onTimer();
	}

	@Test public void whenStarted_AndReceivesEnoughTimeUpdates_shouldRevertCurrentChangeList() {
		model.start();
		model.onTimer();
		model.onTimer();
		model.onTimer();
		model.onTimer();

		verify(ideNotification).onAutoRevertStarted();
		verify(ideActions, times(2)).revertCurrentChangeList();
		verifyNoMoreInteractions(ideActions);
	}

	@Test public void whenStartedAndStopped_should_NOT_RevertOnNextTimeout() {
		model.start();
		model.onTimer();
		model.stop();
		model.onTimer();

		verify(ideNotification).onAutoRevertStarted();
		verify(ideNotification).onAutoRevertStopped();
		verifyZeroInteractions(ideActions);
	}

	@Test public void whenDetectsCommit_should_NOT_RevertOnNextTimeout() {
		model.start();
		model.onTimer();
		model.onCommit();
		model.onTimer();

		verify(ideNotification).onAutoRevertStarted();
		verify(ideNotification).onTimerReset();
		verifyZeroInteractions(ideActions);
	}

	private static class Model {
		private final IdeNotification ideNotification;
		private final IdeActions ideActions;
		private final int timeEventsTillRevert;

		private boolean started = false;
		private int timeEventCounter;

		public Model(IdeNotification ideNotification, IdeActions ideActions, int timeEventsTillRevert) {
			this.ideNotification = ideNotification;
			this.ideActions = ideActions;
			this.timeEventsTillRevert = timeEventsTillRevert;
		}

		public void start() {
			started = true;
			ideNotification.onAutoRevertStarted();
		}

		public void stop() {
			started = false;
			ideNotification.onAutoRevertStopped();
		}

		public void onTimer() {
			if (!started) return;

			timeEventCounter++;
			ideNotification.onTimer();

			if (timeEventCounter >= timeEventsTillRevert) {
				timeEventCounter = 0;
				ideActions.revertCurrentChangeList();
			}
		}

		public void onCommit() {
			timeEventCounter = 0;
			ideNotification.onTimerReset();
		}
	}

	private static class IdeNotification {
		public void onAutoRevertStarted() {
			// TODO implement

		}

		public void onAutoRevertStopped() {
			// TODO implement

		}

		public void onTimerReset() {
			// TODO implement

		}

		public void onTimer() {
			// TODO implement

		}
	}

	private static class IdeActions {
		public void revertCurrentChangeList() {
			// TODO implement

		}
	}
}
