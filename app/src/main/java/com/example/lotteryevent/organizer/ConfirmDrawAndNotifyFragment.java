package com.example.lotteryevent.organizer;

import android.annotation.SuppressLint;
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
    @SuppressLint("SetTextI18n")
    private void fillEntrantMetrics() {
        // Used to find the total number of entrants, adds from the waiting list and the selected list
        AtomicInteger chosenEntrants = new AtomicInteger();

        // Count invited entrants
        db.collection("events").document(eventId)
            .collection("entrants")
            .whereEqualTo("status", "waiting")
            .get()
            .addOnSuccessListener(waitQuery -> {

                // displays the number of entrants in the waiting list
                waitingListCountText.setText(String.valueOf(waitQuery.size()));
            })
            .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Error loading entrant counts", Toast.LENGTH_SHORT).show()
            );

        // Count invited entrants
        db.collection("events").document(eventId)
            .collection("entrants")
            .whereEqualTo("status", "invited")
            .get()
            .addOnSuccessListener(querySelected -> {
                selectedUsersCountText.setText(String.valueOf(querySelected.size()));

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
                                Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "get failed with ", e);
                            Toast.makeText(getContext(), "Error loading event's number of spots available", Toast.LENGTH_SHORT).show();
                        });

            })
            .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Error loading entrant counts", Toast.LENGTH_SHORT).show()
            );
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
                .collection("entrants")
                .whereEqualTo("status", "invited")
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) {
                        Toast.makeText(getContext(), "No chosen entrants to confirm and notify.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AtomicInteger completed = new AtomicInteger(0);
                    int total = query.size();

                    for (DocumentSnapshot d : query.getDocuments()) {
                        String uid = d.getId();

                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(user -> {
                                    // Notify this entrant
                                    notifyEntrant(uid);
                                    updateAfterNotification(completed, total, view);
                                })
                                .addOnFailureListener(user -> {
                                    Toast.makeText(getContext(), "User " + uid + " not found. Skipped.", Toast.LENGTH_SHORT).show();
                                    updateAfterNotification(completed, total, view);
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error loading selected entrants", Toast.LENGTH_SHORT).show()
                );
    }

    private void updateAfterNotification(AtomicInteger completed, int total, View view) {
        if (completed.incrementAndGet() == total) {
            navigateBack(view);
        }
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
                        String title = "Congratulations!";
                        String message = "You've been selected for " + eventName + "! Tap to accept or decline.";
                        String type = "lottery_win";
                        notifManager.sendNotification(uid, title, message, type, eventId, eventName, organizerId, organizerName);
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
     * Cancels the lottery by bringing entrants back to the waiting list and switches fragment to the home fragment
     * @param view Used to navigate to another fragment
     */
    private void cancelLottery(@NonNull View view) {
        db.collection("events").document(eventId)
            .collection("entrants")
            .whereEqualTo("status", "invited")
            .get()
            .addOnSuccessListener(query -> {
                if (!query.isEmpty()) {
                    for (DocumentSnapshot entrantDoc : query.getDocuments()) {
                        entrantDoc.getReference().update("status", "waiting");
                    }
                    Toast.makeText(getContext(), "Lottery Cancelled", Toast.LENGTH_SHORT).show();
                    navigateBack(view);

                }
            })
            .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Error cancelling lottery", Toast.LENGTH_SHORT).show()
            );
        }

    /**
     * This is a helper function that navigates back to the organizer's event page after confirming the draw
     * This uses the navigation component with Safe Args to pass event ID back to event page
     * @param view
     * View that triggers the navigation action
     */
    private void navigateBack(View view) {
        ConfirmDrawAndNotifyFragmentDirections.ActionConfirmDrawAndNotifyFragmentToOrganizerEventPageFragment action =
                ConfirmDrawAndNotifyFragmentDirections
                        .actionConfirmDrawAndNotifyFragmentToOrganizerEventPageFragment(eventId);

        Navigation.findNavController(view).navigate(action);
    }
}
