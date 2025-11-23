package com.example.lotteryevent.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lotteryevent.data.RegistrationHistoryItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * This class reads through each of the events, check if the UID is inside the entrants
 * subcollection, and then returns a list of event name + the user's status.
 */
public class RegistrationHistoryRepositoryImpl implements IRegistrationHistoryRepository{
    private static final String TAG = "RegistrationHistoryRepo";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final MutableLiveData<List<RegistrationHistoryItem>> _history = new MutableLiveData<>();
    private final MutableLiveData<String> _userMessage = new MutableLiveData<>();


    @Override
    public LiveData <List<RegistrationHistoryItem>> fetchRegistrationHistory() {

        // getting the user's id
        String uid;
        if (auth.getCurrentUser() != null) {
            uid = auth.getCurrentUser().getUid();
        } else {
            uid = null;
        }

        // if not signed in, let user know and show no events
        if (uid == null) {
            _userMessage.postValue("User not signed in");
            _history.postValue(new ArrayList<>());
            return _history;
        }

        // going through each event and pulling any instances of user in entrant subcollection
        db.collection("events").get()
                .addOnSuccessListener(eventSnapshots -> {
                    List<RegistrationHistoryItem> list = new ArrayList<>();

                    for (QueryDocumentSnapshot eventDocument : eventSnapshots) {
                        String eventId = eventDocument.getId(); // getting the event ID
                        String eventName = eventDocument.getString("name"); // getting the event name

                        // look at the entrants subcollection for the user
                        eventDocument.getReference().collection("entrants").document(uid).get()
                                .addOnSuccessListener(entrantDocument -> {
                                    // if entrant does not exist, move on to next event
                                    if (!entrantDocument.exists()) {
                                        return;
                                    }

                                    String status = entrantDocument.getString("status");
                                    // if, for some reason, user is in subcollection but has no status, ignore
                                    if (status == null) {
                                        return;
                                    }

                                    // create item (i.e. row) for this event
                                    RegistrationHistoryItem item = new RegistrationHistoryItem();
                                    item.setEventId(eventId);
                                    item.setEventName(eventName);
                                    item.setStatus(status);

                                    list.add(item);
                                    _history.postValue(list);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event history", e);
                    _userMessage.postValue("Failed to load registration history.");
                    _history.postValue(new ArrayList<>());
                });
        return _history;
    }

    @Override
    public LiveData<String> getUserMessage() {
        return null;
    }

    @Override
    public void setUserMessage(String message) {

    }
}
