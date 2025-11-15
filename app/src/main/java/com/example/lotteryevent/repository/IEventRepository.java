package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
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
     Returns a LiveData object holding any error messages that occur during data fetching.
     @return LiveData String containing an error message.
     */
    LiveData<String> getError();

    /**
     Triggers the process of fetching events from the data source for the currently logged-in user.
     */
    void fetchUserEvents();
}