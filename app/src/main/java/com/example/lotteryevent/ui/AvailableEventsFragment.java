package com.example.lotteryevent.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;
import com.example.lotteryevent.adapters.EventAdapter;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.AvailableEventsRepositoryImpl;
import com.example.lotteryevent.repository.IAvailableEventsRepository;
import com.example.lotteryevent.viewmodels.AvailableEventsViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Fragment} subclass that allows entrants to view a list of available events.
 * It is responsible for displaying all events to the user.
 */
public class AvailableEventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private AvailableEventsViewModel availableEventsViewModel;
    private ViewModelProvider.Factory viewModelFactory;

    // UI-level filter state (the actual filtering is done in the ViewModel)
    private String currentKeyword = "";
    private boolean filterAvailableToday = false;

    public AvailableEventsFragment() { }

    public AvailableEventsFragment(ViewModelProvider.Factory viewModelFactory) {
        this.viewModelFactory = viewModelFactory;
    }

    /**
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to. The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
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
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView(view);
        setupObservers();
        setupButtons(view);

        // Trigger initial load
        availableEventsViewModel.fetchAvailableEvents();

        // Ensure initial filter state is applied (keyword = "", availableToday = false)
        applyFiltersAndUpdateList();
    }

    /**
     * Sets up the {@link RecyclerView} used to display the list of
     * available events. This method:
     * - Finds the RecyclerView in the layout
     * - Attaches a {@link LinearLayoutManager} for vertical scrolling
     * - Creates an {@link EventAdapter} with the event item layout
     * - Registers a click listener that navigates to the event
     *   details screen when an event is selected
     *
     * @param view the root view of this fragment used to locate the RecyclerView
     */
    public void setupRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.events_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

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

    /**
     * Subscribes to LiveData from {@link AvailableEventsViewModel} so that the UI updates
     * automatically when data changes.
     * <p>
     * This method:
     * - Observes the filtered list of available events and sends it directly to the adapter
     * - Observes user-facing messages and shows them using a {@link Toast}
     */
    public void setupObservers() {
        availableEventsViewModel.getFilteredEvents().observe(getViewLifecycleOwner(), events -> {
            if (adapter == null) {
                return;
            }

            if (events == null) {
                adapter.setEvents(new ArrayList<>());
            } else {
                adapter.setEvents(events);
            }
        });

        availableEventsViewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Sets up click listeners for the "Available Today" button and the keyword filter button.
     * - The "Available Today" button toggles a flag and updates the ViewModel's filter state
     *   so that only events starting today are shown when it is enabled.
     * - The filter button opens a dialog that lets the user enter a keyword to filter events by
     *   name or description.
     *
     * @param view the root view of this fragment used to locate the buttons
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
     * Notifies the ViewModel of the current filter state (keyword and "available today" flag).
     * <p>
     * All filtering logic is implemented in {@link AvailableEventsViewModel}. This method only
     * forwards the user's filter choices to the ViewModel.
     */
    private void applyFiltersAndUpdateList() {
        availableEventsViewModel.setKeywordFilter(currentKeyword);
        availableEventsViewModel.setFilterAvailableToday(filterAvailableToday);
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
