package com.example.lotteryevent;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * This class handles the lottery logic and allows the organizer to specify how many entrants should be randomly selected from waitlist.
 * It then begins the lottery draw and updates the firestore with information about selected entrants, and removes the specified number of entrants from waiting list
 * putting them into the selected list. It ensures that the number of selected participants does not exceed the waitlist size.
 *
 *
 */
public class LotteryManager {
    private final FirebaseFirestore db;
    public LotteryManager() {

        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * This class randomly selects a specified number of entrants to choose from waitlist for an event and begins the lottery draw process
     *
     * @param eventId
     * This is the Document ID from firestore for the event that contains the waitlist
     *
     * @param numSelectedEntrants
     * This is the number of entrants to select from waitlist
     *
     * <p>
     *     -Reads current waitlist and selected lists
     *     -Randomly shuffles to pick winners
     *     -Updates firestore with selected winners as well as those remaining in waitlist
     * </p>
     */
    public void selectWinners(String eventId, int numSelectedEntrants) {
        DocumentReference eventRef = this.db.collection("events").document(eventId);
        // Read waitlist from subcollection
        eventRef.collection("Waitlist")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> waitlist = new ArrayList<>();

                    for (var doc : querySnapshot.getDocuments()) {
                        String uid = doc.getString("UID");
                        if (uid != null) {
                            waitlist.add(uid);
                        }
                    }

                    if (waitlist.isEmpty()) {
                        System.out.println("Waitlist is empty for " + eventId);
                        return;
                    }

                    // Select winners
                    Collections.shuffle(waitlist);
                    int requested = Math.min(numSelectedEntrants, waitlist.size());
                    List<String> winners = waitlist.subList(0, requested);

                    // Write winners to /Selected
                    for (String uid : winners) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("UID", uid);
                        eventRef.collection("Selected").add(data);
                    }

                    // Remove winners from waitlist
                    for (var doc : querySnapshot) {
                        if (winners.contains(doc.getString("UID"))) {
                            doc.getReference().delete();
                        }
                    }

                    System.out.println("Winners selected for event " + eventId);
                })
                .addOnFailureListener(e ->
                        System.out.println("Error reading waitlist: " + e.getMessage()));
    }
}
