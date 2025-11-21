package com.example.lotteryevent;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.repository.EntrantListRepository;
import com.example.lotteryevent.viewmodels.EntrantListViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for EntrantListViewModel.
 * These do not touch Android framework classes and run on the JVM.
 */
public class EntrantListViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private EntrantListRepository repository;
    private EntrantListViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(EntrantListRepository.class);
        viewModel = new EntrantListViewModel(repository);
    }

    /**
     * testing that correct status entrants are shown
     */
    @Test
    public void test_getEntrants_correct() {
        String eventId = "event123";
        String status = "accepted";

        MutableLiveData<List<Entrant>> repoLiveData = new MutableLiveData<>();
        when(repository.fetchEntrantsByStatus(eventId, status)).thenReturn(repoLiveData);

        LiveData<List<Entrant>> result = viewModel.getEntrants(eventId, status);

        // Verify repository method is called with correct arguments
        verify(repository).fetchEntrantsByStatus(eventId, status);

        // ViewModel should return exactly the same LiveData instance
        assert result == repoLiveData;
    }

    /**
     * If null entrant list, repository should not be called
     */
    @Test
    public void test_notifyAllEntrants_null() {
        viewModel.notifyAllEntrants(null, "event123", "Hello");

        verify(repository, never()).notifyEntrant(anyString(), anyString(), anyString());
    }

    /**
     * If empty entrant list, repository should not be called
     */
    @Test
    public void test_notifyAllEntrants_empty() {
        List<Entrant> entrants = new ArrayList<>();

        viewModel.notifyAllEntrants(entrants, "event123", "Hello");

        verify(repository, never()).notifyEntrant(anyString(), anyString(), anyString());
    }

    /**
     * when sending notifications, only everyone who has a user ID receives it
     */
    @Test
    public void test_notifyAllEntrants_valid() {
        String eventId = "event123";
        String message = "Hello";

        Entrant entrantWithId1 = mock(Entrant.class);
        when(entrantWithId1.getUserId()).thenReturn("user1");

        Entrant entrantWithId2 = mock(Entrant.class);
        when(entrantWithId2.getUserId()).thenReturn("user2");

        Entrant entrantWithoutId = mock(Entrant.class);
        when(entrantWithoutId.getUserId()).thenReturn(null);

        List<Entrant> entrants = Arrays.asList(
                entrantWithId1,
                null,
                entrantWithoutId,
                entrantWithId2
        );

        viewModel.notifyAllEntrants(entrants, eventId, message);

        // Only valid user ids should be used
        verify(repository).notifyEntrant("user1", eventId, message);
        verify(repository).notifyEntrant("user2", eventId, message);
        verify(repository, never()).notifyEntrant(Mockito.isNull(), Mockito.anyString(), Mockito.anyString());
    }
}
