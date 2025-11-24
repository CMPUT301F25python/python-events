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

     Returns a LiveData object holding a specific event which is specified through calling fetchEventAndEntrants().
     The UI can observe this to get real-time updates.
     @return LiveData Event.
     */
    LiveData<Event> getUserEvent();

    /**

     Returns a LiveData object holding a list of entrants of a specific event which is specified through calling fetchEventAndEntrants().
     The UI can observe this to get real-time updates.
     @return LiveData list of Entrants.
     */
    LiveData<List<Entrant>> getEventEntrants();

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

     Returns a LiveData object holding the count of the number of entrants in the waiting list of a
     specified event which is specified through calling fetchEventAndEntrants().
     The UI can observe this to get real-time updates.
     @return LiveData Event.
     */
    LiveData<Integer> getWaitingListCount();

    /**

     Returns a LiveData object holding the count of the number of selected entrants invited from the
     waiting list of a specified event which is specified through calling fetchEventAndEntrants().
     The UI can observe this to get real-time updates.
     @return LiveData Event.
     */
    LiveData<Integer> getSelectedUsersCount();

    /**
     * Fetches an specified event and its entrants.
     * @param eventId The unique identifier of the event to load.
     */
    void fetchEventAndEntrants(String eventId);

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
     * Updates an attribute of the entrants of an event
     * @param eventId event to access its entrants
     * @param fieldName attribute of entrants to modify
     * @param oldValue old value for only updating specific entrants
     * @param newValue new value to set
     */
    void updateEntrantsAttributes(String eventId, String fieldName, Object oldValue, Object newValue);

    /**
     * Changes the status of an event to "finalized"
     * @param eventId event to finalize
     */
    void finalizeEvent(String eventId);
}