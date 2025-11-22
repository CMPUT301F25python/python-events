package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;

import com.example.lotteryevent.data.Event;

public interface IOrganizerEventRepository {
    LiveData<Event> getEvent();
    LiveData<Boolean> isRunDrawButtonEnabled();
    LiveData<Boolean> isLoading();
    LiveData<String> getMessage();

    void fetchEventAndCapacityStatus(String eventId);
}
