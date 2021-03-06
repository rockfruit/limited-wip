package limitedwip;

import limitedwip.ChangeSizeWatchdog.Settings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;

public class ChangeSizeWatchdogTest {
    private static final int maxLinesInChange = 100;
    private static final int notificationIntervalInSeconds = 2;

    private final IdeNotifications ideNotifications = mock(IdeNotifications.class);
    private final IdeActions ideActions = mock(IdeActions.class);
    private final Settings settings = new Settings(true, maxLinesInChange, notificationIntervalInSeconds);
    private final ChangeSizeWatchdog watchdog = new ChangeSizeWatchdog(ideNotifications, ideActions, settings);

    private int secondsSinceStart;


    @Test public void doesNotSendNotification_WhenChangeSizeIsBelowThreshold() {
        when(ideActions.currentChangeListSizeInLines()).thenReturn(10);

        watchdog.onTimer(next());

        verify(ideNotifications, times(0)).onChangeSizeTooBig(anyInt(), anyInt());
    }

    @Test public void sendsNotification_WhenChangeSizeIsAboveThreshold() {
        when(ideActions.currentChangeListSizeInLines()).thenReturn(200);

        watchdog.onTimer(next());

        verify(ideNotifications).onChangeSizeTooBig(200, maxLinesInChange);
    }

    @Test public void sendsChangeSizeNotification_OnlyOnOneOfSeveralUpdates() {
        when(ideActions.currentChangeListSizeInLines()).thenReturn(200);

        watchdog.onTimer(next()); // send notification
        watchdog.onTimer(next());
        watchdog.onTimer(next()); // send notification
        watchdog.onTimer(next());

        verify(ideNotifications, times(2)).onChangeSizeTooBig(200, maxLinesInChange);
    }

    @Test public void sendsChangeSizeNotification_AfterSettingsChange() {
        when(ideActions.currentChangeListSizeInLines()).thenReturn(200);
        InOrder inOrder = inOrder(ideNotifications);

        watchdog.onTimer(next());
        inOrder.verify(ideNotifications).onChangeSizeTooBig(200, maxLinesInChange);

        watchdog.onSettings(settingsWithChangeSizeThreshold(150));
        watchdog.onTimer(next());
        inOrder.verify(ideNotifications).onChangeSizeTooBig(200, 150);
    }

    @Test public void doesNotSendNotification_WhenDisabled() {
        when(ideActions.currentChangeListSizeInLines()).thenReturn(200);

        watchdog.onSettings(watchdogDisabledSettings());
        watchdog.onTimer(next());
        watchdog.onTimer(next());

        verifyZeroInteractions(ideNotifications);
    }

    @Test public void canSkipNotificationsUtilNextCommit() {
        when(ideActions.currentChangeListSizeInLines()).thenReturn(200);

        watchdog.skipNotificationsUntilCommit(true);
        watchdog.onTimer(next());
        watchdog.onTimer(next());
        watchdog.onCommit();
        watchdog.onTimer(next());

        verify(ideNotifications).onChangeSizeTooBig(200, maxLinesInChange);
    }

    @Test public void sendsChangeSizeUpdate() {
        when(ideActions.currentChangeListSizeInLines()).thenReturn(200);
        watchdog.onTimer(next());
        verify(ideNotifications).currentChangeListSize(200, maxLinesInChange);
    }

    @Test public void sendsChangeSizeUpdate_WhenSkipNotificationUntilNextCommit() {
        when(ideActions.currentChangeListSizeInLines()).thenReturn(200);

        watchdog.skipNotificationsUntilCommit(true);
        watchdog.onTimer(next());

        verify(ideNotifications).currentChangeListSize(200, maxLinesInChange);
    }

    @Test public void doesNotSendChangeSizeUpdate_WhenDisabled() {
        when(ideActions.currentChangeListSizeInLines()).thenReturn(200);

        watchdog.onSettings(watchdogDisabledSettings());
        watchdog.onTimer(next());

        verifyZeroInteractions(ideNotifications);
    }

    @Before public void setUp() throws Exception {
        secondsSinceStart = 0;
    }

    private int next() {
        return ++secondsSinceStart;
    }

    private static Settings watchdogDisabledSettings() {
        return new Settings(false, 150, 2);
    }

    private static Settings settingsWithChangeSizeThreshold(int maxLinesInChange) {
        return new Settings(true, maxLinesInChange, 2);
    }
}