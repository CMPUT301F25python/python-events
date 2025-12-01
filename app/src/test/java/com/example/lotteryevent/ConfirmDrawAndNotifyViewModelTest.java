package com.example.lotteryevent;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.FakeEventRepository;
import com.example.lotteryevent.viewmodels.ConfirmDrawAndNotifyViewModel;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Unit tests for the business logic within {@link ConfirmDrawAndNotifyViewModel}.
 * <p>
 *  This class validates that the loadEventAndEntrants, calculateEntrantCounts, calculateUiState,
 *  onPositiveButtonClicked, onNegativeButtonClicked methods execute as expected
 * <p>
 * It uses a {@link FakeEventRepository} to simulate data persistence without requiring
 * a running Android emulator or Firebase connection.
 */
public class ConfirmDrawAndNotifyViewModelTest {
    /**
     * Rule to swap the background executor used by the Architecture Components
     * with a different one which executes each task synchronously.
     * <p>
     * This is required because {@link ConfirmDrawAndNotifyViewModel} uses {@link androidx.lifecycle.LiveData}
     * to update loading states and messages, which normally requires the Android Main Looper.
     */
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private ConfirmDrawAndNotifyViewModel viewModel;
    private FakeEventRepository fakeRepository;
    private NotificationCustomManager notificationCustomManager;

    /**
     * Sets up the test environment.
     * <p>
     * 1. Initializes the Fake Repository.
     * 2. Initializes the ViewModel with the fake repository.
     */
    @Before
    public void setUp() {
        // 1. Init Fake Repository and notif manager
        fakeRepository = new FakeEventRepository();
        notificationCustomManager = mock(NotificationCustomManager.class);
        when(notificationCustomManager.sendNotification(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Tasks.forResult(mock(DocumentReference.class)));

        // 2. Init ViewModel with Fake Repo
        viewModel = new ConfirmDrawAndNotifyViewModel(fakeRepository, notificationCustomManager);
    }

    /**
     * Tests for repository's loadEventAndEntrantCounts called successfully
     */
    @Test
    public void loadEventAndEntrantCountsSuccess() {
        viewModel.loadEventAndEntrantCounts("fake-event-id");

        assertEquals(viewModel.event.getValue(), fakeRepository.getUserEvent().getValue());
    }

    /**
     * Tests for valid behaviour when invalid event id given
     */
    @Test
    public void testLoadEventAndEntrantCountsFailure() {
        viewModel.loadEventAndEntrantCounts(null);

        assertEquals("Error: Missing Event ID.", Objects.requireNonNull(viewModel.bottomUiState.getValue()).infoText);
    }

    /**
     * Tests for behaviour when notifs not set up on clicking "Confirm and Notify" button
     */
    @Test
    public void testNotificationsNotSetUpOnPosBtnClick() {
        ConfirmDrawAndNotifyViewModel customViewModel = new ConfirmDrawAndNotifyViewModel(fakeRepository, null);
        customViewModel.onPositiveButtonClicked(new ArrayList<>(), new ArrayList<>());

        assertEquals("Error: Notifications not set up.", Objects.requireNonNull(customViewModel.bottomUiState.getValue()).infoText);
    }

    /**
     * Tests for valid send notif parameters for lottery win
     */
    @Test
    public void sendWinNotification_callsNotifManagerWithCorrectParams() {
        fakeRepository.fetchEventAndEntrantCounts("fake-event-id");

        viewModel.sendWinNotification("user123");

        verify(notificationCustomManager).sendNotification(eq("user123"), eq("Congratulations!"),
                eq("You've been selected for Event 1! Tap to accept or decline."), eq("lottery_win"),
                eq("fake-event-id"), eq("Event 1"), eq(null), any());
    }

    /**
     * Tests for valid send notif parameters for lottery loss
     */
    @Test
    public void sendLossNotification_callsNotifManagerWithCorrectParams() {
        fakeRepository.fetchEventAndEntrantCounts("fake-event-id");

        viewModel.sendLossNotification(("user123"));

        verify(notificationCustomManager).sendNotification(eq("user123"), eq("Thank you for joining!"),
                eq("You weren't selected for Event 1 in this draw, but you're still on the waiting list and may be chosen in a future redraw."),
                eq("lottery_loss"), eq("fake-event-id"), eq("Event 1"), eq(null), any());
    }

    /**
     * Tests for no counts loaded when in loading state, correct bottom ui state
     */
    @Test
    public void testCalcEntrantCountsLoading() {
        viewModel.bottomUiState.observeForever(bottomUiState -> {});
        fakeRepository.setIsLoading(true);

        assertNull(viewModel.waitingListCount.getValue());
        assertNull(viewModel.availableSpaceCount.getValue());
        assertEquals(BottomUiState.StateType.LOADING, Objects.requireNonNull(viewModel.bottomUiState.getValue()).type);
    }

    /**
     * Tests for expected counts, bottom ui state, when event and waiting entrants loaded successfully
     */
    @Test
    public void testCalcEntrantCountsSuccessWaiting() {
        viewModel.waitingListCount.observeForever(s -> {});
        viewModel.availableSpaceCount.observeForever(s -> {});
        viewModel.bottomUiState.observeForever(s -> {});

        fakeRepository.fetchEventAndEntrantCounts("fake-event-id");

        assertEquals("2", viewModel.waitingListCount.getValue());
        assertEquals("2", viewModel.availableSpaceCount.getValue());
        assertEquals(BottomUiState.StateType.SHOW_TWO_BUTTONS, Objects.requireNonNull(viewModel.bottomUiState.getValue()).type);
    }

    /**
     * Tests for expected counts, bottom ui state, when event and invited entrants loaded successfully
     */
    @Test
    public void testCalcEntrantCountsSuccessInvited() {
        viewModel.waitingListCount.observeForever(s -> {});
        viewModel.availableSpaceCount.observeForever(s -> {});
        viewModel.bottomUiState.observeForever(s -> {});

        ArrayList<Entrant> inMemoryEntrants = fakeRepository.getInMemoryEntrants();
        for (Entrant entrant : inMemoryEntrants) {
            fakeRepository.updateEntrantAttribute("fake-event-id", String.valueOf(entrant.getUserId()), "status", "invited");
        }

        fakeRepository.fetchEventAndEntrantCounts("fake-event-id");

        assertEquals("0", viewModel.waitingListCount.getValue());
        assertEquals("0", viewModel.availableSpaceCount.getValue());
        assertEquals(BottomUiState.StateType.SHOW_TWO_BUTTONS, Objects.requireNonNull(viewModel.bottomUiState.getValue()).type);
    }

    /**
     * Tests for expected counts, bottom ui state, when event and entrants loaded successfully and no capacity is set
     */
    @Test
    public void testCalcEntrantCountsNoLimit() {
        List<Event> inMemoryEvents = fakeRepository.getInMemoryEvents();
        inMemoryEvents.get(0).setCapacity(null);
        fakeRepository.setInMemoryEvents(inMemoryEvents);

        viewModel.waitingListCount.observeForever(s -> {});
        viewModel.availableSpaceCount.observeForever(s -> {});
        viewModel.bottomUiState.observeForever(s -> {});

        ArrayList<Entrant> inMemoryEntrants = fakeRepository.getInMemoryEntrants();
        for (Entrant entrant : inMemoryEntrants) {
            fakeRepository.updateEntrantAttribute("fake-event-id", String.valueOf(entrant.getUserId()), "status", "invited");
        }

        fakeRepository.fetchEventAndEntrantCounts("fake-event-id");

        assertEquals("0", viewModel.waitingListCount.getValue());
        assertEquals("No Limit", viewModel.availableSpaceCount.getValue());
        assertEquals(BottomUiState.StateType.SHOW_TWO_BUTTONS, Objects.requireNonNull(viewModel.bottomUiState.getValue()).type);
    }

    /**
     * Tests for expected counts, bottom ui state, when event and entrants loaded successfully and the capacity is less than the number of entrants invited
     */
    @Test
    public void testCalcEntrantCountsOverCapacity() {
        List<Event> inMemoryEvents = fakeRepository.getInMemoryEvents();
        inMemoryEvents.get(0).setCapacity(1);
        fakeRepository.setInMemoryEvents(inMemoryEvents);

        viewModel.waitingListCount.observeForever(s -> {});
        viewModel.availableSpaceCount.observeForever(s -> {});
        viewModel.bottomUiState.observeForever(s -> {});

        ArrayList<Entrant> inMemoryEntrants = fakeRepository.getInMemoryEntrants();
        for (Entrant entrant : inMemoryEntrants) {
            fakeRepository.updateEntrantAttribute("fake-event-id", String.valueOf(entrant.getUserId()), "status", "invited");
        }

        fakeRepository.fetchEventAndEntrantCounts("fake-event-id");

        assertEquals("0", viewModel.waitingListCount.getValue());
        assertEquals("0", viewModel.availableSpaceCount.getValue());
        assertEquals(BottomUiState.StateType.SHOW_TWO_BUTTONS, Objects.requireNonNull(viewModel.bottomUiState.getValue()).type);
    }
}
