package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.data.User;
import com.example.lotteryevent.repository.IAdminProfilesRepository;

import java.util.List;

/**
 * ViewModel responsible for managing and exposing user profile data to
 * UI for admin features.
 * This viewModel retrieves user profiles from repo and exposes results as LiveData
 * UI automtically updates when data changes, providing message updates as well
 */
public class AdminProfilesViewModel extends ViewModel {

    private final IAdminProfilesRepository repo;

    private final MutableLiveData<List<User>> _profiles = new MutableLiveData<>();
    private final MutableLiveData<String> _message = new MutableLiveData<>();

    public AdminProfilesViewModel(IAdminProfilesRepository repo) {
        this.repo = repo;
    }

    public LiveData<List<User>> getProfiles() { return _profiles; }
    public LiveData<String> getMessage() { return _message; }

    /**
     * Initiates request to fetch user profiles from repo
     * When successful, retrieved list is sent to _profiles
     */
    public void fetchProfiles() {
        repo.getAllProfiles(new IAdminProfilesRepository.ProfilesCallback() {
            @Override
            public void onSuccess(List<User> users) {
                _profiles.postValue(users);
            }

            @Override
            public void onFailure(Exception e) {
                _message.postValue("Failed to load profiles");
            }
        });
    }
}
