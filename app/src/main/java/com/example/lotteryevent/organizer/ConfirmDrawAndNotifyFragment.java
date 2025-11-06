package com.example.lotteryevent.organizer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This fragment allows the organizer to view the number of entrants drawn, confirm draw, and notify the selected entrants.
 * <p>
 *     Organizer can view the number of entrants in waiting list, how many spots available for the event, and how many entrants were drawn.
 *     Organizer can click the "Confirm and Notify" button which confirms the users drawn and notifies the chosen entrants by creating
 *     notification documents for the user in Firebase. Organizer can also press "Cancel" which moves all entrants back to the waiting list.
 * </p>
 */
public class ConfirmDrawAndNotifyFragment extends Fragment {
    private static final String TAG = "ConfirmDrawAndNotifyFragment";
    private FirebaseFirestore db;
    private NotificationCustomManager notifManager;
    private Button confirmAndNotifyButton;
    private Button cancelButton;
    private TextView waitingListCountText;
    private TextView availableSpaceCountText;
    private TextView selectedUsersCountText;
    private String eventId = "temporary filler for event ID";


    /**
     * Inflates the layout for this fragment and gets event ID from bundle.
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return The view for the fragment's UI
     *
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_confirm_draw_and_notify, container, false);

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
        return view;
    }

    /**
     * UI initialization and set up including displaying user metrics to the screen
     * and setting up button click listeners.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        confirmAndNotifyButton = view.findViewById(R.id.confirm_and_notify_button);
        cancelButton = view.findViewById(R.id.cancel_button);

        waitingListCountText = view.findViewById(R.id.waiting_list_count);
        availableSpaceCountText = view.findViewById(R.id.available_space_count);
        selectedUsersCountText = view.findViewById(R.id.selected_users_count);

        notifManager = new NotificationCustomManager(getContext());

        fillEntrantMetrics();
        setupClickListeners(view);

    }


    /**
     * Fills TextViews on the screen with entrant metrics including number of entrants in the waiting list,
     * number of spaces available in the event, and number of entrants chosen by the lottery.
     */
    private void fillEntrantMetrics() {
        // used to find the total number of entrants, adds from the waiting list and the selected list
        AtomicInteger totalEntrantNum = new AtomicInteger();
        AtomicInteger chosenEntrants = new AtomicInteger();

        db.collection("events").document(eventId)
            .collection("waitlist").get()
            .addOnSuccessListener(query -> {
                totalEntrantNum.addAndGet(query.getDocuments().size());
            })
            .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Error loading entrant counts", Toast.LENGTH_SHORT).show()
            );

        db.collection("events").document(eventId)
            .collection("selected").get()
            .addOnSuccessListener(query -> {
                totalEntrantNum.addAndGet(query.getDocuments().size());
                chosenEntrants.set(query.getDocuments().size());
                // displays number of drawn entrants
                selectedUsersCountText.setText(String.valueOf(query.getDocuments().size()));
            })
            .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Error loading entrant counts", Toast.LENGTH_SHORT).show()
            );

        // displays the number of entrants in the waiting list
        waitingListCountText.setText(String.valueOf(totalEntrantNum.get()));

        db.collection("events").document(eventId).get()
            .addOnSuccessListener(document -> {
                if (document != null && document.exists()) {
                    Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    String capacity = document.getString("capacity");

                    if (capacity != null) {
                        // calculates and displays the capacity of the event left after the draw
                        int spacesLeft = Integer.parseInt(capacity) - chosenEntrants.get();
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
                    Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "get failed with ", e);
                Toast.makeText(getContext(), "Error loading event's number of spots available", Toast.LENGTH_SHORT).show();
            });
    }

    /**
     * Sets up click listeners for the buttons.
     * @param view The View from onViewCreated().
     */
    private void setupClickListeners(@NonNull View view) {
        confirmAndNotifyButton.setOnClickListener(v -> confirmSelectedUsersAndNotify(view));
        cancelButton.setOnClickListener(v -> cancelLottery(view));
    }

    /**
     * Confirms that chosen users from the lottery indeed exist and notifies them of their selection.
     * @param view The View from onViewCreated(). Used to navigate to another fragment
     */
    private void confirmSelectedUsersAndNotify(@NonNull View view) {
        db.collection("events").document(eventId)
                .collection("selected").get()
                .addOnSuccessListener(query -> {
                    // no users selected from lottery
                    if (query.isEmpty()) {
                        Toast.makeText(getContext(), "No chosen entrants to confirm and notify.", Toast.LENGTH_SHORT).show();
                    } else {
                        // used to check all users' notify queries sent before changing fragments
                        AtomicInteger completed = new AtomicInteger(0);
                        int total = query.size();
                        for (DocumentSnapshot d : query.getDocuments()) {
                            String uid = d.getId();
                            db.collection("users").document(uid).get()
                                            .addOnSuccessListener(user -> { // user of that id found, can successfully notify them
                                                notifyEntrant(uid);
                                                int current = completed.incrementAndGet();
                                                if (current == total) {
                                                    Navigation.findNavController(view).navigate(R.id.action_confirmDrawAndNotifyFragment_to_homeFragment);
                                                }
                                            })
                                            .addOnFailureListener(user -> { // user with that id not found, cannot notify. move on to the next user
                                                Toast.makeText(getContext(), "User with id " + uid + " not found and could not be notified.", Toast.LENGTH_SHORT).show();
                                                int current = completed.incrementAndGet();
                                                if (current == total) {
                                                    Navigation.findNavController(view).navigate(R.id.action_confirmDrawAndNotifyFragment_to_homeFragment);
                                                }
                                            });
                        }
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading selected entrants", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Gets event's name, organizer id, organizer name, and creates message to send to notification manager's
     * sendNotification() method to send the notification
     * @param uid User ID, used in sending notification.
     */
    private void notifyEntrant(String uid) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(document -> {
                    if (document != null && document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        String eventName = document.getString("name");
                        String organizerId = document.getString("organizerId");
                        String organizerName = document.getString("organizerName");
                        String message = "You have been chosen from to sign up for the event " + eventName + "! Please click here to confirm your registration.";
                        notifManager.sendNotification(uid, message, eventId, eventName, organizerId, organizerName);
                    } else {
                        Log.d(TAG, "No such document");
                        Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "get failed with ", e);
                    Toast.makeText(getContext(), "Error sending notification to chosen entrant", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Cancels the lottery by bringing entrants back to the waiting collection and switches fragment to the home fragment
     * @param view Used to navigate to another fragment
     */
    private void cancelLottery(@NonNull View view) {
        db.collection("events").document(eventId)
            .collection("selected").get()
            .addOnSuccessListener(query -> {
                if (!query.isEmpty()) {
                    for (DocumentSnapshot d : query.getDocuments()) {
                        String id = d.getId();

                        // for each selected entrant, adds them back to the waiting collection
                        db.collection("events").document(eventId)
                            .collection("waitlist").document(id).set(new HashMap<String, Object>())
                            .addOnSuccessListener(queryWaitList -> {
                                Log.d(TAG, "Document ID " + id + " successfully written!");
                                // remove entrant from the selected collection
                                db.collection("events").document(eventId)
                                        .collection("selected").document(id).delete()
                                        .addOnSuccessListener(v -> {Log.d(TAG, "Document ID " + id + " deleted successfully");})
                                        .addOnFailureListener(v -> {Log.d(TAG, "Document ID " + id + " deletion unsuccessful");});
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Error adding entrant back to waitlist", Toast.LENGTH_SHORT).show()
                            );
                    }
                }
            })
            .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Error loading selected entrant users", Toast.LENGTH_SHORT).show()
            );

        Navigation.findNavController(view)
                .navigate(R.id.action_confirmDrawAndNotifyFragment_to_homeFragment);
    }
}
