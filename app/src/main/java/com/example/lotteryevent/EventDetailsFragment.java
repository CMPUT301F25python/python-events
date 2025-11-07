package com.example.lotteryevent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EventDetailsFragment extends Fragment {

    private FirebaseFirestore db;
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> dataList;
    private ListView detailsList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        detailsList = v.findViewById(R.id.details_list);
        dataList = new ArrayList<>();
        listAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, dataList);
        detailsList.setAdapter(listAdapter);

        String eventId = (getArguments() != null) ? getArguments().getString("eventId") : null;
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Missing event id", Toast.LENGTH_SHORT).show();
            return;
        }

        db = FirebaseFirestore.getInstance();
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(this::bind)
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void bind(@NonNull DocumentSnapshot doc) {
//        try {
            if (!doc.exists()) {
                Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
                return;
            }
            dataList.clear();

            String capacity = "N/A";

            if (doc.getLong("waitingListCount") != null && doc.getLong("waitingListLimit") != null) {
                Long count = doc.getLong("waitingListCount");
                Long lim = doc.getLong("waitingListLimit");
                capacity = count + "/" + lim;
            }

            Map<String, Object> all = doc.getData();
            if (all != null) {
                // Prefer explicit fields first
                addAny("Name", doc.getString("name"));
                addAny("Organizer", doc.getString("organizerName"));
                addAny("Location", doc.getString("location"));
                try { // Timestamps are dramatic and cannot deal if they are null or empty smh
                    addAny("Date and Time", Objects.requireNonNull(doc.getTimestamp("eventStartDateTime")).toDate().toString());
                } catch (Throwable t) {
                    // never let binding crash the fragment
                    android.util.Log.e("EventDetailsBind", "Bind failed for doc " + doc.getId(), t);
                    Toast.makeText(requireContext(), "Some fields couldn’t be shown.", Toast.LENGTH_SHORT).show();
                }
                addAny("Description", doc.getString("description"));
                addAny("Lottery Guidelines", doc.getString("lotteryGuidelines"));
                addAny("Waiting List Capacity", capacity);
            }

            listAdapter.notifyDataSetChanged();
//        } catch (Throwable t) {
//            // last-resort guard: never let binding crash the fragment
//            android.util.Log.e("EventDetailsBind", "Bind failed for doc " + doc.getId(), t);
//            Toast.makeText(requireContext(), "Some fields couldn’t be shown.", Toast.LENGTH_SHORT).show();
//        }
    }

    private final java.text.DateFormat DF =
            new java.text.SimpleDateFormat("EEE, MMM d yyyy • h:mm a", java.util.Locale.getDefault());

    private void addAny(String label, @Nullable Object raw) {
        if (raw == null) return;

        String v;
        if (raw instanceof com.google.firebase.Timestamp) {
            v = DF.format(((com.google.firebase.Timestamp) raw).toDate());
        } else if (raw instanceof java.util.Date) {
            v = DF.format((java.util.Date) raw);
        } else {
            v = raw.toString();
        }

        v = v.trim();
        if (v.isEmpty() || "null".equalsIgnoreCase(v)) return;
        dataList.add(label + ": " + v);
    }



    private void add(String label, @Nullable String value) {
        if (value == null) return;
        String v = value.trim();
        if (v.isEmpty()) return;
        if ("null".equalsIgnoreCase(v)) return;
        try {
            dataList.add(label + ": " + v);
        } catch (Exception e) {
            return;
        }
    }

}
