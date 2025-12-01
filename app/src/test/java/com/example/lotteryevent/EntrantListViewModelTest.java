package com.example.lotteryevent;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

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

public class EntrantListViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private EntrantListRepositoryImpl repository;
    private EntrantListViewModel viewModel;
    private MutableLiveData<List<Entrant>> mockRepoLiveData;

    private static final String TEST_EVENT_ID = "event123";

    @Before
    public void setUp() {
        repository = mock(EntrantListRepositoryImpl.class);
        mockRepoLiveData = new MutableLiveData<>();

        // The ViewModel now calls fetchEntrantsByStatus(id, NULL) in the constructor
        when(repository.fetchEntrantsByStatus(TEST_EVENT_ID, null))
                .thenReturn(mockRepoLiveData);

        // Constructor no longer takes status; it defaults to "waiting" internally
        viewModel = new EntrantListViewModel(repository, TEST_EVENT_ID);

        // MediatorLiveData needs an active observer to emit values during tests
        viewModel.getFilteredEntrants().observeForever(mock(Observer.class));
    }

    @Test
    public void test_initialization() {
        // Verify repository was called with NULL status to fetch all entrants
        verify(repository).fetchEntrantsByStatus(TEST_EVENT_ID, null);

        // Verify default status is "waiting"
        assertEquals("waiting", viewModel.getStatus().getValue());
    }

    @Test
    public void test_setFilterStatus() {
        // Act
        viewModel.setFilterStatus("accepted");

        // Assert
        assertEquals("accepted", viewModel.getStatus().getValue());
    }

    @Test
    public void test_notifyAllEntrants_null_list() {
        // Arrange: Repo returns null
        mockRepoLiveData.setValue(null);

        // Act
        viewModel.notifyAllEntrants("Hello");

        // Assert
        verify(repository, never()).notifyEntrant(anyString(), anyString(), anyString());
        verify(repository).setUserMessage("No entrants to notify.");
    }

    @Test
    public void test_notifyAllEntrants_empty_list() {
        // Arrange: Repo returns empty list
        mockRepoLiveData.setValue(new ArrayList<>());

        // Act
        viewModel.notifyAllEntrants("Hello");

        // Assert
        verify(repository, never()).notifyEntrant(anyString(), anyString(), anyString());
        verify(repository).setUserMessage("No entrants to notify.");
    }

    @Test
    public void test_notifyAllEntrants_empty_message() {
        // Arrange: Setup data that passes the filter
        Entrant e = new Entrant();
        e.setUserId("user1");
        e.setStatus("waiting"); // Must match default filter
        mockRepoLiveData.setValue(Arrays.asList(e));

        // Act
        viewModel.notifyAllEntrants("");

        // Assert
        verify(repository, never()).notifyEntrant(anyString(), anyString(), anyString());
        verify(repository).setUserMessage("No message provided.");
    }

    @Test
    public void test_notifyAllEntrants_valid() {
        String message = "Hello";

        // Setup mock entrants
        Entrant e1 = mock(Entrant.class);
        when(e1.getUserId()).thenReturn("user1");
        when(e1.getStatus()).thenReturn("waiting"); // Must match filter

        Entrant e2 = mock(Entrant.class);
        when(e2.getUserId()).thenReturn("user2");
        when(e2.getStatus()).thenReturn("waiting");

        // Setup entrant that should be IGNORED because status doesn't match
        Entrant eWrongStatus = mock(Entrant.class);
        when(eWrongStatus.getUserId()).thenReturn("user3");
        when(eWrongStatus.getStatus()).thenReturn("accepted");

        mockRepoLiveData.setValue(Arrays.asList(e1, eWrongStatus, e2));

        // Act
        viewModel.notifyAllEntrants(message);

        // Assert
        verify(repository).notifyEntrant("user1", TEST_EVENT_ID, message);
        verify(repository).notifyEntrant("user2", TEST_EVENT_ID, message);

        // Ensure user3 (wrong status) was NOT notified
        verify(repository, never()).notifyEntrant(eq("user3"), anyString(), anyString());
    }

    /**
     * Verifies that {@link EntrantListViewModel#cancelInvite(String)} calls the repository
     * with the correct parameters (null status) and triggers a success message upon success.
     */
    @Test
    public void test_cancelInvite_success() {
        String targetUserId = "userToCancel";

        // 1. Trigger the action
        viewModel.cancelInvite(targetUserId);

        ArgumentCaptor<StatusUpdateCallback> callbackCaptor = ArgumentCaptor.forClass(StatusUpdateCallback.class);

        // 2. Verify the call happened AND capture the callback
        verify(repository).updateEntrantStatus(
                eq(TEST_EVENT_ID),
                eq(targetUserId),
                eq("waiting"),
                callbackCaptor.capture(),
                eq(true)
        );

        // 3. Now that we captured it, simulate the Repository calling onSuccess
        callbackCaptor.getValue().onSuccess();

        // 4. Verify the ViewModel reacts correctly
        verify(repository).setUserMessage("User returned to waitlist and notified.");

        // fetchEntrantsByStatus is called once in constructor, and once after success = 2 times total
        verify(repository, times(2)).fetchEntrantsByStatus(TEST_EVENT_ID, null);
    }

    /**
     * Verifies that {@link EntrantListViewModel#cancelInvite(String)} calls the repository
     * with the correct parameters ("waiting" status) and triggers a success message upon success.
     */
    @Test
    public void test_cancelInvite_nullUserId() {
        viewModel.cancelInvite(null);
        verify(repository, never()).updateEntrantStatus(anyString(), anyString(), anyString(), any(), anyBoolean());
    }


    /**
     * Verifies that {@link EntrantListViewModel#cancelInvite(String)} calls the repository
     * with the correct parameters (null status) and triggers a failure message upon failure.
     */
    @Test
    public void test_cancelInvite_failure() {
        String targetUserId = "userToCancel";

        // 1. Trigger the action
        viewModel.cancelInvite(targetUserId);

        ArgumentCaptor<StatusUpdateCallback> callbackCaptor = ArgumentCaptor.forClass(StatusUpdateCallback.class);

        // 2. Verify the update was called and capture the callback
        verify(repository).updateEntrantStatus(
                eq(TEST_EVENT_ID),
                eq(targetUserId),
                eq("waiting"),
                callbackCaptor.capture(),
                eq(true)
        );

        callbackCaptor.getValue().onFailure(new Exception("Firestore error"));

        verify(repository, times(1)).fetchEntrantsByStatus(anyString(), any());

        verify(repository, never()).setUserMessage("User returned to waitlist and notified.");
    }

    /**
     * Test that {@link EntrantListViewModel#getCapitalizedStatus()} correctly
     * capitalizes the status string.
     */
    @Test
    public void test_getCapitalizedStatus() {
        // Default is "waiting"
        assertEquals("Waiting", viewModel.getCapitalizedStatus());

        viewModel.setFilterStatus("accepted");
        assertEquals("Accepted", viewModel.getCapitalizedStatus());
    }
}