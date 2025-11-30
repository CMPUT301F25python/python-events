package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;

/**
 * Repo interface for Run Draw
 * <p>
 *     This interface outlines firestore related operations needed for
 *     RunDrawViewModel and RunDrawFragment
 * </p>
 * <p>
 *     Features:
 *     <ul>
 *         <li>Load waitlist, selected count, and available space metrics</li>
 *         <li>Run Draw for specified number of random entrants</li>
 *         <li>Present Data for viewmodels</li>
 *     </ul>
 * </p>
 *
 */
public interface IRunDrawRepository {

    /**
     * LiveData for number of entrants currently on waitling list
     * @return count
     */
    LiveData<Integer> getWaitingListCount();

    /**
     * LiveData for nubmer of available spaces left for event
     * @return count
     */
    LiveData<Integer> getAvailableSpaceCount();

    /**
     * LiveData for number of entrants with status "invited"
     * @return count
     */
    LiveData<Integer> getSelectedCount();

    /**
     * LiveData to verify firestore operation running
     * @return loading boolean
     */
    LiveData<Boolean> isLoading();

    /**
     * LiveData containing toast messages for user
     * @return message
     */
    LiveData<String> getMessage();

    /**
     * LiveData indicating draw completion
     * @return success boolean
     */
    LiveData<Boolean> getDrawSuccess();

    /**
     * LiveData indicating cancel lottery completion
     * @return cancel success
     */
    LiveData<Boolean> getCancelSuccess();

    /**
     * LiveData for old entrant statuses
     * @return statuses
     */
    public LiveData<String> getOldEntrantsStatus();

    /**
     * LiveData of new chosen entrants
     * @return chosen entrants
     */
    public LiveData<String> getNewChosenEntrants();

    /**
     * LiveData of new unchosen entrants
     * @return unchosen entrants
     */
    public LiveData<String> getNewUnchosenEntrants();

    /**
     * Loads event metrics:
     * <ul>
     *     <li>waitlist size</li>
     *     <li>Selected count</li>
     *     <li>Available spaces</li>
     * </ul>
     * @param eventId
     * Unique Id for each event signifying which entrants should be retrieved
     */
    void loadMetrics(String eventId);

    /**
     * run Draw logic
     * <ul>
     *     <li>Retrieve waitlist entrants</li>
     *     <li>Randomly selects required number of entrants</li>
     *     <li>Update firestore with "invited" status</li>
     * </ul>
     * @param eventId
     * Event we run draw for
     * @param numToSelect
     * Number of participants to randomly select from waitlist
     */
    void runDraw(String eventId, int numToSelect);

    /**
     * cancel draw logic that stops the draw and returns organizer to event details page
     * @param eventId
     * Event to cancel draw for
     */
    void cancelLottery(String eventId);
}
