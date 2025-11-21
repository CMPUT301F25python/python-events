package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import com.example.lotteryevent.data.Entrant;
import java.util.List;

/**
 * Repository interface for fetching entrants and sending notifications.
 */
public interface IEntrantListRepository {

    /**
     * Fetches entrants for an event with a given status.
     *
     * @param eventId id of the event
     * @param status status of the entrant / event (open, finalized, accepted, etc.)
     * @return LiveData list of entrants
     */
    LiveData<List<Entrant>> fetchEntrantsByStatus(String eventId, String status);

    /**
     * Sends a notification to a single entrant.
     *
     * @param uid id of the entrant
     * @param eventId event id
     * @param organizerMessage content of the notification
     */
    void notifyEntrant(String uid, String eventId, String organizerMessage);
}

