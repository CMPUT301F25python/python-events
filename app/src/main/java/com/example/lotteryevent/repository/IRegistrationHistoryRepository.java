package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;

import com.example.lotteryevent.data.RegistrationHistoryItem;

/**
 * This class defines the contract for repositories responsible for retrieving a user's
 * registration history. Implementations of this interface retrieves
 * a user's registration history.
 */
public interface IRegistrationHistoryRepository {
    LiveData<RegistrationHistoryItem> fetchRegistrationHistory();
    LiveData<String> getUserMessage();
    void setUserMessage(String message);
}
