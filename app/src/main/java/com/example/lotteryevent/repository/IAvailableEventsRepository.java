package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;

import com.example.lotteryevent.data.Event;

import java.util.List;

public interface IAvailableEventsRepository {

    /**

     Returns a LiveData object holding the list of available events.
     The UI can observe this to get real-time updates.
     @return LiveData list of Events.
     */
    LiveData<List<Event>> getAvailableEvents();

    /**

     Returns a LiveData object holding the current loading state (true if loading, false otherwise).
     @return LiveData Boolean representing the loading state.
     */
    LiveData<Boolean> isLoading();

    /**
     Returns a LiveData object holding any messages (success or error) that occur during data fetching.
     @return LiveData String containing an message.
     */
    LiveData<String> getMessage();

    /**
     Triggers the process of fetching available events from the data source.
     */
    void fetchAvailableEvents();

    /**
     * Removes the listener used to fetch events from the data source.
     */
    void removeListener();

}