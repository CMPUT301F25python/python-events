package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.data.User;

/**
 * A fake implementation of {@link IUserRepository} for testing purposes.
 * <p>
 * This class simulates the behavior of the real repository by holding data in memory.
 * It allows for predictable and controllable responses, making it ideal for testing
 * ViewModels in isolation from Firebase.
 * <p>
 * It includes methods like {@link #setShouldReturnError(boolean)} to allow tests
 * to simulate failure scenarios.
 */
public class FakeUserRepository implements IUserRepository {

    // --- LiveData to be observed by the ViewModel ---
    private final MutableLiveData<User> _currentUser = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();

    private final MutableLiveData<Boolean> _notifPreference = new MutableLiveData<>();

    private final MutableLiveData<String> _userMessage = new MutableLiveData<>();

    // --- In-memory data source ---
    private User currentUserData;

    // --- Test control flags ---
    private boolean shouldReturnError = false;

    /**
     * Initializes the fake repository with a default user.
     */
    public FakeUserRepository() {
        // Create a default user to simulate a logged-in state.
        currentUserData = new User();
        currentUserData.setName("Joe Bill");
        currentUserData.setEmail("jabba.test@example.com");
        currentUserData.setPhone("123-456-7890");
        _currentUser.setValue(currentUserData);
    }

    @Override
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    @Override
    public LiveData<Boolean> getNotifPreference() { return _notifPreference; }

    @Override
    public LiveData<String> getMessage() {
        return _userMessage;
    }

    @Override
    public LiveData<User> getCurrentUser() {
        return _currentUser;
    }

    @Override
    public void updateUserProfile(User user) {
        _isLoading.setValue(true);

        // Simulate an error if the test requires it.
        if (shouldReturnError) {
            _userMessage.postValue("Profile update failed: Network error.");
            _isLoading.setValue(false);
            return;
        }

        // Simulate a successful update.
        currentUserData = user;
        _currentUser.postValue(currentUserData);
        _userMessage.postValue("Profile updated successfully.");
        _isLoading.setValue(false);
    }

    @Override
    public void deleteCurrentUser() {
        _isLoading.setValue(true);

        // Simulate an error if the test requires it.
        if (shouldReturnError) {
            _userMessage.postValue("Failed to delete all user data.");
            _isLoading.setValue(false);
            return;
        }

        // Simulate a successful deletion.
        currentUserData = null;
        _currentUser.postValue(null);
        _userMessage.postValue("Profile successfully deleted.");
        _isLoading.setValue(false);
    }

    @Override
    public void updateNotifPreference(Boolean enabled, boolean systemNotifPreference, NotificationCustomManager notificationCustomManager) {
        _notifPreference.postValue(enabled && systemNotifPreference);
    }

    // --- Test Control Methods ---

    /**
     * A helper method for tests to force this repository into an error state.
     * @param shouldError True to make subsequent calls fail, false to make them succeed.
     */
    public void setShouldReturnError(boolean shouldError) {
        this.shouldReturnError = shouldError;
    }

    /**
     * A helper method for tests to manually set the current user,
     * for example, to simulate a scenario where there is no user logged in.
     * @param user The user to set, can be null.
     */
    public void setCurrentUser(User user) {
        this.currentUserData = user;
        this._currentUser.postValue(user);
    }

}