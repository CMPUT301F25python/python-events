package com.example.lotteryevent.ui.organizer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;
import com.example.lotteryevent.adapters.EntrantListAdapter;
import com.example.lotteryevent.repository.EntrantListRepositoryImpl;
import com.example.lotteryevent.viewmodels.EntrantListViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;

/**
 * Fragment responsible for displaying a list of entrants for a specific event,
 * filtered by their status. The fragment receives an event ID and status through
 * navigation arguments, queries Firestore via the ViewModel, and displays results
 * in a RecyclerView. It also allows the organizer to send notifications to all
 * entrants currently displayed.
 */

public class EntrantListFragment extends Fragment {

    private static final String TAG = "EntrantListFragment";

    // --- UI Components ---
    private RecyclerView recyclerView;
    private EntrantListAdapter adapter;
    private ProgressBar progressBar;
    private Button sendNotificationButton;
    private ChipGroup statusChipGroup;
    private TextView entrantsCountText;

    // --- Data ---
    private String eventId;

    // --- ViewModel ---
    private EntrantListViewModel viewModel;
    private ViewModelProvider.Factory viewModelFactory;

    /**
     * Required empty public constructor for fragment instantiation by the Android framework.
     */
    public EntrantListFragment() {
        // Required empty public constructor
    }

    /**
     * Constructor used primarily for testing, allowing injection of a custom
     * ViewModelProvider.Factory instance. This makes it possible to supply mock
     * or fake ViewModels during unit or instrumentation tests.
     * @param factory a custom ViewModel factory to be used when instantiating
     *                the fragment's ViewModel
     */
    public EntrantListFragment(GenericViewModelFactory factory) {
        this.viewModelFactory = factory;
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

        if (getArguments() != null) {
            EntrantListFragmentArgs args = EntrantListFragmentArgs.fromBundle(getArguments());
            eventId = args.getEventId();
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
     * fragment's UI by initializing views and fetching data by attaching LiveData observers.
     *
     * @param view               The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupRecyclerView();

        // --- ViewModel Initialization ---
        // If a factory was not injected (production), create the default one.
        if (viewModelFactory == null) {
            EntrantListRepositoryImpl entrantListRepo = new EntrantListRepositoryImpl(getContext());
            GenericViewModelFactory factory = new GenericViewModelFactory();
            factory.put(EntrantListViewModel.class, () -> new EntrantListViewModel(entrantListRepo, eventId));
            viewModelFactory = factory;
        }

        // Get the ViewModel instance using the determined factory.
        viewModel = new ViewModelProvider(this, viewModelFactory).get(EntrantListViewModel.class);

        // --- The rest of the method is the same ---
        setupFilterChips();
        setupObservers();
    }

    /**
     * Initializes the UI components by finding them in the view hierarchy.
     *
     * @param view The root view of the fragment's layout.
     */
    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.entrants_recycler_view);
        progressBar = view.findViewById(R.id.loading_progress_bar);
        sendNotificationButton = view.findViewById(R.id.send_notification_button);
        statusChipGroup = view.findViewById(R.id.status_chip_group);
        entrantsCountText = view.findViewById(R.id.entrants_count_text);
    }


    /**
     * Configures the RecyclerView with a {@link LinearLayoutManager} and sets up the
     * {@link EntrantListAdapter}.
     */
    private void setupRecyclerView() {
        adapter = new EntrantListAdapter(new ArrayList<>(), userId -> {
            viewModel.cancelInvite(userId);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Sets up the listener for the Status Filter Chips.
     * When a chip is clicked, it updates the ViewModel.
     */
    private void setupFilterChips() {
        statusChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            // Get the first checked ID
            int checkedId = checkedIds.get(0);
            String newStatus = "waiting";

            if (checkedId == R.id.chip_waiting) {
                newStatus = "waiting";
            } else if (checkedId == R.id.chip_invited) {
                newStatus = "invited";
            } else if (checkedId == R.id.chip_accepted) {
                newStatus = "accepted";
            } else if (checkedId == R.id.chip_cancelled) {
                newStatus = "cancelled";
            }

            // Update ViewModel
            viewModel.setFilterStatus(newStatus);
        });
    }

    /**
     * Sets up observers on the ViewModel's LiveData.
     * This is the core of the reactive UI.
     */
    private void setupObservers() {
        if (eventId == null) {
            Toast.makeText(getContext(), "Error: Event data not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        /**
         * Callback triggered whenever the LiveData containing entrant lists updates.
         * Hides the loading indicator, updates the adapter with the new list, logs
         * diagnostic information, and configures the notification button to open the
         * dialog for notifying all entrants.
         * @param list the updated list of entrants retrieved from the ViewModel
         */
        viewModel.getFilteredEntrants().observe(getViewLifecycleOwner(), list -> {
            progressBar.setVisibility(View.GONE);
            if (list != null) {
                adapter.updateEntrants(list);
                int count = list.size();
                String text = count + (count == 1 ? " entrant" : " entrants");
                entrantsCountText.setText(text);

                sendNotificationButton.setOnClickListener(v -> {
                    if (list.isEmpty()) {
                        Toast.makeText(getContext(), "No entrants to notify.", Toast.LENGTH_SHORT).show();
                    } else {
                        showNotificationDialog();
                    }
                });
            }
        });
        // observe user message LiveData and display it as a Toast
        viewModel.getUserMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.getStatus().observe(getViewLifecycleOwner(), status -> {
            if (status != null) {
                updateActiveChip(status);
            }
        });
    }

    /**
     * Updates the highlighted chip based on the string status.
     * Useful for setting the initial state.
     */
    private void updateActiveChip(String status) {
        int chipId;
        switch (status.toLowerCase()) {
            case "invited":
                chipId = R.id.chip_invited;
                break;
            case "accepted":
                chipId = R.id.chip_accepted;
                break;
            case "cancelled":
                chipId = R.id.chip_cancelled;
                break;
            case "waiting":
            default:
                chipId = R.id.chip_waiting;
                break;
        }

        if (statusChipGroup.getCheckedChipId() != chipId) {
            statusChipGroup.check(chipId);
        }
    }

    /**
     * Displays a dialog allowing the organizer to input a message that will be
     * sent as a notification to all entrants currently shown in the list.
     * Provides input validation and triggers the ViewModel's bulk notification
     * method when confirmed.
     */
    private void showNotificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Notification Message");
        final EditText input = new EditText(getContext());
        input.setHint("Enter message...");
        builder.setView(input);
        builder.setPositiveButton("Notify All", (dialog, which) -> {
            /**
             * Callback triggered when the "Notify All" button in the notification dialog is
             * pressed. Retrieves the user's input from the EditText and triggers the ViewModel's
             * bulk notification method.
             * @param dialog the dialog that triggered the callback
             */
            String organizerMessage = input.getText().toString().trim();
            viewModel.notifyAllEntrants(organizerMessage);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}