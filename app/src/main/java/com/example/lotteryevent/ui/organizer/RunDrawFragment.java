package com.example.lotteryevent.ui.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import com.example.lotteryevent.LotteryManager;
import com.example.lotteryevent.R;

import com.example.lotteryevent.utilities.FireStoreUtilities;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * This fragment allows the organizer to run the draw for an event.
 * <p>
 *     Organizer enters how many participants should be selected, and then starts draw
 *     When the draw begins, this fragment calls LotteryManager to perform selection
 *     and update firestore
 * </p>
 */
public class RunDrawFragment extends Fragment {
    private FirebaseFirestore db;
    private LotteryManager lotteryManager;
    private EditText numSelectedEntrants;
    private String eventId = "temporary filler for event ID";
    private TextView waitingListCountText;
    private TextView availableSpaceCountText;

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
        View view = inflater.inflate(R.layout.fragment_run_draw, container, false);

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
        return view;
    }

    /**
     * This block handles the logic for the removal and adding of users to waitlist and selected list
     * after draw has been initialized. Also ensures that number of participants selected doesn't exceed
     * number of people on the waitlist
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        lotteryManager = new LotteryManager();
        numSelectedEntrants = view.findViewById(R.id.numSelectedEntrants);
        waitingListCountText = view.findViewById(R.id.waiting_list_count);
        availableSpaceCountText = view.findViewById(R.id.available_space_count);
        Button runDrawButton = view.findViewById(R.id.runDrawButton);
        Button cancelButton = view.findViewById(R.id.cancel_button);

        // Firestore helper to populate waiting list and available spots
        FireStoreUtilities.fillEntrantMetrics(
                db,
                eventId,
                waitingListCountText,
                null,
                availableSpaceCountText,
                getContext()
            );

        // Run draw
        runDrawButton.setOnClickListener(v -> {
            String inputText = numSelectedEntrants.getText().toString().trim();

            if (inputText.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a number", Toast.LENGTH_SHORT).show();
                return;
            }

            int numToSelect;

            // In case input is ever changed from numerical Type (Remove try block and only leave numToSelect <= 0)
            try {
                numToSelect = Integer.parseInt(inputText);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (numToSelect <= 0) {
                Toast.makeText(getContext(), "Number of participants must be greater than zero.", Toast.LENGTH_SHORT).show();
                return;
            }


            android.util.Log.d("RunDraw", "Running draw for event: " + eventId);

            // Waitlist status
            db.collection("events").document(eventId)
                    .collection("entrants")
                    .whereEqualTo("status", "waiting")
                    .get()
                    .addOnSuccessListener(query -> {

                        List<String> waitlist = new ArrayList<>();
                        for (DocumentSnapshot d : query.getDocuments()) {
                            waitlist.add(d.getId());
                        }

                        if (waitlist.isEmpty()) {
                            Toast.makeText(getContext(), "Waitlist is empty", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Prevent organizer from selecting more people than available
                        if (numToSelect > waitlist.size()) {
                            Toast.makeText(getContext(),
                                    "You cannot select more participants than are on the wait list (" + waitlist.size() + ").",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        Collections.shuffle(waitlist);
                        List<String> chosen = waitlist.subList(0, numToSelect);

                        com.google.firebase.firestore.WriteBatch batch = db.batch();

                        for (String uid : chosen) {
                            com.google.firebase.firestore.DocumentReference userRef = db.collection("events").document(eventId)
                                    .collection("entrants")
                                    .document(uid);
                            batch.update(userRef, "status", "invited");
                        }

                        // Commit the batch operation
                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    // Toast message if all users have been successfully updated
                                    Toast.makeText(getContext(), "Draw Complete!", Toast.LENGTH_SHORT).show();

                                    Bundle bundle = new Bundle();
                                    bundle.putString("eventId", eventId);

                                    Navigation.findNavController(view)
                                            .navigate(R.id.action_runDrawFragment_to_confirmDrawAndNotifyFragment, bundle);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Error updating user statuses.", Toast.LENGTH_SHORT).show();
                                    android.util.Log.e("RunDraw", "Batch update failed", e);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error loading waitlist", Toast.LENGTH_SHORT).show();
                    });
        });

        // Cancel button from Firestore utility helper class
        cancelButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
    });
    }
}
