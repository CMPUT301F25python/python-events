package com.example.lotteryevent;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.repository.FakeEventRepository;
import com.example.lotteryevent.viewmodels.ConfirmDrawAndNotifyViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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

    /**
     * Sets up the test environment.
     * <p>
     * 1. Initializes the Fake Repository.
     * 2. Initializes the ViewModel with the fake repository.
     */
    @Before
    public void setUp() {
        // 1. Init Fake Repository
        fakeRepository = new FakeEventRepository();

        // 2. Init ViewModel with Fake Repo
        viewModel = new ConfirmDrawAndNotifyViewModel(fakeRepository, null);
    }

    /**
     * Tests for repository's fetchEventAndEntrants called successfully
     */
    @Test
    public void testLoadEventEntrantsSuccess() {
        viewModel.loadEventAndEntrants("fake-event-id");

        assertEquals(viewModel.event.getValue(), fakeRepository.getUserEvent().getValue());
    }

    /**
     * Tests for valid behaviour when invalid event id given
     */
    @Test
    public void testLoadEventEntrantsFailure() {
        viewModel.loadEventAndEntrants(null);

        assertEquals("Error: Missing Event ID.", Objects.requireNonNull(viewModel.bottomUiState.getValue()).infoText);
    }

    /**
     * Tests for behaviour when notifs not set up on clicking "Confirm and Notify" button
     */
    @Test
    public void testNotificationsNotSetUpOnPosBtnClick() {
        viewModel.onPositiveButtonClicked();

        assertEquals("Error: Notifications not set up.", Objects.requireNonNull(viewModel.bottomUiState.getValue()).infoText);
    }

    /**
     * Tests for the reverting of entrants' status when cancelling
     */
    @Test
    public void testNotificationsOnNegBtnClick() {
        fakeRepository.updateEntrantsAttributes("fake-event-id", "status", "waiting", "invited");
        viewModel.loadEventAndEntrants("fake-event-id");
        viewModel.onNegativeButtonClicked();

        for (Entrant e : Objects.requireNonNull(fakeRepository.getEventEntrants().getValue())) {
            assertEquals("waiting", e.getStatus());
        }
    }

    /**
     * Tests for no counts loaded when in loading state, correct bottom ui state
     */
    @Test
    public void testCalcEntrantCountsLoading() {
        viewModel.bottomUiState.observeForever(bottomUiState -> {});
        fakeRepository.setIsLoading(true);

        assertNull(viewModel.waitingListCount.getValue());
        assertNull(viewModel.selectedUsersCount.getValue());
        assertNull(viewModel.availableSpaceCount.getValue());
        assertEquals(BottomUiState.StateType.LOADING, Objects.requireNonNull(viewModel.bottomUiState.getValue()).type);
    }

    /**
     * Tests for valid waiting list count, no other count, based on what gets
     * updated in repository
     */
    @Test
    public void testCalcEntrantCountWaitingList() {
        viewModel.waitingListCount.observeForever(string -> {});
        fakeRepository.setWaitingListCount(1);

        assertEquals("1", viewModel.waitingListCount.getValue());
        assertNull(viewModel.selectedUsersCount.getValue());
        assertNull(viewModel.availableSpaceCount.getValue());
    }

    /**
     * Tests for expected counts, bottom ui state, when event and entrants loaded successfully
     */
    @Test
    public void testCalcEntrantCountSelectedUsersAvailSpace() {
        viewModel.waitingListCount.observeForever(s -> {});
        viewModel.selectedUsersCount.observeForever(s -> {});
        viewModel.availableSpaceCount.observeForever(s -> {});
        viewModel.bottomUiState.observeForever(s -> {});

        fakeRepository.updateEntrantsAttributes("fake-event-id", "status", "waiting", "invited");

        fakeRepository.fetchEventAndEntrants("fake-event-id");

        assertEquals("0", viewModel.waitingListCount.getValue());
        assertEquals("2", viewModel.selectedUsersCount.getValue());
        assertEquals("0", viewModel.availableSpaceCount.getValue());
        assertEquals(BottomUiState.StateType.SHOW_TWO_BUTTONS, Objects.requireNonNull(viewModel.bottomUiState.getValue()).type);
    }
}
