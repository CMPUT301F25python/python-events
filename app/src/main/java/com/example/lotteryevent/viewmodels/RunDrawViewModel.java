package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.repository.IRunDrawRepository;

public class RunDrawViewModel extends ViewModel {

    private final IRunDrawRepository repository;

    // LiveData for the Fragment
    public LiveData<Integer> waitingListCount;
    public LiveData<Integer> selectedCount;
    public LiveData<Integer> availableSpaceCount;
    public LiveData<Boolean> isLoading;
    public LiveData<String> message;
    public LiveData<Boolean> drawSuccess;

    public RunDrawViewModel(IRunDrawRepository repo) {
        this.repository = repo;

        waitingListCount = repo.getWaitingListCount();
        selectedCount = repo.getSelectedCount();
        availableSpaceCount = repo.getAvailableSpaceCount();
        isLoading = repo.isLoading();
        message = repo.getMessage();
        drawSuccess = repo.getDrawSuccess();
    }

    // Loads all metrics
    public void loadMetrics(String eventId) {
        repository.loadMetrics(eventId);
    }

    // Runs draw and updates entrant statuses
    public void runDraw(String eventId, int numToSelect) {
        repository.runDraw(eventId, numToSelect);
    }
}
