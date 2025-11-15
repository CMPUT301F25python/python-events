package com.example.lotteryevent.viewmodels;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.FakeEventRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Unit tests for the HomeViewModel.
 * These tests run on the local JVM and do not require an Android device or emulator.
 */
public class HomeViewModelTest {

    // A JUnit Test Rule that swaps the background executor used by the Architecture Components
    // with a different one that executes each task synchronously.
    // This is mandatory for testing LiveData.
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    // The ViewModel we are testing
    private HomeViewModel viewModel;
    // A fake repository that we control for the test
    private FakeEventRepository fakeRepository;

    @Before
    public void setup() {
        // This method runs before each test case.
        // 1. We create our fake repository.
        fakeRepository = new FakeEventRepository();
        // 2. We create the ViewModel and pass our fake repository to its constructor.
        // This is the dependency injection that makes the test possible.
        viewModel = new HomeViewModel(fakeRepository);
    }

    @Test
    public void fetchUserEvents_whenSuccessful_setsEventsLiveData() {
        // --- Act ---
        // Call the method on the ViewModel that we want to test.
        viewModel.fetchUserEvents();

        // --- Assert ---
        // Get the value from the LiveData. Since we are using InstantTaskExecutorRule,
        // the LiveData updates happen immediately.
        List<Event> events = viewModel.getEvents().getValue();

        // Verify that the LiveData was updated with the correct data from our FakeRepository.
        assertNotNull("Events LiveData should not be null", events);
        assertEquals("There should be 2 events in the list", 2, events.size());
        // Note: You might need to adjust the getter based on your Event class (e.g., .getEventName())
        assertEquals("The name of the first event is incorrect", "Event 1", events.get(0).getName());
    }

    @Test
    public void fetchUserEvents_whenError_setsErrorLiveDataAndEmptyList() {
        // --- Arrange ---
        // Configure our fake repository to simulate an error condition.
        fakeRepository.setShouldReturnError(true);

        // --- Act ---
        // Call the method we want to test.
        viewModel.fetchUserEvents();

        // --- Assert ---
        // Get the values from the LiveData objects.
        String errorMessage = viewModel.getError().getValue();
        List<Event> events = viewModel.getEvents().getValue();

        // Verify that the error LiveData was set with the error message.
        assertNotNull("Error message should not be null", errorMessage);
        assertEquals("The error message is incorrect", "Test Error: Could not fetch events.", errorMessage);

        // Verify that the events list is empty when an error occurs.
        assertNotNull("Events list should not be null", events);
        assertTrue("Events list should be empty after an error", events.isEmpty());
    }

    @Test
    public void isLoading_isFalse_afterFetchCompletes() {
        // --- Act ---
        viewModel.fetchUserEvents();

        // --- Assert ---
        // Verify that the loading state is false after the data fetching process has finished.
        Boolean isLoading = viewModel.isLoading().getValue();
        assertNotNull("isLoading LiveData should not be null", isLoading);
        assertFalse("isLoading should be false after the fetch is complete", isLoading);
    }
}