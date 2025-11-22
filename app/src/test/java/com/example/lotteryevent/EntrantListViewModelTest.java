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
import com.example.lotteryevent.repository.EntrantListRepositoryImpl;
import com.example.lotteryevent.viewmodels.EntrantListViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
 * @author Sanaa Bhaidani
 * @version 1.0
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

    /**
     * Creates a mocked {@link EntrantListRepositoryImpl} and injects it into a new
     * {@link EntrantListViewModel} instance before each test. Ensures each test
     * starts with a clean and isolated environment.
     */
    @Before
    public void setUp() {
        repository = mock(EntrantListRepositoryImpl.class);
        mockRepoLiveData = new MutableLiveData<>();
        when(repository.fetchEntrantsByStatus("event123", "accepted"))
                .thenReturn(mockRepoLiveData);

        viewModel = new EntrantListViewModel(repository, "event123", "accepted");
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
        verify(repository).fetchEntrantsByStatus("event123", "accepted");

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
        String eventId = "event123";
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
        verify(repository).notifyEntrant("user1", eventId, message);
        verify(repository).notifyEntrant("user2", eventId, message);
        verify(repository, never()).notifyEntrant(Mockito.isNull(), Mockito.anyString(), Mockito.anyString());
    }
}
