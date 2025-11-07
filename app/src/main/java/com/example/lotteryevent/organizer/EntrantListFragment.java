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

/**
 * A fragment that displays a list of entrants for a specific event, filtered by their status.
 * <p>
 * This fragment receives an {@code eventId} and a {@code status} (e.g., "accepted", "waiting")
 * as navigation arguments. It then queries the corresponding Firestore subcollection and displays
 * the results in a {@link RecyclerView}.
 */
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

    /**
     * Required empty public constructor for fragment instantiation by the Android framework.
     */
    public EntrantListFragment() {
        // Required empty public constructor
    }

    /**
     * Called when the fragment is first created. This is where we initialize non-view related
     * components and retrieve navigation arguments.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state,
     *                           this is the state.
     */
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

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_list, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has returned,
     * but before any saved state has been restored in to the view. This is where we finalize the
     * fragment's UI by initializing views and fetching data.
     *
     * @param view               The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();

        titleTextView.setText(capitalizeFirstLetter(status));
        fetchEntrantsByStatus();
    }

    /**
     * Initializes the UI components by finding them in the view hierarchy.
     *
     * @param view The root view of the fragment's layout.
     */
    private void initializeViews(View view) {
        titleTextView = view.findViewById(R.id.entrant_list_title);
        recyclerView = view.findViewById(R.id.entrants_recycler_view);
        progressBar = view.findViewById(R.id.loading_progress_bar);
    }

    /**
     * Configures the RecyclerView with a {@link LinearLayoutManager} and sets up the
     * {@link EntrantListAdapter}.
     */
    private void setupRecyclerView() {
        adapter = new EntrantListAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Fetches entrants from the Firestore subcollection where their 'status' field
     * matches the status passed as an argument to this fragment. It updates the
     * RecyclerView's adapter upon successful completion.
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

    /**
     * A utility method to capitalize the first letter of a given string.
     * Used for formatting the title of the page.
     *
     * @param str The string to capitalize.
     * @return The capitalized string, or an empty string if the input is null or empty.
     */
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}