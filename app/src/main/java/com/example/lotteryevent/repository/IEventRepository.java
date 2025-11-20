package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;
import java.util.List;

/**

 Interface for the Event Repository.
 This acts as a contract that defines 'what' data operations are possible for Events,
 but not 'how' they are implemented. The ViewModel will depend on this contract.
 */
public interface IEventRepository {

    /**

     Returns a LiveData object holding the list of events for the current user.
     The UI can observe this to get real-time updates.
     @return LiveData list of Events.
     */
    LiveData<List<Event>> getUserEvents();

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
     Triggers the process of fetching events from the data source for the currently logged-in user.
     */
    void fetchUserEvents();

    /**
     * Creates a new event in Firebase
     * The result (success or failure) will be posted to the message LiveData
     */
    void createEvent(Event event);

    /**
     * Returns a LiveData object holding a list of all entrants for an event based on status.
     * @param eventId id of the event
     * @param status status of the event (open, finalized)
     * @return LiveData list of entrants for an event
     */
    LiveData<List<Entrant>> fetchEntrantsByStatus(String eventId, String status);
}