package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.repository.IEntrantListRepository;
import com.example.lotteryevent.repository.IEntrantListRepository.StatusUpdateCallback;

import java.util.List;

/**
 * ViewModel responsible for providing entrant data and notification functionality
 * to the UI layer. This ViewModel delegates all data retrieval and notification
 * logic to an {@link IEntrantListRepository} implementation, ensuring that the UI
 * remains lifecycle-aware and reactive via LiveData.
 * @author Sanaa Bhaidani
 * @version 1.0
 */

public class EntrantListViewModel extends ViewModel {

    private final IEntrantListRepository entrantListRepo;
    private final String eventId;
    private final String status;
    private final LiveData<List<Entrant>> entrants;

    /**
     * Creates a new ViewModel instance and injects the repository used for fetching
     * entrant information and sending notifications.
     * @param entrantListRepo the repository implementation responsible for Firestore
     *                        data access and notification dispatching
     * @param eventId the unique identifier of the event whose entrants should be
     *                retrieved
     * @param status the status filter used to determine which entrants are returned
     *               (accepted, cancelled, waiting, invited)
     */
    public EntrantListViewModel(IEntrantListRepository entrantListRepo, String eventId, String status) {
        this.entrantListRepo = entrantListRepo;
        this.eventId = eventId;
        this.status = status;
        this.entrants = entrantListRepo.fetchEntrantsByStatus(eventId, status);
    }

    public LiveData<List<Entrant>> getEntrants() {
        return entrants;
    }

    public String getCapitalizedStatus() {
        if (status == null || status.isEmpty()) return "";
        return status.substring(0, 1).toUpperCase() + status.substring(1);
    }

    /**
     * Expose message LiveData from repository to the UI layer
     * @return the user-facing message
     */
    public LiveData<String> getUserMessage() {
        return entrantListRepo.getUserMessage();
    }

    /**
     * Sends a notification message to all entrants in the provided list. Iterates
     * through each entrant and delegates the notification sending to the repository.
     * Only valid entrants with non-null user IDs are processed.
     * @param organizerMessage the message content written by the event organizer
     */
    public void notifyAllEntrants(String organizerMessage) {
        List<Entrant> currentList = entrants.getValue();

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
            @Override
            public void onSuccess() {
                entrantListRepo.setUserMessage("User returned to waitlist.");
                // Refresh the list
                entrantListRepo.fetchEntrantsByStatus(eventId, status);
            }

            @Override
            public void onFailure(Exception e) {
            }
        });
    }
}
