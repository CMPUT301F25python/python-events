package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;

import com.example.lotteryevent.data.Event;

public interface IOrganizerEventRepository {
    LiveData<Event> getEvent();
    LiveData<Boolean> isRunDrawButtonEnabled();
    LiveData<Boolean> isLoading();
    LiveData<String> getMessage();

    /**
     * Fetches event details and capacity status from the database.
     * @param eventId the ID of the event to fetch
     */
    void fetchEventAndCapacityStatus(String eventId);

    /**
     * Finalizes an event by updating its status in the database.
     * @param eventId the ID of the event to finalize
     */
    void finalizeEvent(String eventId);
}
