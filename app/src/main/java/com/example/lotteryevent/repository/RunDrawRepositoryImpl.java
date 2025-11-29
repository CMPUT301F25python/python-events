package com.example.lotteryevent.repository;

import android.util.Log;
import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.utilities.FireStoreUtilities;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Run Draw repository that handles all firestore operations for running draw and loading event metrics
 *
 * <p>
 *     This repository:
 *     <ul>
 *         <li>Loads waiting list, selected entrants and available space metrics</li>
 *         <li>Executes draw by randomly selecting specified number of entrants</li>
 *         <li>Updates entrant status to "invited" in firestore using batch</li>
 *         <li>Cancels draw and returns invited users back to waiting list</li>
 *         <li>Exposes Livedata to viewmodel</li>
 *     </ul>
 * </p>
 */
public class RunDrawRepositoryImpl implements IRunDrawRepository{

    private static final String TAG = "RunDrawRepository";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Context context;

    // LiveData outputs shown to ViewModel
    private final MutableLiveData<Integer> _waitingListCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> _availableSpaceCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> _selectedCount = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _drawSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _cancelSuccess = new MutableLiveData<>(false);

    private final MutableLiveData<String> _oldEntrantsStatus = new MutableLiveData<>();
    private final MutableLiveData<String> _newChosenEntrants = new MutableLiveData<>();
    private final MutableLiveData<String> _newUnchosenEntrants = new MutableLiveData<>();

    public RunDrawRepositoryImpl(Context context) {
        this.context = context;
    }

    @Override
    public LiveData<Integer> getWaitingListCount() { return _waitingListCount; }

    @Override
    public LiveData<Integer> getAvailableSpaceCount() { return _availableSpaceCount; }

    @Override
    public LiveData<Integer> getSelectedCount() { return _selectedCount; }

    @Override
    public LiveData<Boolean> isLoading() { return _isLoading; }

    @Override
    public LiveData<String> getMessage() { return _message; }

    @Override
    public LiveData<Boolean> getDrawSuccess() { return _drawSuccess; }

    @Override
    public LiveData<Boolean> getCancelSuccess() { return _cancelSuccess; }

    @Override
    public LiveData<String> getOldEntrantsStatus() { return _oldEntrantsStatus; }

    @Override
    public LiveData<String> getNewChosenEntrants() { return _newChosenEntrants; }

    @Override
    public LiveData<String> getNewUnchosenEntrants() { return _newUnchosenEntrants; }

    /**
     * Loads waiting list count, selected count, available space count
     * @param eventId
     * Unique Id for each event signifying which entrants should be retrieved
     */
    @Override
    public void loadMetrics(String eventId) {
        // Metrics from utility class
        FireStoreUtilities.fillEntrantMetrics(
                db,
                eventId,
                count -> _waitingListCount.postValue(count),
                count -> _selectedCount.postValue(count),
                count -> _availableSpaceCount.postValue(count),
                msg -> _message.postValue(msg)
        );
    }

    /**
     * Runs lottery draw
     * @param eventId
     * Event we run draw for
     * @param numToSelect
     * Number of participants to randomly select from waitlist
     */
    @Override
    public void runDraw(String eventId, int numToSelect) {
        Gson gson = new Gson();

        _isLoading.postValue(true);

        db.collection("events").document(eventId).collection("entrants")
            .get()
            .addOnSuccessListener(entrantsQuery -> {
                List<Entrant> entrants = new ArrayList<>();
                for (DocumentSnapshot d : entrantsQuery.getDocuments()) {
                    Entrant entrant = d.toObject(Entrant.class);
                    entrants.add(entrant);
                }

                Map<String, String> oldEntrantsStatuses = new HashMap<>();
                for (Entrant entrant : entrants) {
                    oldEntrantsStatuses.put(entrant.getUserId(), entrant.getStatus());
                }
                _oldEntrantsStatus.postValue(gson.toJson(oldEntrantsStatuses));


                db.collection("events")
                    .document(eventId)
                    .collection("entrants")
                    .whereEqualTo("status", "waiting")
                    .get()
                    .addOnSuccessListener(query -> {

                        List<String> waitlist = new ArrayList<>();
                        for (DocumentSnapshot d : query.getDocuments()) {
                            waitlist.add(d.getId());
                        }

                        if (waitlist.isEmpty()) {
                            _message.postValue("Waitlist is empty");
                            _isLoading.postValue(false);
                            return;
                        }
                        if (numToSelect > waitlist.size()) {
                            _message.postValue("You cannot select more than " + waitlist.size() + " people");
                            _isLoading.postValue(false);
                            return;
                        }

                        Collections.shuffle(waitlist);
                        List<String> chosen = waitlist.subList(0, numToSelect);

                        ArrayList<String> newChosenEntrants = new ArrayList<>(chosen);

                        _newChosenEntrants.postValue(gson.toJson(newChosenEntrants));

                        ArrayList<String> newUnchosenEntrants = new ArrayList<>(waitlist);
                        newUnchosenEntrants.removeAll(chosen);

                        _newUnchosenEntrants.postValue(gson.toJson(newUnchosenEntrants));

                        WriteBatch batch = db.batch();

                        for (String uid : chosen) {
                            DocumentReference userRef = db.collection("events")
                                    .document(eventId)
                                    .collection("entrants")
                                    .document(uid);

                            batch.update(userRef, "status", "invited");
                        }

                        batch.commit()
                                .addOnSuccessListener(unused -> {
                                    _message.postValue("Draw Complete!");
                                    _drawSuccess.postValue(true);
                                    _isLoading.postValue(false);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Batch update failed", e);
                                    _message.postValue("Error updating user statuses");
                                    _isLoading.postValue(false);
                                });
                    })
                    .addOnFailureListener(e -> {
                        _message.postValue("Error loading waitlist");
                        _isLoading.postValue(false);
                    });

            })
            .addOnFailureListener(e -> {
                _message.postValue("Error loading entrants");
                _isLoading.postValue(false);
            });
    }

    /**
     * Cancels lottery draw by restoring statuses from "invited" to "waiting"
     * @param eventId
     * Event to cancel draw for
     */
    @Override
    public void cancelLottery(String eventId) {
        _isLoading.postValue(true);

        FireStoreUtilities.cancelLottery(
                db,
                eventId,
                () -> {
                    _message.postValue("Lottery cancelled");
                    _cancelSuccess.postValue(true);
                    _isLoading.postValue(false);
                },
                (err) -> {
                    _message.postValue(err);
                    _cancelSuccess.postValue(false);
                    _isLoading.postValue(false);
                }
        );
    }

}
