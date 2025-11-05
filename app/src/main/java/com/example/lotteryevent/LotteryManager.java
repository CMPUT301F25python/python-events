package com.example.lotteryevent;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * This class handles the lottery logic
 * and allows the organizer to specify how many entrants should be randomly selected from waitlist
 * It then begins the lottery draw and updates the firestore with information about selected entrants
 *
 *
 */
public class LotteryManager {
    private final FirebaseFirestore db;
    public LotteryManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * This class randomly selects a specified number of entrants to choose from waitlist for an event
     *
     * @param eventId
     * This is the Document ID from firestore for the event
     *
     * @param numSelectedEntrants
     * This is the number of entrants to select
     */
    public void selectWinners(String eventId, int numSelectedEntrants) {
        DocumentReference eventRef = this.db.collection("events").document(eventId);
        eventRef.get().addOnSuccessListener((doc -> {

            //If our entrant list exists, then get the list of entrants
            if (doc.exists()) {
                List<String> entrants = (List<String>) doc.get("entrants");
                //If the list of entrants is empty then notify the organizer
                if (entrants == null || entrants.isEmpty()) {
                    System.out.println("There are currently no entrants for this event: " + eventId);
                }

                //Randomize selection of entrants if not empty
                Collections.shuffle(entrants);

                //Get winners from start of waiting list to end
                List<String> winners = entrants.subList(0, Math.min(numSelectedEntrants, entrants.size()));
                List<String> waitingList = new ArrayList<>();

                //Ensure that our list is split up into selected entrants and remainder is put into waiting list
                if (entrants.size() > numSelectedEntrants) {
                    waitingList.addAll(entrants.subList(numSelectedEntrants, entrants.size()));
                }

                //Update Firestore
                Map<String, Object> updates = new HashMap<>();
                updates.put("SelectedWinners", winners);
                updates.put("waitingList", waitingList);

                eventRef.update(updates)
                        .addOnSuccessListener(v -> System.out.println("Selected winners for " + eventId))
                        .addOnFailureListener(e -> System.out.println("Unable to update event: " + e.getMessage()));
            }
            else {
                System.out.println("Event not found: " + eventId);
            }
        }));
    }
}
