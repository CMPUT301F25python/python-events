package com.example.lotteryevent.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.lotteryevent.repository.AdminUserProfileRepositoryImpl;
import com.example.lotteryevent.repository.EventDetailsRepositoryImpl;


/**
 * Triggers the deletion process via the ViewModel using the current {@code userId}.
 * <p>
 * This method is called only after the administrator confirms the action in the dialog.
 * </p>
 */
public class EventDetailsViewModelFactory implements ViewModelProvider.Factory {
    private final EventDetailsRepositoryImpl repository;
    private final AdminUserProfileRepositoryImpl userProfileRepository;

    /**
     * Constructs the factory and initializes the shared repository instance.
     *
     * @param application The application context (unused in this specific implementation but often required for standard factories).
     */
    public EventDetailsViewModelFactory(Application application) {
        this.repository = new EventDetailsRepositoryImpl();
        this.userProfileRepository = new AdminUserProfileRepositoryImpl();
    }

    /**
     * Creates a new instance of the given ViewModel class.
     *
     * @param modelClass The class of the ViewModel to create.
     * @param <T>        The type of the ViewModel.
     * @return A newly created instance of the ViewModel.
     * @throws IllegalArgumentException If the provided class is not assignable from {@link com.example.lotteryevent.viewmodels.EventDetailsViewModel}.
     */
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(EventDetailsViewModel.class)) {
            return (T) new EventDetailsViewModel(repository, userProfileRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}