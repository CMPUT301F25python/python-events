package com.example.lotteryevent;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.repository.EntrantListRepositoryImpl;
import com.example.lotteryevent.repository.IEntrantListRepository.StatusUpdateCallback;
import com.example.lotteryevent.viewmodels.EntrantListViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit test suite for {@link com.example.lotteryevent.viewmodels.EntrantListViewModel}.
 * These tests run on the JVM without Android framework dependencies and verify
 * that the ViewModel correctly interacts with its repository for fetching
 * entrants and sending notifications.
 * <p>Mockito is used for mocking the repository, and
 * {@link androidx.arch.core.executor.testing.InstantTaskExecutorRule} ensures
 * LiveData updates execute synchronously during testing.</p>
 */
public class EntrantListViewModelTest {

    /**
     * Ensures that LiveData operations run synchronously during JVM unit tests.
     * This rule replaces the background executor normally used by Architecture
     * Components with one that executes tasks immediately.
     */
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private EntrantListRepositoryImpl repository;
    private EntrantListViewModel viewModel;
    private MutableLiveData<List<Entrant>> mockRepoLiveData;

    private static final String TEST_EVENT_ID = "event123";
    private static final String TEST_STATUS = "accepted";


    /**
     * Creates a mocked {@link EntrantListRepositoryImpl} and injects it into a new
     * {@link EntrantListViewModel} instance before each test. Ensures each test
     * starts with a clean and isolated environment.
     */
    @Before
    public void setUp() {
        repository = mock(EntrantListRepositoryImpl.class);
        mockRepoLiveData = new MutableLiveData<>();
        when(repository.fetchEntrantsByStatus(TEST_EVENT_ID, TEST_STATUS))
                .thenReturn(mockRepoLiveData);

        viewModel = new EntrantListViewModel(repository, TEST_EVENT_ID, TEST_STATUS);
    }

    /**
     * Verifies that {@link EntrantListViewModel#getEntrants()} correctly
     * delegates to the repository with the expected eventId and status values.
     * <p>Asserts that:</p>
     * <ul>
     *     <li>the repository method is invoked with the correct parameters,</li>
     *     <li>the ViewModel returns the exact same LiveData instance provided by the repository.</li>
     * </ul>
     */
    @Test
    public void test_getEntrants_correct() {
        // Act
        LiveData<List<Entrant>> result = viewModel.getEntrants();

        // Verify repository method was called (happened in constructor)
        verify(repository).fetchEntrantsByStatus(TEST_EVENT_ID, TEST_STATUS);

        // Verify the ViewModel returns the exact LiveData instance we created
        assert result == mockRepoLiveData;
    }

    /**
     * Ensures that when {@link EntrantListViewModel#notifyAllEntrants(String)}
     * is called with a null entrant list, the repository is never asked to send
     * notifications. This guards against null-pointer related behavior.
     */
    @Test
    public void test_notifyAllEntrants_null() {
        mockRepoLiveData.setValue(null);
        viewModel.notifyAllEntrants("Hello");
        verify(repository, never()).notifyEntrant(anyString(), anyString(), anyString());
        verify(repository).setUserMessage("No entrants to notify.");
    }

    /**
     * Ensures that an empty entrant list does not trigger any calls to
     * {@link EntrantListRepositoryImpl#notifyEntrant(String, String, String)}.
     * Prevents unnecessary iteration or operations on empty datasets.
     */
    @Test
    public void test_notifyAllEntrants_empty() {
        // Arrange: Set the internal LiveData value to empty list
        mockRepoLiveData.setValue(new ArrayList<>());

        // Act
        viewModel.notifyAllEntrants("Hello");

        // Assert
        verify(repository, never()).notifyEntrant(anyString(), anyString(), anyString());
        verify(repository).setUserMessage("No entrants to notify.");
    }

    /**
     * Ensures that when {@link EntrantListViewModel#notifyAllEntrants(String)}
     * is called with an empty message, the repository does not notify entrants
     */
    @Test
    public void test_notifyAllEntrants_empty_message() {
        // Arrange: Set valid list
        List<Entrant> entrants = new ArrayList<>();
        entrants.add(new Entrant());
        mockRepoLiveData.setValue(entrants);

        // Act
        viewModel.notifyAllEntrants("");

        // Assert
        verify(repository, never()).notifyEntrant(anyString(), anyString(), anyString());
        verify(repository).setUserMessage("No message provided.");
    }

    /**
     * Verifies that {@link EntrantListViewModel#notifyAllEntrants(String)}
     * sends notifications only for entrants with valid, non-null user IDs.
     * <p>Test behavior:</p>
     * <ul>
     *     <li>Entrants with mocked user IDs "user1" and "user2" should trigger notification calls.</li>
     *     <li>Entrants with null user IDs (or null entries in the list) should be ignored.</li>
     * </ul>
     * <p>Mockito is used to validate that:</p>
     * <ul>
     *     <li>notifyEntrant() is called exactly for valid IDs,</li>
     *     <li>notifyEntrant() is <em>never</em> called with null user IDs.</li>
     * </ul>
     */
    @Test
    public void test_notifyAllEntrants_valid() {
        String message = "Hello";

        // generating mock entrants
        Entrant entrantWithId1 = mock(Entrant.class);
        Entrant entrantWithId2 = mock(Entrant.class);
        Entrant entrantWithoutId = mock(Entrant.class);
        /**
         * Mockito stubs for entrant.getUserId() calls. These stubs allow the test to
         * simulate valid IDs, invalid IDs, and null users within the iterator loop.
         * @return the mocked user ID for each test entrant
         */
        when(entrantWithId1.getUserId()).thenReturn("user1");
        when(entrantWithId2.getUserId()).thenReturn("user2");
        when(entrantWithoutId.getUserId()).thenReturn(null);

        List<Entrant> entrants = Arrays.asList(entrantWithId1, null, entrantWithoutId, entrantWithId2);
        mockRepoLiveData.setValue(entrants);

        viewModel.notifyAllEntrants(message);

        /**
         * Verification step ensuring the repository receives notification calls
         * only for entrants with valid non-null user IDs. Confirms that filtering
         * inside the ViewModel behaves as expected.
         * @param uid the expected user ID of a valid entrant
         * @param eventId the event associated with the notification
         * @param message the organizer's message sent to all entrants
         */
        verify(repository).notifyEntrant("user1", TEST_EVENT_ID, message);
        verify(repository).notifyEntrant("user2", TEST_EVENT_ID, message);
        verify(repository, never()).notifyEntrant(Mockito.isNull(), Mockito.anyString(), Mockito.anyString());
    }

    /**
     * Verifies that {@link EntrantListViewModel#cancelInvite(String)} calls the repository
     * with the correct parameters ("waiting" status) and triggers a data refresh upon success.
     */
    @Test
    public void test_cancelInvite_success() {
        String targetUserId = "userToCancel";

        viewModel.cancelInvite(targetUserId);

        // Capture the callback passed to the repository
        ArgumentCaptor<StatusUpdateCallback> callbackCaptor = ArgumentCaptor.forClass(StatusUpdateCallback.class);

        verify(repository).updateEntrantStatus(eq(TEST_EVENT_ID), eq(targetUserId), eq("waiting"), callbackCaptor.capture());
        callbackCaptor.getValue().onSuccess();

        // Verify the ViewModel reacts correctly to success:
        verify(repository).setUserMessage("User returned to waitlist.");
        verify(repository, times(2)).fetchEntrantsByStatus(TEST_EVENT_ID, TEST_STATUS);
    }

    /**
     * Verifies that {@link EntrantListViewModel#cancelInvite(String)} does not call the repository
     * if the provided userId is null.
     */
    @Test
    public void test_cancelInvite_nullUserId() {
        viewModel.cancelInvite(null);

        // Verify repo is never called
        verify(repository, never()).updateEntrantStatus(anyString(), anyString(), anyString(), any());
    }

    /**
     * Verifies that {@link EntrantListViewModel#cancelInvite(String)} calls the repository
     * with the correct parameters ("waiting" status) and triggers a failure message upon failure.
     */
    @Test
    public void test_cancelInvite_failure() {
        String targetUserId = "userToCancel";
        viewModel.cancelInvite(targetUserId);

        ArgumentCaptor<StatusUpdateCallback> callbackCaptor = ArgumentCaptor.forClass(StatusUpdateCallback.class);
        verify(repository).updateEntrantStatus(eq(TEST_EVENT_ID), eq(targetUserId), eq("waiting"), callbackCaptor.capture());

        // Simulate Failure
        callbackCaptor.getValue().onFailure(new Exception("Firestore error"));

        // Verify that we DO NOT trigger a refresh or success message
        // (fetchEntrantsByStatus was called 1 time in constructor, shouldn't be called again)
        verify(repository, times(1)).fetchEntrantsByStatus(anyString(), anyString());
        verify(repository, never()).setUserMessage("User returned to waitlist.");
    }

    /**
     * Test that {@link EntrantListViewModel#getCapitalizedStatus()} correctly
     * capitalizes the status string.
     */
    @Test
    public void test_getCapitalizedStatus() {
        assertEquals("Accepted", viewModel.getCapitalizedStatus());
    }

    /**
     * Test that {@link EntrantListViewModel#getCapitalizedStatus()} correctly
     * capitalizes the status string.
     */
    @Test
    public void test_getCapitalizedStatus_handles_case() {
        // Create a temporary VM with lowercase status for this specific test
        EntrantListViewModel localVm = new EntrantListViewModel(repository, TEST_EVENT_ID, "invited");
        assertEquals("Invited", localVm.getCapitalizedStatus());
    }

    /**
     * Test that {@link EntrantListViewModel#getCapitalizedStatus()} correctly
     * handles a null status.
     */
    @Test
    public void test_getCapitalizedStatus_handles_null() {
        EntrantListViewModel localVm = new EntrantListViewModel(repository, TEST_EVENT_ID, null);
        assertEquals("", localVm.getCapitalizedStatus());
    }
}
