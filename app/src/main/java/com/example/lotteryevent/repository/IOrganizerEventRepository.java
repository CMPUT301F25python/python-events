package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;

import com.example.lotteryevent.data.Event;

public interface IOrganizerEventRepository {
    LiveData<Event> getEvent();
    LiveData<Boolean> isRunDrawButtonEnabled();
    LiveData<Boolean> isLoading();
    LiveData<String> getMessage();

    void fetchEventAndCapacityStatus(String eventId);

    /**
     * Updates the poster image (Base64) for the specified event.
     * @param eventId The ID of the event to update.
     * @param posterImageUrl The Base64-encoded poster image data.
     */
    void updateEventPoster(String eventId, String posterImageUrl);
}
