package com.example.lotteryevent.repository;

import android.util.Log;
import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.utilities.FireStoreUtilities;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

public class RunDrawRepositoryImpl implements IRunDrawRepository{

    private static final String TAG = "RunDrawRepository";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Context context;

    private final MutableLiveData<Integer> _waitingListCount = new MutableLiveData<>();
    private final MutableLiveData<Integer> _availableSpaceCount = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _drawSuccess = new MutableLiveData<>(false);

    public RunDrawRepositoryImpl(Context context) {
        this.context = context;
    }

    @Override
    public LiveData<Integer> getWaitingListCount() { return _waitingListCount; }

    @Override
    public LiveData<Integer> getAvailableSpaceCount() { return _availableSpaceCount; }

    @Override
    public LiveData<Boolean> isLoading() { return _isLoading; }

    @Override
    public LiveData<String> getMessage() { return _message; }

    @Override
    public LiveData<Boolean> getDrawSuccess() { return _drawSuccess; }

    @Override
    public void loadMetrics(String eventId) {
        // Metrics from utility class
        FireStoreUtilities.fillEntrantMetrics(
                db,
                eventId,
                null,
                count -> _waitingListCount.postValue(count),
                count -> _availableSpaceCount.postValue(count),
                context
        );
    }

    @Override
    public void runDraw(String eventId, int numToSelect) {
        _isLoading.postValue(true);

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
                    }

                    Collections.shuffle(waitlist);
                    List<String> chosen = waitlist.sublist(0, numToSelect);

                    writeBatch batch = db.batch();

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
    }

}
