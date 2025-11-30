package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import com.example.lotteryevent.data.Entrant;
import java.util.List;

/**
 * This class defines the contract for repositories responsible for retrieving entrant data
 * and sending notifications related to events. Implementations of this
 * interface provide asynchronous access to entrant lists via LiveData as well
 * as methods to notify individual entrants with organizer-generated messages.
 */
public interface IEntrantListRepository {

    /**
     * Retrieves a list of entrants belonging to a specific event whose status
     * matches the provided value. The results are delivered asynchronously through
     * a LiveData stream so UI components can observe changes without manually
     * accessing the data source.
     * @param eventId the unique identifier of the event for which entrants are
     *                being fetched
     * @param status the status filter used to determine which entrants should be
     *               returned (accepted, waiting, cancelled, invited)
     * @return a LiveData object containing a list of entrants that match the
     *         requested status
     */

    LiveData<List<Entrant>> fetchEntrantsByStatus(String eventId, String status);

    /**
     * Sends a single entrant a notification message associated with a specific
     * event. The underlying implementation is responsible for preparing the
     * message, retrieving any required metadata, and dispatching the notification
     * through the appropriate notification system.
     * @param uid the unique identifier of the entrant to notify
     * @param eventId the unique identifier of the event associated with the
     *                notification
     * @param organizerMessage the custom text content that the event organizer wishes to
     *                         send to the entrant
     */
    void notifyEntrant(String uid, String eventId, String organizerMessage);

    /**
     * LiveData for user-facing messages
     * @return String with message
     */
    LiveData<String> getUserMessage();

    /**
     * Allows organizer to set a custom message
     * @param message set by the organizer
     */
    void setUserMessage(String message);

    /**
     * Updates the status of a specific entrant (e.g., from "invited" to "waiting").
     *
     * @param eventId   The event ID.
     * @param userId    The entrant's user ID.
     * @param newStatus The new status to set.
     * @param callback  A callback to handle success/failure in the ViewModel.
     * @param sendNotif boolean to indicate whether to notify user of a specific status change
     */
    void updateEntrantStatus(String eventId, String userId, String newStatus, StatusUpdateCallback callback, boolean sendNotif);

    /**
     * Simple callback interface for status updates.
     */
    interface StatusUpdateCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

}

