package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;

/**
 * Interface for the repository that handles data for the Event Details screen.
 * This contract defines all the necessary operations for fetching details of a single event
 * and managing the current user's interaction with it (e.g., joining, leaving, accepting).
 */
public interface IEventDetailsRepository {

    /**
     * Exposes the details of the currently loaded event as LiveData.
     * The value will be an Event object on success, or null otherwise.
     */
    LiveData<Event> getEventDetails();

    /**
     * Exposes the current user's entrant status for this event as LiveData.
     * The value will be an Entrant object if the user is involved with the event,
     * or null if they are not logged in or not an entrant.
     */
    LiveData<Entrant> getEntrantStatus();

    /**
     * Exposes the current loading state as a boolean LiveData.
     * True if any operation is in progress, false otherwise.
     */
    LiveData<Boolean> isLoading();

    /**
     * Exposes user-facing messages, such as success confirmations or errors, as LiveData.
     */
    LiveData<String> getMessage();

    /**
     * Kicks off the process of fetching both the event's main details and the
     * current user's entrant status from the data source.
     *
     * @param eventId The unique identifier of the event to load.
     */
    void fetchEventAndEntrantDetails(String eventId);

    /**
     * Adds the current user to the event's waiting list.
     * The result of this operation will be reflected in the getMessage() LiveData.
     *
     * @param eventId The ID of the event to join.
     */
    void joinWaitingList(String eventId);

    /**
     * Removes the current user from the event's waiting list.
     *
     * @param eventId The ID of the event to leave.
     */
    void leaveWaitingList(String eventId);

    /**
     * Updates the current user's invitation status (e.g., to "accepted" or "declined").
     *
     * @param eventId The ID of the event.
     * @param newStatus The new status string to set.
     */
    void updateInvitationStatus(String eventId, String newStatus);

    /**
     * Exposes the current count of attendees (status = "accepted") for the event.
     */
    LiveData<Integer> getAttendeeCount();

    /**
     * Exposes the current count of entrants on the waiting list (status = "waiting").
     */
    LiveData<Integer> getWaitingListCount();
}