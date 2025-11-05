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

            // If our entrant list exists, then get the list of entrants
            if (doc.exists()) {

                List<String> waitlist = (List<String>) doc.get("waitlist");
                List<String> selected = (List<String>) doc.get("selected");

                if (waitlist == null) waitlist = new ArrayList<>();
                if (selected == null) selected = new ArrayList<>();

                // Check if waitlist is empty and stop process
                if (waitlist.isEmpty()) {
                    System.out.println("Waitlist is empty for this event: " + eventId);
                    return;
                }

                int requested = numSelectedEntrants;
                // Ensure selected number of entrants is within waiting list size
                if (requested > waitlist.size()) {
                    System.out.println("Requesting more entrants than available in waitlist");
                    requested = waitlist.size();
                }

                // Shuffle and pick winners
                Collections.shuffle(waitlist);
                List<String> winners = waitlist.subList(0, requested);

                selected.addAll(winners);
                waitlist = new ArrayList<>(waitlist.subList(requested, waitlist.size()));

                // Update Firestore
                Map<String, Object> updates = new HashMap<>();
                updates.put("elected", selected);
                updates.put("waitlist", waitlist);

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
