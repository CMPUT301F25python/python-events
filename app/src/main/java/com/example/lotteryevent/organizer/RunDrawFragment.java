package com.example.lotteryevent.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.annotation.Nullable;

import com.example.lotteryevent.LotteryManager;
import com.example.lotteryevent.R;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.Lists;

public class RunDrawFragment extends Fragment {
    private FirebaseFirestore db;
    private LotteryManager lotteryManager;
    private EditText numSelectedEntrants;
    private String eventId = "temporary filler for event ID";

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
            android.util.Log.d("RunDraw", "calling LotteryManager for event: " + eventId);
            //lotteryManager.selectWinners(eventId, numToSelect);

            this.db.collection("events").document(this.eventId).get()
                    .addOnSuccessListener(document -> {
                        if (!document.exists()) {
                            Toast.makeText(this.getContext(), "Event not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        List<String> waitlist = (List<String>) document.get("waitlist");
                        List<String> selected = (List<String>) document.get("selected");

                        if (waitlist == null) waitlist = new ArrayList<>();
                        if (selected == null) selected = new ArrayList<>();

                        if (waitlist.isEmpty()) {
                            Toast.makeText(this.getContext(), "waitlist is empty", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (numToSelect > waitlist.size()) {
                            Toast.makeText(this.getContext(), "Waitlist does not contain enough entrants", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Randomly pick entrants
                        Collections.shuffle(waitlist);
                        List<String> chosen = waitlist.subList(0, numToSelect);

                        selected.addAll(chosen);
                        waitlist.removeAll(chosen);

                        this.db.collection("events").document(this.eventId)
                                .update("selected", selected, "waitlist", waitlist)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(this.getContext(), "Draw Complete!", Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(this.getContext(), "Error saving results", Toast.LENGTH_SHORT).show()
                                );
                        })
                        .addOnFailureListener(e ->
                            Toast.makeText(this.getContext(), "Error loading event", Toast.LENGTH_SHORT).show());
            });

        cancelButton.setOnClickListener(v -> requireActivity().onBackPressed());
    }

}
