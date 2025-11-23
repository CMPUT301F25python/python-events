package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.data.User;
import com.example.lotteryevent.repository.IUserRepository;
import com.example.lotteryevent.ui.UserProfileFragment;

/**
 * ViewModel for the UserProfile screen, responsible for preparing and managing the data for the UI
 * <p>
 *     This ViewModel provides the {@link UserProfileFragment} with the necessary data, such as the
 *     currently active user, loading status, and error messages. It acts as an intermediary between
 *     the UI and the data layer (represented by {@link IUserRepository}
 * </p>
 * This class is decoupled from the data source implementation, communicating only with the user
 * repository interface.
 */
public class UserProfileViewModel extends ViewModel {
    private final IUserRepository userRepository;

    public UserProfileViewModel(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // --- Data Exposure ---
    public LiveData<User> getCurrentUser() {
        return userRepository.getCurrentUser();
    }

    public LiveData<Boolean> getNotifPreference() { return userRepository.getNotifPreference(); }

    public LiveData<Boolean> isLoading() {
        return userRepository.isLoading();
    }

    public LiveData<String> getMessage() {
        return userRepository.getMessage();
    }

    // --- User Actions ---

    /**
     * Validates user data and requests an update from the repository.
     * @param name The user's name.
     * @param email The user's email.
     * @param phone The user's phone number.
     * @return An error message if validation fails, otherwise null.
     */
    public String updateUserProfile(String name, String email, String phone) {
        String validationError = validateProfileInfo(name, email, phone);
        if (validationError != null) {
            return validationError;
        }

        User updatedUser = new User();
        updatedUser.setName(name);
        updatedUser.setEmail(email);
        updatedUser.setPhone(phone.isEmpty() ? null : phone);

        userRepository.updateUserProfile(updatedUser);
        return null; // Success
    }

    public void deleteUserProfile() {
        userRepository.deleteCurrentUser();
    }

    /**
     * Validates user profile information.
     *
     * @return The invalid field name, or a more descriptive error. Null if valid.
     */
    private String validateProfileInfo(String name, String email, String phone) {
        if (name.isEmpty()) {
            return "Name is required.";
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.isEmpty()) {
            return "A valid email is required.";
        }
        if (!phone.isEmpty()) {
            String phoneDigitsOnly = phone.replaceAll("\\D", "");
            if (phoneDigitsOnly.length() != 10) {
                return "Enter a valid 10-digit phone number.";
            }
        }
        return null; // Validation successful
    }

    public void updateNotifPreference(boolean enabled, boolean systemNotifEnabled, NotificationCustomManager notificationCustomManager) {
        userRepository.updateNotifPreference(enabled, systemNotifEnabled, notificationCustomManager);
    }
}
