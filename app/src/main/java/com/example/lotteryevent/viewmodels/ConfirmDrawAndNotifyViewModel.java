package com.example.lotteryevent.viewmodels;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.BottomUiState;
import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.IEventRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ViewModel for ConfirmDrawAndNotifyFragment
 * This class handles the logic behind allowing the organizer to view the number of entrants drawn, confirm draw, and notify the selected entrants.
 */
public class ConfirmDrawAndNotifyViewModel extends ViewModel {
    private final NotificationCustomManager notifManager;
    private final IEventRepository repository;

    public LiveData<Event> event;

    public LiveData<String> message;

    private final MediatorLiveData<String> _waitingListCount = new MediatorLiveData<>();
    private final MediatorLiveData<String> _availableSpaceCount = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> _navigateBack = new MutableLiveData<>();

    public LiveData<Boolean> navigateBack = _navigateBack;
    public LiveData<String> waitingListCount = _waitingListCount;
    public LiveData<String> availableSpaceCount = _availableSpaceCount;

    public LiveData<Boolean> isLoading() {
        return repository.isLoading();
    }

    public LiveData<String> getMessage() {
        return repository.getMessage();
    }

    // This MediatorLiveData observes other LiveData objects
    // and calculates a new UI state whenever one of them changes.
    private final MediatorLiveData<BottomUiState> _bottomUiState = new MediatorLiveData<>();
    public LiveData<BottomUiState> bottomUiState = _bottomUiState;

    /**
     * Sets up view model, initializing repository, live data, mediator live data's observers
     * @param repository repository for fetching and setting data
     * @param notificationCustomManager notificationCustomManager created in fragment needed bc
     *                                  fragment should take care of providing context
     */
    public ConfirmDrawAndNotifyViewModel(IEventRepository repository, NotificationCustomManager notificationCustomManager) {
        notifManager = notificationCustomManager;

        this.repository = repository;
        this.event = repository.getUserEvent();
        this.message = repository.getMessage();

        /**
         * Recalculates entrant counts whenever the waiting list count updates
         * @param event The updated waiting list count
         */
        _waitingListCount.addSource(repository.getWaitingListCount(), event -> calculateEntrantCounts());
        /**
         * Recalculates entrant counts whenever the available space count updates
         * @param event The updated avail space count
         */
        _availableSpaceCount.addSource(repository.getAvailableSpaceCount(), event -> calculateEntrantCounts());

        /**
         * Recalculates ui state when user event changes
         * @param event The updated user event
         */
        _bottomUiState.addSource(repository.getUserEvent(), event -> calculateUiState());
        /**
         * Recalculates entrant counts and ui state when loading boolean changes
         * @param isLoading The updated loading boolean value
         */
        _bottomUiState.addSource(repository.isLoading(), isLoading -> {
            calculateEntrantCounts();
            calculateUiState();
        });
        /**
         * Recalculates ui state when available space count changes
         * @param count the available space count
         */
        _bottomUiState.addSource(repository.getAvailableSpaceCount(), count -> calculateUiState());
        /**
         * Recalculates ui state when waiting list count changes
         * @param count the waiting list count
         */
        _bottomUiState.addSource(repository.getWaitingListCount(), count -> calculateUiState());
    }

    /**
     * The entry point for the Fragment to start loading event and entrants
     */
    public void loadEventAndEntrantCounts(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            _bottomUiState.setValue(BottomUiState.infoText("Error: Missing Event ID."));
            return;
        }
        repository.fetchEventAndEntrantCounts(eventId);
    }

    /**
     * Calculates and updates the waiting list, selected users list, and available space left list
     */
    private void calculateEntrantCounts() {
        Boolean isLoading = repository.isLoading().getValue();
        Event event = repository.getUserEvent().getValue();
        Integer waitingListCount = repository.getWaitingListCount().getValue();
        Integer availableSpaceCount = repository.getAvailableSpaceCount().getValue();

        if (isLoading != null && isLoading) {
            return;
        }
        if (event == null) {
            return;
        }

        if (waitingListCount != null) {
            _waitingListCount.postValue(String.valueOf(waitingListCount));
        }

        if (availableSpaceCount == null) {
            _availableSpaceCount.postValue("No Limit");
        } else if (availableSpaceCount > 0) {
            _availableSpaceCount.postValue(String.valueOf(availableSpaceCount));
        } else {
            _availableSpaceCount.postValue(String.valueOf(0));
        }
    }

    /**
     * Determines the UI state on screen depending on fetching data performance
     */
    private void calculateUiState() {
        Event event = repository.getUserEvent().getValue();
        Boolean isLoading = repository.isLoading().getValue();
        if (isLoading != null && isLoading) {
            _bottomUiState.setValue(BottomUiState.loading());
            return;
        }
        if (event == null) {
            return;
        }
        _bottomUiState.postValue(BottomUiState.twoButtons("Confirm and Notify", "Cancel"));
    }

    /**
     * Notifies selected entrants to accept, notifies of lottery loss, navigates back to event details screen
     */
    public void onPositiveButtonClicked(ArrayList<String> newChosenEntrants, ArrayList<String> newUnchosenEntrants) {
        if (notifManager == null) {
            _bottomUiState.setValue(BottomUiState.infoText("Error: Notifications not set up."));
            return;
        }
        String eventId = Objects.requireNonNull(event.getValue()).getEventId();
        ArrayList<Task<DocumentReference>> tasks = new ArrayList<>();

        for (String entrant : newChosenEntrants) {
            String eventName = Objects.requireNonNull(event.getValue()).getName();
            String organizerId = event.getValue().getOrganizerId();
            String organizerName = event.getValue().getOrganizerName();
            String title = "Congratulations!";
            String message = "You've been selected for " + eventName + "! Tap to accept or decline.";
            String type = "lottery_win";
            Task<DocumentReference> task = notifManager.sendNotification(entrant, title, message, type, eventId, eventName, organizerId, organizerName);
            tasks.add(task);
        }

        for (String entrant : newUnchosenEntrants) {
            String eventName = Objects.requireNonNull(event.getValue()).getName();
            String organizerId = event.getValue().getOrganizerId();
            String organizerName = event.getValue().getOrganizerName();
            String title = "Thank you for joining!";
            String message = "You weren't selected for " + eventName + " in this draw, but you're still on the waiting list and may be chosen in a future redraw.";
            String type = "lottery_loss";
            Task<DocumentReference> task = notifManager.sendNotification(entrant, title, message, type, eventId, eventName, organizerId, organizerName);
            tasks.add(task);
        }

        Tasks.whenAllComplete(tasks)
            /**
             * Navigates back when all tasks complete
             * @param allTask contains all tasks
             */
            .addOnCompleteListener(allTask -> {
                _navigateBack.postValue(true);
            });
    }

    /**
     * Moves invited entrants back to waiting list and navigates back to event details screen
     */
    public void onNegativeButtonClicked(Map<String, String> oldEntrantsStatus) {
        ArrayList<Task<Void>> tasks = new ArrayList<>();
        for (Map.Entry<String, String> entry : oldEntrantsStatus.entrySet()) {
            Task<Void> task = repository.updateEntrantAttribute(Objects.requireNonNull(event.getValue()).getEventId(), entry.getKey(), "status", entry.getValue());
            tasks.add(task);
        }
        Tasks.whenAllComplete(tasks)
            /**
             * Navigates back when all tasks complete
             * @param allTask contains all tasks
             */
            .addOnCompleteListener(allTask -> {
                _navigateBack.postValue(true);
            });
    }
}
