package com.example.lotteryevent.viewmodels; // Using your established package name

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import com.example.lotteryevent.repository.IAvailableEventsRepository;
import com.example.lotteryevent.data.Event;
import java.util.List;

/**
 * ViewModel for the admin's 'Sent Notifications' event selection screen.
 * Displays a list of all events for the admin to choose from.
 */
public class AdminSentNotiViewModel extends ViewModel {

    private final IAvailableEventsRepository eventsRepository;
    private final LiveData<List<Event>> allEvents;

    public AdminSentNotiViewModel(IAvailableEventsRepository repository) {
        this.eventsRepository = repository;
        repository.fetchAvailableEvents();
        this.allEvents = repository.getAvailableEvents();
    }

    public LiveData<List<Event>> getAllEvents() {
        return allEvents;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        eventsRepository.removeListener();
    }
}