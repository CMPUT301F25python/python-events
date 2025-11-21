package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;

public interface IRunDrawRepository {

    LiveData<Integer> getWaitingListCount();
    LiveData<Integer> getAvailableSpaceCount();

    LiveData<Boolean> isLoading();
    LiveData<String> getMessage();

    LiveData<Boolean> getDrawSuccess();
    void loadMetrics(String eventId);

    void runDraw(String eventId, int numToSelect);
}
