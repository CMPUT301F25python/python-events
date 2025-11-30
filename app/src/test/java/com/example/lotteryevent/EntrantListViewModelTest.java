package com.example.lotteryevent;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void test_cancelInvite_success() {
        String targetUserId = "userToCancel";

        viewModel.cancelInvite(targetUserId);

        ArgumentCaptor<StatusUpdateCallback> callbackCaptor = ArgumentCaptor.forClass(StatusUpdateCallback.class);
        verify(repository).updateEntrantStatus(eq(TEST_EVENT_ID), eq(targetUserId), eq("waiting"), callbackCaptor.capture());

        // Simulate Success
        callbackCaptor.getValue().onSuccess();

        verify(repository).setUserMessage("User returned to waitlist.");

        // Verify re-fetch:
        // 1 call in constructor + 1 call in onSuccess = 2 total calls
        verify(repository, times(2)).fetchEntrantsByStatus(TEST_EVENT_ID, null);
    }

    @Test
    public void test_cancelInvite_nullUserId() {
        viewModel.cancelInvite(null);
        verify(repository, never()).updateEntrantStatus(anyString(), anyString(), anyString(), any());
    }

    @Test
    public void test_getCapitalizedStatus() {
        // Default is "waiting"
        assertEquals("Waiting", viewModel.getCapitalizedStatus());

        viewModel.setFilterStatus("accepted");
        assertEquals("Accepted", viewModel.getCapitalizedStatus());
    }
}