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

    /**
     * Retrieves the current user's registration history by querying all events in
     * Firestore and checking whether the user's UID exists in the "entrants"
     * subcollection of each event. For each matching entry, a corresponding
     * {@link RegistrationHistoryItem} is created and added to the history list.
     * @return LiveData stream containing updates to the user's registration history
     */
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
                /**
                 * Callback triggered when Firestore successfully returns all event documents.
                 * Iterates through each event and performs a secondary fetch to check if the
                 * current user is listed as an entrant for that event.
                 * @param eventSnapshots the QuerySnapshot containing all events in the collection
                 */
                .addOnSuccessListener(eventSnapshots -> {
                    List<RegistrationHistoryItem> list = new ArrayList<>();

                    for (QueryDocumentSnapshot eventDocument : eventSnapshots) {
                        String eventId = eventDocument.getId(); // getting the event ID
                        String eventName = eventDocument.getString("name"); // getting the event name

                        /**
                         * Callback fired when Firestore successfully retrieves the entrant document for
                         * the current user within a specific event. If the document exists and contains
                         * a non-null "status" field, a new {@link RegistrationHistoryItem} is created
                         * and added to the history list.
                         * @param entrantDocument the Firestore document representing the user's entry
                         *                        within the event's "entrants" subcollection
                         */
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
                /**
                 * Callback triggered when Firestore fails to retrieve the list of events.
                 * Logs the error, updates the user-facing message, and posts an empty list
                 * to history LiveData so UI observers can react gracefully.
                 * @param e the exception thrown during Firestore event retrieval
                 */
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load event history", e);
                    _userMessage.postValue("Failed to load registration history.");
                    _history.postValue(new ArrayList<>());
                });
        return _history;
    }

    /**
     * Returns LiveData containing user-facing messages including errors or success.
     * @return LiveData containing the latest user message string
     */
    @Override
    public LiveData<String> getUserMessage() {
        return _userMessage;
    }
}
