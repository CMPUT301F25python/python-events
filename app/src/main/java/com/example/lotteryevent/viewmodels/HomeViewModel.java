package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.IEventRepository;
import com.example.lotteryevent.ui.HomeFragment;

import java.util.List;

/**
 * ViewModel for the Home screen, responsible for preparing and managing the data for the UI.
 * <p>
 * This ViewModel provides the {@link HomeFragment} with the necessary data,
 * such as a list of lottery events, loading status, and error messages. It acts as an intermediary
 * between the UI and the data layer (represented by {@link IEventRepository}).
 * <p>
 * By using a ViewModel, the UI data survives configuration changes (like screen rotations).
 * This class is decoupled from the specific data source implementation, communicating only with the
 * {@link IEventRepository} interface. This design follows the principles of dependency injection,
 * making the ViewModel more testable and maintainable.
 */
public class HomeViewModel extends ViewModel {

    private final IEventRepository eventRepository;

    /**
     * Constructs a HomeViewModel. The repository is "injected" through the constructor,
     * which allows for better testing and follows dependency inversion principles.
     *
     * @param eventRepository An implementation of IEventRepository that this ViewModel will use
     *                        to interact with the data layer.
     */
    public HomeViewModel(IEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Exposes the LiveData of the list of user-created events from the repository.
     * The UI can observe this LiveData to get real-time updates.
     *
     * @return A LiveData object containing a list of {@link Event}s.
     */
    public LiveData<List<Event>> getEvents() {
        return eventRepository.getUserEvents();
    }

    /**
     * Exposes the loading state from the repository.
     * The UI can observe this to show or hide loading indicators.
     *
     * @return A LiveData object containing a boolean that is true if data is loading, false otherwise.
     */
    public LiveData<Boolean> isLoading() {
        return eventRepository.isLoading();
    }

    /**
     * Exposes error messages from the repository.
     * The UI can observe this to display error messages to the user.
     *
     * @return A LiveData object containing an error message string.
     */
    public LiveData<String> getError() {
        return eventRepository.getError();
    }

    /**
     * Called by the UI to request a refresh of the event data.
     * This call is simply delegated to the repository.
     */
    public void fetchUserEvents() {
        eventRepository.fetchUserEvents();
    }
}
