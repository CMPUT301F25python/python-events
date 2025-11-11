package com.example.lotteryevent.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.lotteryevent.repository.IEventRepository;

/**
 * Factory for creating instances of HomeViewModel with a constructor that takes an IEventRepository.
 */
public class HomeViewModelFactory implements ViewModelProvider.Factory {

    private final IEventRepository eventRepository;

    public HomeViewModelFactory(IEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HomeViewModel.class)) {
            // If the requested ViewModel is of type HomeViewModel, create it
            // and pass the repository to its constructor.
            return (T) new HomeViewModel(eventRepository);
        }
        // Otherwise, throw an exception.
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}