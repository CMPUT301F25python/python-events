package com.example.lotteryevent.repository;

import com.example.lotteryevent.data.Event;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminEventsRepositoryImpl implements IAdminEventsRepository{

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Repo implementation to retrieve all events for admin view
     * Uses firestore to fetch entire collection of events regardless of status
     * @param callback
     */
    @Override
    public void getAllEvents(EventsCallback callback) {
        db.collection("events")
                .get()
                .addOnSuccessListener(query -> {
                    List<Event> list = new ArrayList<>();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null) {
                            list.add(e);
                        }
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }
}
