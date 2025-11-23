package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.data.RegistrationHistoryItem;
import com.example.lotteryevent.repository.IRegistrationHistoryRepository;

import java.util.List;

/**
 * This viewmodel exposes the user's event history.
 */
public class RegistrationHistoryViewModel extends ViewModel {
    private final IRegistrationHistoryRepository historyRepository;
    private final LiveData<List<RegistrationHistoryItem>> userHistory;

    public RegistrationHistoryViewModel(IRegistrationHistoryRepository historyRepository){
        this.historyRepository = historyRepository;
        this.userHistory = historyRepository.fetchRegistrationHistory();
    }

    public LiveData<List<RegistrationHistoryItem>> getUserHistory() {
        return userHistory;
    }

    public LiveData<String> getUserMessage(){
        return historyRepository.getUserMessage();
    }
}