package com.example.lotteryevent.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import com.example.lotteryevent.LotteryManager;
import com.example.lotteryevent.R;

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
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.db = FirebaseFirestore.getInstance();
        this.lotteryManager = new LotteryManager();
        this.numSelectedEntrants = view.findViewById(R.id.numSelectedEntrants);

        Button runDrawButton = view.findViewById(R.id.runDrawButton);
        Button cancelButton = view.findViewById(R.id.cancelButton);

        runDrawButton.setOnClickListener(v -> {
            String inputText = numSelectedEntrants.getText().toString().trim();

            if (inputText.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a number", Toast.LENGTH_SHORT).show();
                return;
            }

            int numToSelect = Integer.parseInt(inputText);

            // Temp Debug block to confirm correct EventId
            android.util.Log.d("RunDraw", "Running draw for event: " + eventId);

            // Read waitlist subcollection
            db.collection("events").document(eventId)
                    .collection("waitlist").get()
                    .addOnSuccessListener(query -> {

                        List<String> waitlist = new ArrayList<>();
                        for (DocumentSnapshot d : query.getDocuments()) {
                            waitlist.add(d.getId());
                        }

                        if (waitlist.isEmpty()) {
                            Toast.makeText(getContext(), "Waitlist is empty", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int requested = Math.min(numToSelect, waitlist.size());
                        Collections.shuffle(waitlist);
                        List<String> chosen = waitlist.subList(0, requested);

                        // Remove chosen users from waitlist
                        for (String uid : chosen) {
                            db.collection("events").document(eventId)
                                    .collection("waitlist")
                                    .document(uid)
                                    .delete();
                        }

                        // Add selected users to "selected" subcollection
                        for (String uid : chosen) {
                            db.collection("events").document(eventId)
                                    .collection("selected")
                                    .document(uid)
                                    .set(new HashMap<String, Object>());
                        }

                        Toast.makeText(getContext(), "Draw Complete!", Toast.LENGTH_SHORT).show();

                        Bundle bundle = new Bundle();
                        bundle.putString("eventId", eventId);

                        Navigation.findNavController(view)
                                .navigate(R.id.action_runDrawFragment_to_confirmDrawAndNotifyFragment, bundle);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Error loading waitlist", Toast.LENGTH_SHORT).show()
                    );
        });
    }

}
