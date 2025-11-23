package com.example.lotteryevent.repository;

/**
 * This class defines the contract for repositories responsible for retrieving a user's
 * registration history. mplementations of this interface provides asynchronous access to user's
 * draw results and active waiting list via LiveData.
 */
public interface IRegistrationHistoryRepository {
    LiveData<RegistrationHistoryResult> fetchRegistrationHistory();

}
