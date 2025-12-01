package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.repository.IEntrantListRepository;

import java.util.List;

/**
 * ViewModel for managing entrant data for the map view
 */
public class EntrantMapViewModel extends ViewModel {

    private final LiveData<List<Entrant>> entrants;
    private final IEntrantListRepository entrantRepository;
    public LiveData<String> message;

    /**
     * EntrantMapViewModel constructor
     * @param entrantRepository IEntrantListRepository
     * @param eventId String event id
     */
    public EntrantMapViewModel(IEntrantListRepository entrantRepository, String eventId) {
        this.entrantRepository = entrantRepository;
        this.entrants = entrantRepository.fetchAllEntrants(eventId);
        this.message = entrantRepository.getUserMessage();
    }

    /**
     * Gets all entrants
     *
     * @return LiveData<List<Entrant>> list of entrants
     */
    public LiveData<List<Entrant>> getEntrants() {
        return entrants;
    }

    /**
     * Gets the user message
     *
     * @return LiveData<String> message
     */
    public LiveData<String> getMessage() {
        return message;
    }
}
