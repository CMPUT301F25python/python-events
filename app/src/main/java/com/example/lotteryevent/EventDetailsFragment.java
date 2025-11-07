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
import java.util.Map;
import java.util.Objects;

public class EventDetailsFragment extends Fragment {

    private FirebaseFirestore db;
    private ArrayAdapter<String> listAdapter;
    private ArrayList<String> dataList;
    private ListView detailsList;

    /**
     * Creates view
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_details, container, false);
    }

    /**
     * Deals with view
     * @param v The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
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

    /**
     * Goes through document to add certain fields to the details list
     * @param doc
     */
    private void bind(@NonNull DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
            return;
        }
        dataList.clear();

//        // will implement again after data is available
//        String capacity = "N/A";
//
//        if (doc.getLong("waitingListCount") != null && doc.getLong("waitingListLimit") != null) {
//            Long count = doc.getLong("waitingListCount");
//            Long lim = doc.getLong("waitingListLimit");
//            capacity = count + "/" + lim;
//        }

        Map<String, Object> all = doc.getData();
        if (all != null) {
            // Prefer explicit fields first
            addAny("Name", doc.getString("name"));
            addAny("Organizer", doc.getString("organizerName"));
            addAny("Location", doc.getString("location"));
            try { // timestamps are dramatic and cannot deal if they are null or empty smh
                addAny("Date and Time", Objects.requireNonNull(doc.getTimestamp("eventStartDateTime")).toDate().toString());
            } catch (Throwable t) {
                // never let binding crash the fragment
                android.util.Log.e("EventDetailsBind", "Bind failed for doc " + doc.getId(), t);
                Toast.makeText(requireContext(), "Some fields couldn’t be shown.", Toast.LENGTH_SHORT).show();
            }
            if (doc.getDouble("price") != null) {
                java.text.NumberFormat nf = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.CANADA);
                addAny("Price", nf.format(((Number) doc.get("price")).doubleValue()));
            }
            addAny("Description", doc.getString("description"));
            addAny("Lottery Guidelines", doc.getString("lotteryGuidelines"));
            addAny("Max Attendees", doc.getLong("capacity"));
//            addAny("Waiting List Capacity", capacity); // will implement again after data is available
        }

        listAdapter.notifyDataSetChanged();
    }

    /**
     * A simple date formatter.
     */
    private final java.text.DateFormat DF =
            new java.text.SimpleDateFormat("EEE, MMM d yyyy • h:mm a", java.util.Locale.getDefault());

    /**
     * Adds a field to the list.
     * @param label
     * @param raw
     */
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
}