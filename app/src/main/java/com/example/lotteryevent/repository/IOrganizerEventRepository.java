package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;

import java.util.List;

public interface IOrganizerEventRepository {
    LiveData<Event> getEvent();
    LiveData<List<Entrant>> getEntrants();
    LiveData<Boolean> isRunDrawButtonEnabled();
    LiveData<Boolean> isLoading();
    LiveData<String> getMessage();

    /**
     * Fetches event details and capacity status from the database.
     * @param eventId the ID of the event to fetch
     */
    void fetchEventAndCapacityStatus(String eventId);

    /**
     * Updates the poster image (Base64) for the specified event.
     * @param eventId The ID of the event to update.
     * @param posterImageUrl The Base64-encoded poster image data.
     */
    void updateEventPoster(String eventId, String posterImageUrl);
     * Finalizes an event by updating its status in the database.
     * @param eventId the ID of the event to finalize
     */
    void finalizeEvent(String eventId);

    /**
     * Fetches the full list of entrants for the specific event.
     * @param eventId the ID of the event
     */
    void fetchEntrants(String eventId);
}
