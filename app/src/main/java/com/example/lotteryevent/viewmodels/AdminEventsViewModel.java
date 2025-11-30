package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.IAvailableEventsRepository;

import java.util.List;

/**
 * ViewModel used by AdminSelectEventFragment to load a list of ALL events.
 */
public class AdminEventsViewModel extends ViewModel {

    private final IAvailableEventsRepository repository;

    public AdminEventsViewModel(IAvailableEventsRepository repo) {
        this.repository = repo;
    }

    public LiveData<List<Event>> getEvents() {
        return repository.getAvailableEvents();
    }

    public LiveData<Boolean> isLoading() {
        return repository.isLoading();
    }

    public LiveData<String> getMessage() {
        return repository.getMessage();
    }

    public void fetchEvents() {
        repository.fetchAvailableEvents();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removeListener();
    }
}
