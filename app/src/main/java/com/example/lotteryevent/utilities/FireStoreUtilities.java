package com.example.lotteryevent.utilities;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.function.Consumer;

public class FireStoreUtilities {
        private static final String TAG = "FireStoreUtilities";

        /**
         * MVVM version of entrant metrics loader.
         *
         * This returns metrics through callbacks instead of updating UI elements.
         * This makes the method safe for Repository + ViewModel usage.
         *
         * @param db Firestore instance
         * @param eventId the event ID
         * @param waitingListCallback receives waiting list count (Integer)
         * @param selectedCallback receives selected ("invited") count (Integer)
         * @param availableSpacesCallback receives available spots count (Integer or null for No Limit)
         * @param errorCallback receives any error message
         */
        public static void fillEntrantMetrics(
                FirebaseFirestore db,
                String eventId,
                Consumer<Integer> waitingListCallback,
                Consumer<Integer> selectedCallback,
                Consumer<Integer> availableSpacesCallback,
                Consumer<String> errorCallback
        ) {

            if (db == null || eventId == null) {
                if (errorCallback != null) errorCallback.accept("Invalid parameters");
                return;
            }

            // Waitlist count
            db.collection("events").document(eventId)
                    .collection("entrants")
                    .whereEqualTo("status", "waiting")
                    .get()
                    .addOnSuccessListener(waitQuery -> {
                        if (waitingListCallback != null) {
                            waitingListCallback.accept(waitQuery.size());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error loading waiting list", e);
                        if (errorCallback != null) errorCallback.accept("Error loading waiting list");
                    });

            // Get invited count
            db.collection("events").document(eventId)
                    .collection("entrants")
                    .whereEqualTo("status", "invited")
                    .get()
                    .addOnSuccessListener(selectedQuery -> {

                        int selectedCount = selectedQuery.size();

                        if (selectedCallback != null) {
                            selectedCallback.accept(selectedCount);
                        }

                        // Get event capacity
                        db.collection("events").document(eventId)
                                .get()
                                .addOnSuccessListener(document -> {

                                    Long capacityLong = document.getLong("capacity");

                                    if (capacityLong != null) {
                                        int capacity = capacityLong.intValue();
                                        int spacesLeft = Math.max(0, capacity - selectedCount);

                                        if (availableSpacesCallback != null) {
                                            availableSpacesCallback.accept(spacesLeft);
                                        }

                                    } else {
                                        // No capacity limit
                                        if (availableSpacesCallback != null) {
                                            availableSpacesCallback.accept(null);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Error loading capacity", e);
                                    if (errorCallback != null) errorCallback.accept("Error loading event capacity");
                                });

                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error loading selected count", e);
                        if (errorCallback != null) errorCallback.accept("Error loading selected count");
                    });
        }


    /**
     * Cancels the lottery by bringing entrants back to the waiting list and switches fragment to the home fragment
     */
    public static void cancelLottery(
            FirebaseFirestore db,
            String eventId,
            @Nullable Runnable onSuccess,
            @Nullable Consumer<String> onError
    ) {
        if (db == null || eventId == null) {
            if (onError != null) onError.accept("Invalid parameters for cancellation");
            return;
        }
        db.collection("events").document(eventId)
                .collection("entrants")
                .whereEqualTo("status", "invited")
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        for (DocumentSnapshot entrantDoc : query.getDocuments()) {
                            entrantDoc.getReference().update("status", "waiting");
                        }
                        if (onSuccess != null) {
                            onSuccess.run();
                        } else {
                            if (onError != null) onError.accept("No invited entrants to cancel");
                        }

                    }
                })
                .addOnFailureListener(e -> {
                       if (onError != null) onError.accept( "Error cancelling lottery");
                });
    }


}
