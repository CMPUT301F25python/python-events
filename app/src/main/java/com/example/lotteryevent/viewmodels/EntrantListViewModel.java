package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
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
     * @param entrants the list of entrants who should receive the notification
     * @param eventId the event identifier associated with the notification
     * @param organizerMessage the message content written by the event organizer
     */
    public void notifyAllEntrants(List<Entrant> entrants, String eventId, String organizerMessage) {
        // if empty list, return immediately without even trying to send notifications
        if (entrants == null || entrants.isEmpty()){
            return;
        }

        if (organizerMessage == null || organizerMessage.trim().isEmpty()) {
            entrantListRepo.setUserMessage("No message provided.");
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

    /**
     * A utility method to capitalize the first letter of a given string.
     * Used for formatting the title of the page.
     *
     * @param str The string to capitalize.
     * @return The capitalized string, or an empty string if the input is null or empty.
     */
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Exposing capitalizeFirstLetter to the View
     * @param status the status filter used to determine which entrants are returned
     *        (accepted, cancelled, waiting, invited)
     * @return Status string with capitalized first letter
     */
    public String getCapitalizedStatus(String status) {
        return capitalizeFirstLetter(status);
    }
}
