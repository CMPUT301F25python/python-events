package com.example.lotteryevent.viewmodels;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import androidx.navigation.Navigation;

import com.example.lotteryevent.BottomUiState;
import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.IEventRepository;
import com.example.lotteryevent.ui.ConfirmDrawAndNotifyFragmentDirections;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ViewModel for ConfirmDrawAndNotifyFragment
 * This class handles the logic behind allowing the organizer to view the number of entrants drawn, confirm draw, and notify the selected entrants.
 */
public class ConfirmDrawAndNotifyViewModel extends ViewModel {
    @SuppressLint("StaticFieldLeak")
    private View view;
    private final NotificationCustomManager notifManager;
    private final IEventRepository repository;
    public LiveData<Event> eventDetails;
    public LiveData<List<Entrant>> eventEntrants;
    public LiveData<String> message;

    private final MediatorLiveData<String> _waitingListCount = new MediatorLiveData<>();
    private final MediatorLiveData<String> _selectedUsersCount = new MediatorLiveData<>();
    private final MediatorLiveData<String> _availableSpaceCount = new MediatorLiveData<>();

    public LiveData<String> waitingListCount = _waitingListCount;
    public LiveData<String> selectedUsersCount = _selectedUsersCount;
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
     * @param context context from fragment
     * @param view view from fragment
     */
    public ConfirmDrawAndNotifyViewModel(IEventRepository repository, Context context, View view) {
        this.view = view;
        notifManager = new NotificationCustomManager(context);

        this.repository = repository;
        this.eventDetails = repository.getUserEvent();
        this.eventEntrants = repository.getEventEntrants();
        this.message = repository.getMessage();

        _waitingListCount.addSource(repository.getWaitingListCount(), event -> calculateEntrantCounts());
        _selectedUsersCount.addSource(repository.getSelectedUsersCount(), event -> calculateEntrantCounts());
        _availableSpaceCount.addSource(repository.getEventEntrants(), event -> calculateEntrantCounts());


        _bottomUiState.addSource(repository.getUserEvent(), event -> calculateUiState());
        _bottomUiState.addSource(repository.getEventEntrants(), event -> calculateUiState());
        _bottomUiState.addSource(repository.isLoading(), isLoading -> {
            calculateEntrantCounts();
            calculateUiState();
        });
        _bottomUiState.addSource(repository.getSelectedUsersCount(), count -> calculateUiState());
        _bottomUiState.addSource(repository.getWaitingListCount(), count -> calculateUiState());
    }

    /**
     * The entry point for the Fragment to start loading event and entrants
     */
    public void loadEventAndEntrants(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            _bottomUiState.setValue(BottomUiState.infoText("Error: Missing Event ID."));
            return;
        }
        repository.fetchEventAndEntrants(eventId);
    }

    /**
     * Calculates and updates the waiting list, selected users list, and available space left list
     */
    private void calculateEntrantCounts() {
        Boolean isLoading = repository.isLoading().getValue();
        Event event = repository.getUserEvent().getValue();
        Integer selectedUsersCount = repository.getSelectedUsersCount().getValue();
        Integer waitingListCount = repository.getWaitingListCount().getValue();

        if (isLoading != null && isLoading) {
            return;
        }

        if (waitingListCount == null) {
            return;
        }
        _waitingListCount.postValue(String.valueOf(waitingListCount));

        if (selectedUsersCount == null || event == null) {
            return;
        }
        _selectedUsersCount.postValue(String.valueOf(selectedUsersCount));

        Integer capacity = event.getCapacity();
        if (capacity == null) {
            _availableSpaceCount.postValue("No Limit");
        } else {
            Integer spaceLeft = capacity - selectedUsersCount;
            if (spaceLeft > 0) {
                _availableSpaceCount.postValue(String.valueOf(spaceLeft));
            } else {
                _availableSpaceCount.postValue("0");
            }
        }
    }

    /**
     * Determines the UI state on screen depending on fetching data performance
     */
    private void calculateUiState() {
        Event event = repository.getUserEvent().getValue();
        List<Entrant> entrants = repository.getEventEntrants().getValue();
        Boolean isLoading = repository.isLoading().getValue();
        if (isLoading != null && isLoading) {
            _bottomUiState.setValue(BottomUiState.loading());
            return;
        }
        if (event == null || entrants == null) {
            return;
        }
        _bottomUiState.postValue(BottomUiState.twoButtons("Confirm and Notify", "Cancel"));
    }

    /**
     * Notifies selected entrants to accept, navigates back to event details screen
     */
    public void onPositiveButtonClicked() {
        String eventId = Objects.requireNonNull(eventDetails.getValue()).getEventId();
        ArrayList<Task<DocumentReference>> tasks = new ArrayList<>();
        for (Entrant entrant : Objects.requireNonNull(eventEntrants.getValue())) {
            String eventName = Objects.requireNonNull(eventDetails.getValue()).getName();
            String organizerId = eventDetails.getValue().getOrganizerId();
            String organizerName = eventDetails.getValue().getOrganizerName();
            String title = "Congratulations!";
            String message = "You've been selected for " + eventName + "! Tap to accept or decline.";
            String type = "lottery_win";
            Task<DocumentReference> task = notifManager.sendNotification(entrant.getUserId(), title, message, type, eventId, eventName, organizerId, organizerName);
            tasks.add(task);
        }

        Tasks.whenAllComplete(tasks)
            .addOnCompleteListener(allTask -> {
                navigateBack();
            });
    }

    /**
     * Moves invited entrants back to waiting list and navigates back to event details screen
     */
    public void onNegativeButtonClicked() {
        repository.updateEntrantsAttributes(Objects.requireNonNull(eventDetails.getValue()).getEventId(), "status", "invited", "waiting");
        navigateBack();
    }

    /**
     * Navigate back to evetn details screen
     */
    private void navigateBack() {
        ConfirmDrawAndNotifyFragmentDirections.ActionConfirmDrawAndNotifyFragmentToOrganizerEventPageFragment action =
                ConfirmDrawAndNotifyFragmentDirections
                        .actionConfirmDrawAndNotifyFragmentToOrganizerEventPageFragment(Objects.requireNonNull(eventDetails.getValue()).getEventId());

        Navigation.findNavController(view).navigate(action);
    }
}
