package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.repository.IEntrantListRepository;

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

    /**
     * Creates a new ViewModel instance and injects the repository used for fetching
     * entrant information and sending notifications.
     * @param entrantListRepo the repository implementation responsible for Firestore
     *                        data access and notification dispatching
     */
    public EntrantListViewModel(IEntrantListRepository entrantListRepo) {
        this.entrantListRepo = entrantListRepo;
    }

    /**
     * Retrieves a LiveData stream of entrants for the specified event and status.
     * The underlying repository handles Firestore operations and posts results
     * asynchronously to the returned LiveData.
     * @param eventId the unique identifier of the event whose entrants should be
     *                retrieved
     * @param status the status filter used to determine which entrants are returned
     *               (accepted, cancelled, waiting, invited)
     * @return a LiveData object containing a list of entrants matching the criteria
     */
    public LiveData<List<Entrant>> getEntrants(String eventId, String status) {
        return entrantListRepo.fetchEntrantsByStatus(eventId, status);
    }

    /**
     * Sends a notification message to all entrants in the provided list. Iterates
     * through each entrant and delegates the notification sending to the repository.
     * Only valid entrants with non-null user IDs are processed.
     * @param entrants the list of entrants who should receive the notification
     * @param eventId the event identifier associated with the notification
     * @param organizerMessage the message content written by the event organizer
     */
    public void notifyAllEntrants(List<Entrant> entrants, String eventId, String organizerMessage) {
        // if empty list, return immediately without even trying to send notifications
        if (entrants == null || entrants.isEmpty()){
            return;
        }

        /**
         * Loop callback executed for each entrant in the notification list. Ensures the
         * entrant object and its user ID are non-null before delegating the notification
         * request to the repository.
         * @param e the entrant currently being processed for notification sending
         */
        for (Entrant e : entrants) {
            if (e != null && e.getUserId() != null) {
                entrantListRepo.notifyEntrant(e.getUserId(), eventId, organizerMessage);
            }
        }
    }
}
