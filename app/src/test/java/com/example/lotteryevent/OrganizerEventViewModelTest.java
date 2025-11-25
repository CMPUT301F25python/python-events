package com.example.lotteryevent;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.FakeOrganizerEventRepository;
import com.example.lotteryevent.viewmodels.OrganizerEventViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the OrganizerEventViewModel.
 */
public class OrganizerEventViewModelTest {

    // Rule to execute LiveData updates synchronously for testing.
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    // SUT (System Under Test)
    private OrganizerEventViewModel viewModel;
    // Test double
    private FakeOrganizerEventRepository fakeRepository;

    @Before
    public void setup() {
        // Arrange: Create the fake repository and inject it into the ViewModel.
        fakeRepository = new FakeOrganizerEventRepository();
        viewModel = new OrganizerEventViewModel(fakeRepository);
    }

    @Test
    public void loadEvent_whenSuccessful_setsEventAndButtonState() {
        // Act: Trigger the data load in the ViewModel.
        viewModel.loadEvent("any-event-id");

        // Assert: Verify the LiveData objects are updated with the correct success data.
        Event resultEvent = viewModel.getEvent().getValue();
        Boolean isEnabled = viewModel.isRunDrawButtonEnabled().getValue();

        assertNotNull("Event LiveData should not be null", resultEvent);
        assertEquals("The event name is incorrect", "Test Event", resultEvent.getName());

        assertNotNull("isRunDrawButtonEnabled LiveData should not be null", isEnabled);
        assertTrue("Run Draw button should be enabled on success", isEnabled);
    }

    @Test
    public void loadEvent_whenSuccessful_setsCorrectUiState() {
        // Act
        viewModel.loadEvent("any-event-id");

        // Assert
        OrganizerEventViewModel.UiState uiState = viewModel.getUiState().getValue();
        assertEquals("UI State should be OPEN for a standard event", OrganizerEventViewModel.UiState.OPEN, uiState);
    }

    @Test
    public void isLoading_isFalse_afterFetchCompletes() {
        // Act
        viewModel.loadEvent("any-event-id");

        // Assert
        Boolean isLoading = viewModel.isLoading().getValue();
        assertNotNull("isLoading LiveData should not be null", isLoading);
        assertFalse("isLoading should be false after the fetch is complete", isLoading);
    }

    /**
     * Verifies that when updateEventPoster is called on the ViewModel,
     * the call is delegated to the repository with the same arguments.
     */
    @Test
    public void updateEventPoster_delegatesToRepository() {
        String eventId = "event-123";
        String base64 = "test-base64-data";

        viewModel.updateEventPoster(eventId, base64);

        assertEquals(eventId, fakeRepository.getLastUpdatedPosterEventId());
        assertEquals(base64, fakeRepository.getLastUpdatedPosterBase64());
    }


}