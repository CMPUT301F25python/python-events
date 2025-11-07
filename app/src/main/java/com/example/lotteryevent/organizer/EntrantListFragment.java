package com.example.lotteryevent.organizer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;
import com.example.lotteryevent.adapters.EntrantListAdapter;
import com.example.lotteryevent.data.Entrant;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EntrantListFragment extends Fragment {

    private static final String TAG = "EntrantListFragment";

    // --- UI Components ---
    private RecyclerView recyclerView;
    private EntrantListAdapter adapter;
    private ProgressBar progressBar;
    private TextView titleTextView;

    // --- Firebase & Data ---
    private FirebaseFirestore db;
    private String eventId;
    private String status;

    public EntrantListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            EntrantListFragmentArgs args = EntrantListFragmentArgs.fromBundle(getArguments());
            eventId = args.getEventId();
            status = args.getStatus();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();

        titleTextView.setText(capitalizeFirstLetter(status));
        fetchEntrantsByStatus();
    }

    private void initializeViews(View view) {
        titleTextView = view.findViewById(R.id.entrant_list_title);
        recyclerView = view.findViewById(R.id.entrants_recycler_view);
        progressBar = view.findViewById(R.id.loading_progress_bar);
    }

    private void setupRecyclerView() {
        adapter = new EntrantListAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Fetches entrants from the Firestore subcollection where their 'status' field
     * matches the status passed to this fragment.
     */
    private void fetchEntrantsByStatus() {
        if (eventId == null || status == null) {
            Toast.makeText(getContext(), "Error: Missing event data.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        db.collection("events").document(eventId).collection("entrants")
                .whereEqualTo("status", status)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Entrant> entrants = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Entrant entrant = document.toObject(Entrant.class);
                            if (entrant != null) {
                                entrants.add(entrant);
                            }
                        }
                        adapter.updateEntrants(entrants);
                        Log.d(TAG, "Fetched " + entrants.size() + " entrants with status: " + status);
                    } else {
                        Log.w(TAG, "Error getting documents: ", task.getException());
                        Toast.makeText(getContext(), "Failed to load entrants.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}