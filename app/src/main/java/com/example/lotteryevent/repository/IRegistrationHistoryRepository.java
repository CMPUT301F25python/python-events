package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;

import com.example.lotteryevent.data.RegistrationHistoryItem;

import java.util.List;

/**
 * This class defines the contract for repositories responsible for retrieving a user's
 * registration history. Implementations of this interface retrieves
 * a user's registration history.
 */
public interface IRegistrationHistoryRepository {
    /**
     * Retrieves the current user's full registration history as a LiveData stream.
     * The returned list may represent event entries such as accepted, declined,
     * waitlisted, or other participation statuses.
     * @return a LiveData object containing a list of {@link RegistrationHistoryItem}
     *         representing the user's registration history
     */
    LiveData <List<RegistrationHistoryItem>> fetchRegistrationHistory();
    /**
     * Returns a LiveData stream used to communicate user-facing messages such as
     * errors, confirmations, or information related to registration history
     * loading operations.
     * @return a LiveData object holding message strings to be displayed by the UI
     */
    LiveData<String> getUserMessage();
}
