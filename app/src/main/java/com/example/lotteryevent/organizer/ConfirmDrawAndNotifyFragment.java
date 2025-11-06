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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This fragment allows the organizer to run the draw for an event.
 * <p>
 *     Organizer enters how many participants should be selected, and then starts draw
 *     When the draw begins, this fragment calls LotteryManager to perform selection
 *     and update firestore
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

//    private ArrayList<String> chosenEntrantsId;

    private String eventId = "temporary filler for event ID";


    /**
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
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
     *
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

//        chosenEntrantsId = new ArrayList<>();
        notifManager = new NotificationCustomManager(getContext());

        fillEntrantMetrics();
        setupClickListeners(view);

    }

    private void fillEntrantMetrics() {
        AtomicInteger totalEntrantNum = new AtomicInteger();

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
                selectedUsersCountText.setText(String.valueOf(query.getDocuments().size()));
            })
            .addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Error loading entrant counts", Toast.LENGTH_SHORT).show()
            );

        waitingListCountText.setText(String.valueOf(totalEntrantNum.get()));

        db.collection("events").document(eventId).get()
            .addOnSuccessListener(document -> {
                if (document != null && document.exists()) {
                    Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    Long capacityLong = document.getLong("capacity");
                    Integer capacity = capacityLong.intValue();
                    if (capacity != null) {
                        availableSpaceCountText.setText(String.valueOf(capacity));
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

    private void setupClickListeners(@NonNull View view) {
        confirmAndNotifyButton.setOnClickListener(v -> confirmSelectedUsersAndNotify(view));

        cancelButton.setOnClickListener(v -> cancelLottery(view));
    }

    private void confirmSelectedUsersAndNotify(@NonNull View view) {
        db.collection("events").document(eventId)
                .collection("selected").get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Toast.makeText(getContext(), "No chosen entrants to confirm and notify.", Toast.LENGTH_SHORT).show();
                    } else {
                        AtomicInteger completed = new AtomicInteger(0);
                        int total = query.size();
                        for (DocumentSnapshot d : query.getDocuments()) {
                            String uid = d.getId();
                            db.collection("users").document(uid).get()
                                            .addOnSuccessListener(user -> {
                                                notifyEntrant(uid);
                                                int current = completed.incrementAndGet();
                                                if (current == total) {
                                                    Navigation.findNavController(view).navigate(R.id.action_confirmDrawAndNotifyFragment_to_homeFragment);
                                                }
                                            })
                                            .addOnFailureListener(user -> {
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
    private void notifyEntrant(String uid) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(document -> {
                    if (document != null && document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        String eventName = document.getString("name");
                        String organizerId = document.getString("organizerId");
                        String organizerName = document.getString("organizerName");
                        String title = "Congratulations!";
                        String message = "You have been chosen from to sign up for the event " + eventName + "! Please click here to confirm your registration.";
                        notifManager.sendNotification(uid, title, message, eventId, eventName, organizerId, organizerName);
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

    private void cancelLottery(@NonNull View view) {
        db.collection("events").document(eventId)
            .collection("selected").get()
            .addOnSuccessListener(query -> {
                if (!query.isEmpty()) {
                    for (DocumentSnapshot d : query.getDocuments()) {
//                        Map<String,Object> data = d.getData();
//                        System.out.println("Data: " + data);
                        String id = d.getId();

                        db.collection("events").document(eventId)
                            .collection("waitlist").document(id).set(new HashMap<String, Object>())
                            .addOnSuccessListener(queryWaitList -> {
                                Log.d(TAG, "Document ID " + id + " successfully written!");
                                // remove document from selected collection
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
