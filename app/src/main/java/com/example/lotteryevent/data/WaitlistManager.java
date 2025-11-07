package com.example.lotteryevent.data;

import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles Firestore operations related to event waitlists.
 * including joining and leaving an event's waitlist.
 *
 * @author Sanaa Bhaidani
 * @version 1.0
 */
public class WaitlistManager {
    private final String TAG = "WaitlistManager";
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public WaitlistManager(){
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Add the user to the waitlist for a specific event given they are not already on the waitlist
     * and they are not the event organizer
     * @param eventId The firestore ID of the event
     * @param organizerId  The firestore ID of the event organizer
     */
    public void joinWaitlist(String eventId, String organizerId){
        String currentUser = auth.getCurrentUser().getUid(); //getting the users ID

        DocumentReference eventReference = db.collection("events").document(eventId);

        // adding the user to the waitlist data struct
        Map<String, Object> waitlist = new HashMap<>();
        waitlist.put("userId", currentUser);

        // adding user to firebase
        eventReference.collection("waitlist").whereEqualTo("userId", currentUser).get()
                .addOnSuccessListener(querySnapshot -> {
                    // if already on the waitlist, don't add them again
                    if(!querySnapshot.isEmpty()){
                        return;
                    }

                    // cannot join your own event
                    if(organizerId.equals(currentUser)) {
                        return;
                    }

                    // adding a user
                    eventReference.collection("waitlist").add(waitlist)
                            .addOnSuccessListener(documentReference -> {
                                Log.d(TAG, "User added to waitlist for event " + eventId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Could not join waitlist" + e);
                            });
                    })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking the current waitlist collection", e);
                });
    }

}
