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
        if (!doc.exists()) {
            Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
            return;
        }
        dataList.clear();

        String capacity = "N/A";

        if (doc.getLong("waitinglistCount") != null && doc.getLong("waitinglistlimit") != null) {
            Long count = doc.getLong("waitinglistCount");
            Long lim = doc.getLong("waitinglistlimit");
            capacity = count + "/" + lim;
        }

        Map<String, Object> all = doc.getData();
        if (all != null) {
            // Prefer explicit fields first
            add("Name", doc.getString("name"));
            add("Organizer", doc.getString("organizerName"));
            add("Date and Time", Objects.requireNonNull(doc.getTimestamp("eventStartDateTime")).toDate().toString());
            add("Location", doc.getString("location"));
            add("Description", doc.getString("description"));
            add("Lottery Guidelines", doc.getString("lotteryGuidelines"));
            add("Waiting List Capacity", capacity);
        }

//        // Then add any other fields generically
//        Map<String, Object> all = doc.getData();
//        if (all != null) {
//            for (Map.Entry<String, Object> e : all.entrySet()) {
//                String k = e.getKey();
//                if (k.equals("name") || k.equals("location") || k.equals("status") || k.equals("description")) continue;
//                dataList.add(k + ": " + String.valueOf(e.getValue()));
//            }
//        }

        listAdapter.notifyDataSetChanged();
    }

//    private void addIfPresent(@NonNull List<String> out, String label, @Nullable Object value) {
//        if (value == null) return;
//        if (value instanceof String && ((String) value).trim().isEmpty()) return;
//        if (value instanceof com.google.firebase.Timestamp ts) {
//            out.add(label + ": " + ts.toDate());
//        } else {
//            out.add(label + ": " + value.toString());
//        }
//    }


    private void add(String label, @Nullable String value) {
        if (value == null) return;                  // null -> skip
        String v = value.trim();                    // remove spaces/tabs/newlines
        if (v.isEmpty()) return;                    // blank -> skip
        if ("null".equalsIgnoreCase(v)) return;     // literal "null" -> skip\
        try {
            dataList.add(label + ": " + v);
        } catch (NumberFormatException e) {
            android.util.Log.e("EventsMap", "Skipping bad param " + value, e);
        }
    }

}
