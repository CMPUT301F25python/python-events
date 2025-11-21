package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.IAvailableEventsRepository;
import com.example.lotteryevent.ui.AvailableEventsFragment;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 }
 * ViewModel for the Available Events screen, responsible for preparing and managing the data for the UI.
 * <p>
 * This ViewModel provides the {@link AvailableEventsFragment} with the necessary data,
 * such as a list of available. It acts as an intermediary between the UI and the data layer (represented
 * by {@link IAvailableEventsRepository}).
 * <p>
 * By using a ViewModel, the UI data survives configuration changes (like screen rotations).
 * This class is decoupled from the specific data source implementation, communicating only with the
 * {@link IAvailableEventsRepository} interface. This design follows the principles of dependency injection,
 * making the ViewModel more testable and maintainable.
 */
public class AvailableEventsViewModel extends ViewModel{

    private final IAvailableEventsRepository availableEventsRepository;

    /**
     * Constructs a HomeViewModel. The repository is "injected" through the constructor,
     * which allows for better testing and follows dependency inversion principles.
     *
     * @param availableEventsRepository An implementation of IAvailableEventsRepository that this ViewModel will use
     * to interact with the data layer.
     */
    public AvailableEventsViewModel(IAvailableEventsRepository availableEventsRepository) {
        this.availableEventsRepository = availableEventsRepository;
    }

    /**
     * Exposes the LiveData of the list of available events from the repository.
     * The UI can observe this LiveData to get real-time updates.
     *
     * @return A LiveData object containing a list of {@link Event}s.
     */
    public LiveData<List<Event>> getEvents() {
        return availableEventsRepository.getAvailableEvents();
    }

    /**
     * Exposes the loading state from the repository.
     * The UI can observe this to show or hide loading indicators.
     *
     * @return A LiveData object containing a boolean that is true if data is loading, false otherwise.
     */
    public LiveData<Boolean> isLoading() {
        return availableEventsRepository.isLoading();
    }

    /**
     * Exposes  messages from the repository.
     * The UI can observe this to display error/success messages to the user.
     *
     * @return A LiveData object containing a message string.
     */
    public LiveData<String> getMessage() { return availableEventsRepository.getMessage(); }


    /**
     * Called by the UI to request a refresh of the event data.
     * This call is simply delegated to the repository.
     */
    public void fetchAvailableEvents() {
        availableEventsRepository.fetchAvailableEvents();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        availableEventsRepository.removeListener();
    }
}
