package com.example.lotteryevent;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.FakeEventRepository;
import com.example.lotteryevent.viewmodels.HomeViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Unit tests for the {@link HomeViewModel}.
 * <p>
 * These tests verify the business logic of the Home screen, specifically how the ViewModel
 * handles data returned from the repository (success and error states) and manages
 * loading indicators.
 * <p>
 * This class runs on the local JVM (Unit Test) using a {@link FakeEventRepository}
 * to simulate the data layer.
 */
public class HomeViewModelTest {

    /**
     * A JUnit Test Rule that swaps the background executor used by the Architecture Components
     * with a different one that executes each task synchronously.
     * <p>
     * This is mandatory for testing LiveData, as it bypasses the Main Looper requirement.
     */
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private HomeViewModel viewModel;
    private FakeEventRepository fakeRepository;

    /**
     * Sets up the test environment before each test case.
     * <p>
     * 1. Creates a fresh instance of {@link FakeEventRepository}.
     * 2. Injects the fake repository into the {@link HomeViewModel}.
     */
    @Before
    public void setUp() {
        fakeRepository = new FakeEventRepository();
        viewModel = new HomeViewModel(fakeRepository);
    }

    /**
     * Verifies the initial state of the ViewModel before any actions are taken.
     * The event list should ideally be null or empty depending on initialization.
     */
    @Test
    public void initialState_isCorrect() {
        // --- Assert ---
        // Check that we start with the initial data from the repo constructor
        // (FakeRepo initializes with 2 items immediately in its constructor)
        List<Event> initialEvents = viewModel.getEvents().getValue();
        assertNotNull("Initial events should not be null", initialEvents);
        assertEquals("Should start with default repository data", 2, initialEvents.size());
    }

    /**
     * Tests the happy path where the repository successfully returns a list of events.
     * <p>
     * <b>Scenario:</b> {@code fetchUserEvents()} is called and the repository succeeds.
     * <b>Expected:</b> The {@code events} LiveData is updated with the data.
     */
    @Test
    public void fetchUserEvents_success_updatesEventsLiveData() {
        // --- Arrange ---
        // (Repository is already set up with default data in setUp())

        // --- Act ---
        viewModel.fetchUserEvents();

        // --- Assert ---
        List<Event> events = viewModel.getEvents().getValue();

        assertNotNull("Events LiveData should not be null", events);
        assertEquals("There should be 2 events in the list", 2, events.size());

        // Verify data integrity
        assertEquals("The name of the first event is incorrect", "Event 1", events.get(0).getName());
        assertEquals("The name of the second event is incorrect", "Event 2", events.get(1).getName());
    }

    /**
     * Tests the error path where the repository fails to fetch data.
     * <p>
     * <b>Scenario:</b> The repository is configured to return an error.
     * <b>Expected:</b>
     * 1. The {@code message} LiveData contains the error text.
     * 2. The {@code events} LiveData is cleared (empty).
     */
    @Test
    public void fetchUserEvents_error_setsErrorMessageAndClearsList() {
        // --- Arrange ---
        // Configure the fake to fail
        fakeRepository.setShouldReturnError(true);

        // --- Act ---
        viewModel.fetchUserEvents();

        // --- Assert ---
        String errorMessage = viewModel.getMessage().getValue();
        List<Event> events = viewModel.getEvents().getValue();

        // Verify error message
        assertNotNull("Error message should be present", errorMessage);
        assertEquals("Test Error: Could not fetch events.", errorMessage);

        // Verify list is empty
        assertNotNull("Events list should not be null", events);
        assertTrue("Events list should be empty after an error", events.isEmpty());
    }

    /**
     * Verifies that the loading state is correctly reset after an operation.
     * <p>
     * <b>Scenario:</b> Data fetch is initiated and completes.
     * <b>Expected:</b> {@code isLoading} LiveData should be false at the end of the operation.
     */
    @Test
    public void fetchUserEvents_complete_setsIsLoadingToFalse() {
        // --- Act ---
        viewModel.fetchUserEvents();

        // --- Assert ---
        Boolean isLoading = viewModel.isLoading().getValue();
        assertNotNull("isLoading LiveData should not be null", isLoading);
        assertFalse("isLoading should be false after fetch completes", isLoading);
    }
}