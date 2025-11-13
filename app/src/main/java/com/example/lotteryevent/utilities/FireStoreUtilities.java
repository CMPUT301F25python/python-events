package com.example.lotteryevent.utilities;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.concurrent.atomic.AtomicInteger;

public class FireStoreUtilities {
    private static final String TAG = "FireStoreUtilities";

    /**
     * Fills Textviews with entrant metrics:
     * - Waiting list count
     * - Selected users count
     * - Available space count
     */
    public static void fillEntrantMetrics(
            FirebaseFirestore db,
            String eventId,
            TextView waitingListCountText,
            TextView selectedUsersCountText,
            TextView availableSpaceCountText,
            Context context
    ) {
        // Null catcher
        if (db == null || eventId == null || context == null) {
            Log.e(TAG, "Invalid parameters in fillEntrantMetrics");
        }
        if (waitingListCountText == null) {
            Log.e(TAG, "WaitingListCountText is null. Ensure correct xml ID");
            return;
        }
        // Count invited entrants
        db.collection("events").document(eventId)
                .collection("entrants")
                .whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(waitQuery -> {

                    // Displays the number of entrants in the waiting list
                    waitingListCountText.setText(String.valueOf(waitQuery.size()));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Error loading entrant counts", Toast.LENGTH_SHORT).show()
                );

        // Count invited entrants
        db.collection("events").document(eventId)
                .collection("entrants")
                .whereEqualTo("status", "invited")
                .get()
                .addOnSuccessListener(querySelected -> {
                    if (selectedUsersCountText != null) {
                        selectedUsersCountText.setText(String.valueOf(querySelected.size()));
                    }

                    // Load event capacity and show available spots
                    db.collection("events").document(eventId).get()
                            .addOnSuccessListener(document -> {
                                if (document != null && document.exists()) {
                                    Long capacityLong = document.getLong("capacity");

                                    if (capacityLong != null) {
                                        int capacity = capacityLong.intValue();
                                        // calculates and displays the capacity of the event left after the draw
                                        int spacesLeft = capacity - querySelected.size();
                                        if (spacesLeft > 0) {
                                            availableSpaceCountText.setText(String.valueOf(spacesLeft));
                                        } else {
                                            availableSpaceCountText.setText("0");
                                        }
                                    } else {
                                        availableSpaceCountText.setText("No Limit");
                                    }
                                } else {
                                    Log.d(TAG, "No such document");
                                    Toast.makeText(context, "Event not found.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "get failed with ", e);
                                Toast.makeText(context, "Error loading event's number of spots available", Toast.LENGTH_SHORT).show();
                            });

                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Error loading entrant counts", Toast.LENGTH_SHORT).show()
                );
    }
}
