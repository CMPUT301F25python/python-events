package com.example.lotteryevent.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;
import android.widget.EditText;

import com.example.lotteryevent.R;
import com.example.lotteryevent.adapters.EventAdapter;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.AvailableEventsRepositoryImpl;
import com.example.lotteryevent.repository.EventRepositoryImpl;
import com.example.lotteryevent.repository.IAvailableEventsRepository;
import com.example.lotteryevent.repository.IEventRepository;
import com.example.lotteryevent.viewmodels.AvailableEventsViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.example.lotteryevent.viewmodels.HomeViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AvailableEventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private AvailableEventsViewModel availableEventsViewModel;
    private ViewModelProvider.Factory viewModelFactory;
    private List<Event> allEvents = new ArrayList<>();
    private String currentKeyword = "";
    private boolean filterAvailableToday = false;

    public AvailableEventsFragment() { }

    public AvailableEventsFragment(ViewModelProvider.Factory viewModelFactory) {
        this.viewModelFactory = viewModelFactory;
    }


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
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (viewModelFactory == null) {
            GenericViewModelFactory factory = new GenericViewModelFactory();
            IAvailableEventsRepository availableEventsRepository = new AvailableEventsRepositoryImpl();
            factory.put(AvailableEventsViewModel.class, () -> new AvailableEventsViewModel(availableEventsRepository));
            viewModelFactory = factory;
        }

        // Use the factory (either from production or from the test) to create the ViewModel.
        availableEventsViewModel = new ViewModelProvider(this, viewModelFactory).get(AvailableEventsViewModel.class);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_available_events, container, false);
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

        setupRecyclerView(view);
        setupObservers();
        setupButtons(view);
        availableEventsViewModel.fetchAvailableEvents();

    }

    public void setupRecyclerView(View view) {
        // Use ONE RecyclerView reference
        recyclerView = view.findViewById(R.id.events_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Use the adapter constructor that takes the row layout you want here
        adapter = new EventAdapter(R.layout.item_event);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(event -> {
            String id = event.getEventId();
            if (id == null || id.trim().isEmpty() || "null".equals(id)) {
                Toast.makeText(requireContext(), "Missing/invalid event id", Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle args = new Bundle();
            args.putString("eventId", id);

            androidx.navigation.NavController nav =
                    androidx.navigation.fragment.NavHostFragment.findNavController(AvailableEventsFragment.this);
            nav.navigate(R.id.eventDetailsFragment, args);
        });
    }

    public void setupObservers() {
        availableEventsViewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            if (events != null) {
                allEvents = events;
            } else {
                allEvents = new ArrayList<>();
            }
            applyFiltersAndUpdateList();
        });

        availableEventsViewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Sets up click listeners for the "Available Today" and filter buttons.
     *
     * @param view The root view of this fragment.
     */
    public void setupButtons(View view) {
        View availableTodayButton = view.findViewById(R.id.available_today_button);
        View filterButton = view.findViewById(R.id.filter_button);

        // Toggle "available today" filter
        availableTodayButton.setOnClickListener(v -> {
            filterAvailableToday = !filterAvailableToday;
            applyFiltersAndUpdateList();
        });

        // Show a simple dialog to enter a keyword for interest-based filtering
        filterButton.setOnClickListener(v -> showKeywordDialog());
    }

    /**
     * Applies the interest (keyword) and availability filters to the full list of events
     * and updates the adapter with the filtered list.
     *
     * <p>Interest filter:
     * <ul>
     *     <li>If a keyword is provided, only events whose name or description contains
     *         that keyword (case-insensitive) are shown.</li>
     * </ul>
     *
     * <p>Availability filter:
     * <ul>
     *     <li>If "Available Today" is active, only events whose start date is today
     *         are shown.</li>
     * </ul>
     */
    private void applyFiltersAndUpdateList() {
        if (adapter == null) {
            return;
        }

        if (allEvents == null) {
            adapter.setEvents(new ArrayList<>());
            return;
        }

        List<Event> filtered = new ArrayList<>();

        String keyword = currentKeyword == null ? "" : currentKeyword.trim().toLowerCase();
        boolean filterByKeyword = !keyword.isEmpty();

        java.util.Calendar today = java.util.Calendar.getInstance();
        int todayYear = today.get(java.util.Calendar.YEAR);
        int todayDayOfYear = today.get(java.util.Calendar.DAY_OF_YEAR);

        for (Event event : allEvents) {

            // Interest filter: checks for keyword in event title or description
            if (filterByKeyword) {
                String name = event.getName();
                String description = event.getDescription();

                String nameLower = name == null ? "" : name.toLowerCase();
                String descLower = description == null ? "" : description.toLowerCase();

                if (!nameLower.contains(keyword) && !descLower.contains(keyword)) { // Keyword does not appear in name or description
                    continue;
                }
            }

            // Availability filter: checks for if event starts today
            if (filterAvailableToday) {
                com.google.firebase.Timestamp startTs = event.getEventStartDateTime();
                if (startTs == null) { // No start date, therefore cannot be available today
                    continue;
                }

                java.util.Calendar eventCal = java.util.Calendar.getInstance();
                eventCal.setTime(startTs.toDate());

                int eventYear = eventCal.get(java.util.Calendar.YEAR);
                int eventDayOfYear = eventCal.get(java.util.Calendar.DAY_OF_YEAR);

                if (eventYear != todayYear || eventDayOfYear != todayDayOfYear) { // Event does not start today
                    continue;
                }
            }

            filtered.add(event);
        }

        adapter.setEvents(filtered);
    }

    /**
     * Shows a dialog allowing the user to enter a keyword used to filter events
     * by name or description.
     */
    private void showKeywordDialog() {
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Enter a keyword");
        input.setText(currentKeyword);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Filter by keyword")
                .setView(input)
                .setPositiveButton("Apply", (dialog, which) -> {
                    currentKeyword = input.getText().toString();
                    applyFiltersAndUpdateList();
                })
                .setNegativeButton("Clear", (dialog, which) -> {
                    currentKeyword = "";
                    applyFiltersAndUpdateList();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

}
