package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.repository.IRunDrawRepository;

/**
 * Viewmodel for RunDrawFragment
 *
 * <p>
 *     This viewmodel communicates statuses needed for running draw like:
 *     <li>Waiting list count</li>
 *     <li>Selected entrants count</li>
 *     <li>Available space</li>
 *     <li>Draw completion</li>
 *
 *     <p>
 *         Talks to IRunDrawRepository that also communicates with firestore utility class to handle
 *         firestore operations in tandem
 *     </p>
 * </p>
 */
public class RunDrawViewModel extends ViewModel {

    private final IRunDrawRepository repository;

    // LiveData for the Fragment
    public LiveData<Integer> waitingListCount;
    public LiveData<Integer> selectedCount;
    public LiveData<Integer> availableSpaceCount;
    public LiveData<Boolean> isLoading;
    public LiveData<String> message;
    public LiveData<Boolean> drawSuccess;

    public LiveData<String> oldEntrantsStatus;
    public LiveData<String> newChosenEntrants;
    public LiveData<String> newUnchosenEntrants;

    public LiveData<Boolean> cancelSuccess;

    /**
     * Builds viewmodel using RunDraw repo
     * @param repo
     * repo used for run draw operations
     */
    public RunDrawViewModel(IRunDrawRepository repo) {
        this.repository = repo;

        waitingListCount = repo.getWaitingListCount();
        selectedCount = repo.getSelectedCount();
        availableSpaceCount = repo.getAvailableSpaceCount();
        isLoading = repo.isLoading();
        message = repo.getMessage();
        oldEntrantsStatus = repo.getOldEntrantsStatus();
        newChosenEntrants = repo.getNewChosenEntrants();
        newUnchosenEntrants = repo.getNewUnchosenEntrants();
        drawSuccess = repo.getDrawSuccess();
        cancelSuccess = repo.getCancelSuccess();
    }

    /**
     * Loads all metrics for the event
     * <ul>
     *     <li>Waiting list count</li>
     *     <li>Selected Count</li>
     *     <li>Available spots</li>
     * </ul>
     * @param eventId
     * The id for the event metrics should be loaded for
     */
    public void loadMetrics(String eventId) {
        repository.loadMetrics(eventId);
    }

    /**
     * Runs lottery and updates firestore
     * <p>
     *     Waitlist is retrieved and then a specified number of entrants are selected
     *     with their statuses updated to "invited"
     * </p>
     * @param eventId
     * Event to run draw for
     * @param numToSelect
     * Number of participants to select from waitlist to randomly draw for
     */
    public void runDraw(String eventId, int numToSelect) {
        repository.runDraw(eventId, numToSelect);
    }

    public void cancelLottery(String eventId) {
        repository.cancelLottery(eventId);
    }
}
