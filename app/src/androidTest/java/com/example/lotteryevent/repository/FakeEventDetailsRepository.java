package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * A stateful fake implementation of IEventDetailsRepository for unit testing.
 * It holds an in-memory list of all entrants to accurately simulate count queries
 * and other database operations.
 */
public class FakeEventDetailsRepository implements IEventDetailsRepository {

    // --- LiveData to be observed by the ViewModel ---
    private final MutableLiveData<Event> _eventDetails = new MutableLiveData<>();
    private final MutableLiveData<Entrant> _entrantStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> _message = new MutableLiveData<>();
    // --- NEW: LiveData for the counts ---
    private final MutableLiveData<Integer> _attendeeCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> _waitingListCount = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isDeleted = new MutableLiveData<>(false);
    private boolean isAdmin = false;

    // --- In-memory "database" ---
    private Event inMemoryEvent;
    // This list simulates the entire 'entrants' subcollection.
    private final List<Entrant> inMemoryEntrants = new ArrayList<>();
    // We need a simulated user ID for the "current" user.
    private final String currentUserId = "test-user-id";

    // --- Test control flags ---
    private boolean shouldReturnError = false;

    public FakeEventDetailsRepository() {
        resetToDefaultState();
    }

    // --- Getters from the Interface ---
    @Override public LiveData<Event> getEventDetails() { return _eventDetails; }
    @Override public LiveData<Entrant> getEntrantStatus() { return _entrantStatus; }
    @Override public LiveData<Boolean> isLoading() { return _isLoading; }
    @Override public LiveData<String> getMessage() { return _message; }
    // --- NEW: Implement new getters ---
    @Override public LiveData<Integer> getAttendeeCount() { return _attendeeCount; }
    @Override public LiveData<Integer> getWaitingListCount() { return _waitingListCount; }

    @Override
    public void fetchEventAndEntrantDetails(String eventId) {
        _isLoading.postValue(true);
        if (shouldReturnError) {
            _message.postValue("Test Error: Could not fetch details.");
            _isLoading.postValue(false);
            return;
        }
        // Simulate a successful fetch by posting all current in-memory data.
        _eventDetails.postValue(inMemoryEvent);
        _entrantStatus.postValue(findCurrentUserEntrant());
        recalculateCountsAndPost(); // Calculate and post the counts.
        _isLoading.postValue(false);
    }

    @Override
    public void joinWaitingList(String eventId, Double latitude, Double longitude) {
        _isLoading.postValue(true);
        if (shouldReturnError) { /* ... error handling ... */ return; }

        // Don't add if already an entrant
        if (findCurrentUserEntrant() != null) {
            _isLoading.postValue(false);
            return;
        }

        Entrant newEntrant = new Entrant();
        newEntrant.setUserId(currentUserId); // Set the user ID
        newEntrant.setUserName("Test User");
        newEntrant.setStatus("waiting");
        newEntrant.setDateRegistered(Timestamp.now());

        if (latitude != null && longitude != null) {
            newEntrant.setGeoLocation(new GeoPoint(latitude, longitude));
        } else {
            newEntrant.setGeoLocation(null);
        }

        inMemoryEntrants.add(newEntrant); // Add to the full list
        _entrantStatus.postValue(newEntrant); // Update the current user's status
        recalculateCountsAndPost(); // Update the counts
        _message.postValue("Successfully joined the waiting list!");
        _isLoading.postValue(false);
    }

    @Override
    public void leaveWaitingList(String eventId) {
        _isLoading.postValue(true);
        if (shouldReturnError) { /* ... error handling ... */ return; }

        inMemoryEntrants.removeIf(e -> Objects.equals(e.getUserId(), currentUserId));
        _entrantStatus.postValue(null);
        recalculateCountsAndPost();
        _message.postValue("You have left the event.");
        _isLoading.postValue(false);
    }

    @Override
    public void updateInvitationStatus(String eventId, String newStatus) {
        _isLoading.postValue(true);
        if (shouldReturnError) { /* ... error handling ... */ return; }

        Entrant currentUserEntrant = findCurrentUserEntrant();
        if (currentUserEntrant != null) {
            currentUserEntrant.setStatus(newStatus);
            _entrantStatus.postValue(currentUserEntrant); // Post the updated object
            recalculateCountsAndPost();
            String successMsg = "accepted".equals(newStatus) ? "Invitation accepted!" : "Invitation declined.";
            _message.postValue(successMsg);
        }
        _isLoading.postValue(false);
    }

    // --- NEW: Helper method to simulate the count query ---
    private void recalculateCountsAndPost() {
        int attendees = 0;
        int waiting = 0;
        for (Entrant entrant : inMemoryEntrants) {
            if ("accepted".equals(entrant.getStatus())) {
                attendees++;
            } else if ("waiting".equals(entrant.getStatus())) {
                waiting++;
            }
        }
        _attendeeCount.postValue(attendees);
        _waitingListCount.postValue(waiting);
    }

    private Entrant findCurrentUserEntrant() {
        for (Entrant entrant : inMemoryEntrants) {
            if (Objects.equals(entrant.getUserId(), currentUserId)) {
                return entrant;
            }
        }
        return null;
    }

    // --- Test Control Methods ---
    public void setShouldReturnError(boolean shouldError) {
        this.shouldReturnError = shouldError;
    }

    public void setInitialEntrant(Entrant entrant) {
        // Ensure the test user ID is set for consistency
        if (entrant != null && entrant.getUserId() == null) {
            entrant.setUserId(currentUserId);
        }
        inMemoryEntrants.add(entrant);
    }

    public void resetToDefaultState() {
        shouldReturnError = false;

        inMemoryEvent = new Event();
        inMemoryEvent.setName("Default Test Event");
        inMemoryEvent.setStatus("open");
        inMemoryEvent.setRegistrationStartDateTime(new Timestamp(new Date(System.currentTimeMillis() - 100000)));
        inMemoryEvent.setRegistrationEndDateTime(new Timestamp(new Date(System.currentTimeMillis() + 100000)));

        inMemoryEntrants.clear();

        // Clear LiveData
        _eventDetails.postValue(null);
        _entrantStatus.postValue(null);
        _message.postValue(null);
        _attendeeCount.postValue(0);
        _waitingListCount.postValue(0);
    }

    /**
     * Helper for tests to directly access and modify the in-memory event.
     */
    public Event getInMemoryEvent() {
        return inMemoryEvent;
    }

    /**
     * Allows tests to add a specific Entrant state before a test runs.
     * @param entrant The Entrant object to add to the list.
     */
    public void addInitialEntrant(Entrant entrant) {
        if (entrant != null) {
            // Remove any existing entrant with the same ID to prevent duplicates
            inMemoryEntrants.removeIf(e -> Objects.equals(e.getUserId(), entrant.getUserId()));
            inMemoryEntrants.add(entrant);
        }
    }

    /**
     * Test Helper: Manually sets the admin status for the current test scenario.
     * @param isAdmin true to simulate an admin user, false for a regular user.
     */
    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    @Override
    public LiveData<Boolean> getIsAdmin() {
        return new MutableLiveData<>(isAdmin);
    }

    /**
     * Simulates the repository check for admin privileges.
     * In this fake, it simply relies on the flag set by {@link #setIsAdmin(boolean)}.
     */
    @Override
    public void checkAdminStatus(String userId) {
        // Pass: in the fake repository, the status is already determined by the isAdmin boolean
    }

    /**
     * Simulates the deletion of an event.
     * Sets the isDeleted LiveData to true to signal success.
     */
    @Override
    public void deleteEvent(String eventId) {
        // Simulate successful deletion immediately
        _isDeleted.postValue(true);
    }

    /**
     * Returns a LiveData indicating if the event has been successfully deleted.
     */
    @Override
    public LiveData<Boolean> getIsDeleted() {
        return _isDeleted;
    }

}