package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.BottomUiState;
import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.IEventDetailsRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * ViewModel for the EventDetailsFragment.
 * This class is responsible for the business logic of determining what UI to show
 * based on the event's state and the user's status. It combines data streams from the
 * repository into a single, simple UI state that the Fragment can observe.
 */
public class EventDetailsViewModel extends ViewModel {
    private final IEventDetailsRepository repository;
    private String eventId;

    // --- LiveData exposed to the Fragment ---
    public LiveData<Event> eventDetails;
    public LiveData<String> message;

    // Signal to the Fragment that we need location permission
    private final MutableLiveData<Boolean> _requestLocationPermission = new MutableLiveData<>();
    public LiveData<Boolean> requestLocationPermission = _requestLocationPermission;

    // This MediatorLiveData observes other LiveData objects
    // and calculates a new UI state whenever one of them changes.
    private final MediatorLiveData<BottomUiState> _bottomUiState = new MediatorLiveData<>();
    public LiveData<BottomUiState> bottomUiState = _bottomUiState;
    public MutableLiveData<Boolean> isAdmin = new MutableLiveData<>(false);

    public EventDetailsViewModel(IEventDetailsRepository repository) {
        this.repository = repository;
        // Pass through the simple LiveData objects from the repository.
        this.eventDetails = repository.getEventDetails();
        this.message = repository.getMessage();

        // Add the sources that the UI state depends on.
        _bottomUiState.addSource(repository.getEventDetails(), event -> calculateUiState());
        _bottomUiState.addSource(repository.getEntrantStatus(), entrant -> calculateUiState());
        _bottomUiState.addSource(repository.isLoading(), isLoading -> calculateUiState());
        _bottomUiState.addSource(repository.getAttendeeCount(), count -> calculateUiState());
        _bottomUiState.addSource(repository.getWaitingListCount(), count -> calculateUiState());
    }

    /**
     * The entry point for the Fragment to start loading data.
     */
    public void loadEventDetails(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            _bottomUiState.setValue(BottomUiState.infoText("Error: Missing Event ID."));
            return;
        }
        this.eventId = eventId;
        repository.fetchEventAndEntrantDetails(eventId);
    }

    /**
     * Calculate UI state based on the current data.
     */
    private void calculateUiState() {
        Event event = repository.getEventDetails().getValue();
        Entrant entrant = repository.getEntrantStatus().getValue();
        Boolean isLoading = repository.isLoading().getValue();
        Integer attendeeCount = repository.getAttendeeCount().getValue();
        Integer waitingListCount = repository.getWaitingListCount().getValue();

        if (isLoading != null && isLoading) {
            _bottomUiState.setValue(BottomUiState.loading());
            return;
        }
        if (event == null || attendeeCount == null || waitingListCount == null) {
            return; // We don't have all the data we need yet.
        }

        boolean isEntrant = entrant != null;
        String entrantStatus = isEntrant ? entrant.getStatus() : null;
        String eventStatus = event.getStatus();
        Timestamp regStart = event.getRegistrationStartDateTime();
        Timestamp regEnd = event.getRegistrationEndDateTime();
        Timestamp now = Timestamp.now();


        if (isEntrant && "cancelled".equals(entrantStatus)) {
            _bottomUiState.postValue(BottomUiState.infoText("Your invitation was cancelled."));
            return;
        }
        if (isEntrant && "declined".equals(entrantStatus)) {
            _bottomUiState.postValue(BottomUiState.infoText("You have declined this invitation."));
            return;
        }
        if (isEntrant && "accepted".equals(entrantStatus)) {
            _bottomUiState.postValue(BottomUiState.infoText("Invitation accepted! You are attending."));
            return;
        }
        if ("finalized".equals(eventStatus)) {
            _bottomUiState.postValue(BottomUiState.infoText("This event is now closed."));
            return;
        }
        if ("open".equals(eventStatus)) {
            if (isEntrant) {
                if ("invited".equals(entrantStatus)) {
                    _bottomUiState.postValue(BottomUiState.twoButtons("Accept Invitation", "Decline Invitation"));
                } else {
                    _bottomUiState.postValue(BottomUiState.oneButton("Leave Waiting List"));
                }
            } else {
                if (regStart != null && now.compareTo(regStart) < 0) {
                    _bottomUiState.postValue(BottomUiState.infoText("Registration has not yet opened."));
                    return;
                }

                if (regEnd == null || now.compareTo(regEnd) < 0) {
                    Integer capacity = event.getCapacity();
                    Integer waitingListLimit = event.getWaitingListLimit();

                    if (capacity != null && attendeeCount >= capacity &&
                            waitingListLimit != null && waitingListCount >= waitingListLimit) {
                        _bottomUiState.postValue(BottomUiState.infoText("This event and its waiting list are full."));
                    } else {
                        _bottomUiState.postValue(BottomUiState.oneButton("Join Waiting List"));
                    }
                } else {
                    _bottomUiState.postValue(BottomUiState.infoText("Registration for this event is closed."));
                }
            }
        } else {
            // Fallback for any other status like "draft", "postponed", etc.
            _bottomUiState.postValue(BottomUiState.infoText("This event is not currently open."));
        }
    }

    // --- User Actions ---

    public void onPositiveButtonClicked() {
        BottomUiState currentState = _bottomUiState.getValue();
        if (currentState == null || currentState.positiveButtonText == null) return;

        // Based on the button text, decide which repository action to call.
        String action = currentState.positiveButtonText;
        if (action.equals("Join Waiting List")) {
            Event event = repository.getEventDetails().getValue();
            // Check if location is required
            if (event != null && event.getGeoLocationRequired()) {
                // Signal the Fragment to handle permission/GPS
                _requestLocationPermission.setValue(true);
            } else {
                // No location needed, join immediately with nulls
                repository.joinWaitingList(eventId, null, null);
            }
        }  else if (action.equals("Accept Invitation")) {
            repository.updateInvitationStatus(eventId, "accepted");
        } else if (action.equals("Leave Waiting List")) {
            repository.leaveWaitingList(eventId);
        }
    }

    public void onNegativeButtonClicked() {
        BottomUiState currentState = _bottomUiState.getValue();
        if (currentState == null || currentState.negativeButtonText == null) return;

        String action = currentState.negativeButtonText;
        if (action.equals("Decline Invitation")) {
            repository.updateInvitationStatus(eventId, "declined");
        }
    }

    /**
     * Exposes the administrative status of the current user.
     * The Fragment can observe this to determine if admin-only controls (like delete) should be visible.
     *
     * @return A LiveData Boolean that is true if the user is an admin, false otherwise.
     */
    public LiveData<Boolean> getIsAdmin() {
        return repository.getIsAdmin();
    }


    /**
     * Checks the current user's authentication status and queries the repository to see if they have admin privileges.
     * This should be called when the Fragment view is created to update the {@link #getIsAdmin()} LiveData.
     */
    public void checkAdminStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();if (user != null) {
            repository.checkAdminStatus(user.getUid());
        }
    }

    /**
     * Requests the deletion of a specific event through the repository.
     * This action is generally restricted to users with administrative privileges.
     *
     * @param eventId The unique ID of the event to be deleted.
     */
    public void deleteEvent(String eventId) {
        repository.deleteEvent(eventId);
    }

    /**
     * Exposes a status flag indicating whether the event has been successfully deleted.
     * The Fragment can observe this to trigger navigation away from the screen (e.g., popping the back stack).
     *
     * @return A LiveData Boolean that becomes true when the deletion operation completes successfully.
     */
    public LiveData<Boolean> getIsDeleted() {
        return repository.getIsDeleted();
    }


    // --- Location Methods ---
    /**
     * Called by the Fragment after it successfully retrieves the location.
     */
    public void onLocationRetrieved(double latitude, double longitude) {
        _requestLocationPermission.setValue(false);
        // Call repository with the new data
        repository.joinWaitingList(eventId, latitude, longitude);
    }

    /**
     * Called by the Fragment if the user denied permission.
     */
    public void onLocationPermissionDenied() {
        // Reset the signal
        _requestLocationPermission.setValue(false);
    }

    /**
     * This function exposes the waiting list count to the Fragment.
     * @return a LiveData<Integer> representing the waiting list count
     */
    public LiveData<Integer> getWaitingListCount(){
        return repository.getWaitingListCount();
    }
}