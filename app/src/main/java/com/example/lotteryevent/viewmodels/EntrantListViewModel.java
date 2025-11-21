package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.repository.IEntrantListRepository;

import java.util.List;
public class EntrantListViewModel extends ViewModel {

    private final IEntrantListRepository entrantListRepo;
    public EntrantListViewModel(IEntrantListRepository entrantListRepo) {
        this.entrantListRepo = entrantListRepo;
    }

    /**
     * Returns LiveData list of entrants for the given eventId and status.
     * The repository handles Firestore fetch and _entrants LiveData posting.
     */
    public LiveData<List<Entrant>> getEntrants(String eventId, String status) {
        return entrantListRepo.fetchEntrantsByStatus(eventId, status);
    }

    /**
     * Telling repository to send notifications to all entrants.
     */
    public void notifyAllEntrants(List<Entrant> entrants, String eventId, String organizerMessage) {
        if (entrants == null || entrants.isEmpty()) return;
        for (Entrant e : entrants) {
            if (e != null && e.getUserId() != null) {
                entrantListRepo.notifyEntrant(e.getUserId(), eventId, organizerMessage);
            }
        }
    }
}
