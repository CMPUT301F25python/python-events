package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.repository.IEntrantListRepository;
import com.example.lotteryevent.repository.IEntrantListRepository.StatusUpdateCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel responsible for providing entrant data and notification functionality
 * to the UI layer. This ViewModel delegates all data retrieval and notification
 * logic to an {@link IEntrantListRepository} implementation, ensuring that the UI
 * remains lifecycle-aware and reactive via LiveData.
 */

public class EntrantListViewModel extends ViewModel {

    private final IEntrantListRepository entrantListRepo;
    private final String eventId;
    private final MutableLiveData<String> filterStatus = new MutableLiveData<>("waiting");
    private final LiveData<List<Entrant>> allEntrantsSource;
    private final MediatorLiveData<List<Entrant>> displayedEntrants = new MediatorLiveData<>();

    /**
     * Creates a new ViewModel instance and injects the repository used for fetching
     * entrant information and sending notifications.
     * @param entrantListRepo the repository implementation responsible for Firestore
     *                        data access and notification dispatching
     * @param eventId the unique identifier of the event whose entrants should be
     *                retrieved
     */
    public EntrantListViewModel(IEntrantListRepository entrantListRepo, String eventId) {
        this.entrantListRepo = entrantListRepo;
        this.eventId = eventId;

        // Fetch ALL entrants
        this.allEntrantsSource = entrantListRepo.fetchEntrantsByStatus(eventId, null);
        // Setup Mediator to watch the DATA
        displayedEntrants.addSource(allEntrantsSource, entrants -> {
            combineDataAndFilter(entrants, filterStatus.getValue());
        });

        // Setup Mediator to watch the FILTER
        displayedEntrants.addSource(filterStatus, status -> {
            combineDataAndFilter(allEntrantsSource.getValue(), status);
        });
    }

    /**
     * Filters the list in memory
     * @param allEntrants the list of all entrants
     * @param status the status filter
     */
    private void combineDataAndFilter(List<Entrant> allEntrants, String status) {
        if (allEntrants == null) {
            displayedEntrants.setValue(new ArrayList<>());
            return;
        }

        // If no filter is selected (shouldn't happen), show all
        if (status == null) {
            displayedEntrants.setValue(allEntrants);
            return;
        }

        List<Entrant> filteredList = new ArrayList<>();

        // Perform the filtering in Java memory
        for (Entrant entrant : allEntrants) {
            if (entrant.getStatus() != null &&
                    entrant.getStatus().equalsIgnoreCase(status)) {
                filteredList.add(entrant);
            }
        }

        displayedEntrants.setValue(filteredList);
    }

    /**
     * Expose entrants LiveData from repository to the UI layer
     * @return the list of entrants
     */
    public LiveData<List<Entrant>> getFilteredEntrants() {
        return displayedEntrants;
    }

    /**
     * Expose status LiveData from repository to the UI layer
     * @return the filter status
     */
    public LiveData<String> getStatus() {
        return filterStatus;
    }

    /**
     * Expose message LiveData from repository to the UI layer
     * @return the user-facing message
     */
    public LiveData<String> getUserMessage() {
        return entrantListRepo.getUserMessage();
    }

    /**
     * Capitalizes the first letter of the status string.
     * @return the status string with the first letter capitalized
     */
    public String getCapitalizedStatus() {
        if (filterStatus.getValue() == null) return "";
        String s = filterStatus.getValue();
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Updates the filter immediately.
     */
    public void setFilterStatus(String newStatus) {
        // This triggers 'combineDataAndFilter' via the Mediator
        filterStatus.setValue(newStatus);
    }

    /**
     * Sends a notification message to all entrants in the provided list. Iterates
     * through each entrant and delegates the notification sending to the repository.
     * Only valid entrants with non-null user IDs are processed.
     * @param organizerMessage the message content written by the event organizer
     */
    public void notifyAllEntrants(String organizerMessage) {
        List<Entrant> currentList = displayedEntrants.getValue();

        if (currentList == null || currentList.isEmpty()) {
            entrantListRepo.setUserMessage("No entrants to notify.");
            return;
        }

        if (organizerMessage == null || organizerMessage.trim().isEmpty()) {
            entrantListRepo.setUserMessage("No message provided.");
            return;
        }

        for (Entrant e : currentList) {
            if (e != null && e.getUserId() != null) {
                entrantListRepo.notifyEntrant(e.getUserId(), this.eventId, organizerMessage);
            }
        }
    }

    /**
     * Cancels an invitation for a specific user.
     * Sets their status back to "waiting".
     * On success, it refreshes the list automatically via the repository logic (or we trigger a re-fetch).
     */
    public void cancelInvite(String userId) {
        if (userId == null) return;

        entrantListRepo.updateEntrantStatus(this.eventId, userId, "waiting", new StatusUpdateCallback() {
            /**
             * Message that entrant sent to waitlist and update list
             */
            @Override
            public void onSuccess() {
                entrantListRepo.setUserMessage("User returned to waitlist and notified.");
                entrantListRepo.fetchEntrantsByStatus(eventId, null);
            }

            /**
             * On exception do nothing for now
             * @param e exception thrown
             */
            @Override
            public void onFailure(Exception e) {
            }
        }, true);
    }
}
