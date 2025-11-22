package com.example.lotteryevent.ui.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.lotteryevent.adapters.InvitedEntrantsAdapter;
import com.example.lotteryevent.repository.EntrantListRepositoryImpl;
import com.example.lotteryevent.viewmodels.EntrantListViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

import java.util.ArrayList;

/**
 * Fragment responsible for displaying and managing entrants who have been "invited"
 * to an event via the lottery system.
 * <p>
 * This Fragment follows the MVVM architecture:
 * <ul>
 *     <li><b>View:</b> Displays a RecyclerView of {@link com.example.lotteryevent.data.Entrant} objects.</li>
 *     <li><b>ViewModel:</b> {@link EntrantListViewModel} manages the state and handles data fetching.</li>
 *     <li><b>Repository:</b> Handles the backend logic for cancelling invitations and updating Firestore.</li>
 * </ul>
 * The organizer can cancel an invitation, which returns the entrant to the "waiting" list.
 * </p>
 *
 * @see EntrantListViewModel
 * @see InvitedEntrantsAdapter
 */
public class ManageInvitedFragment extends Fragment {

    private static final String STATUS_INVITED = "invited";

    // --- UI Components ---
    private RecyclerView recyclerView;
    private TextView titleTextView;
    private ProgressBar progressBar;
    private InvitedEntrantsAdapter adapter;

    // --- Data & Logic ---
    private String eventId;
    private EntrantListViewModel viewModel;
    private ViewModelProvider.Factory viewModelFactory;

    /**
     * Required empty public constructor for fragment instantiation by the system.
     */
    public ManageInvitedFragment() {
        // Required empty public constructor
    }

    /**
     * Constructor for testing purposes, allowing injection of a mock ViewModelFactory.
     *
     * @param factory The factory to use for creating the ViewModel.
     */
    public ManageInvitedFragment(GenericViewModelFactory factory) {
        this.viewModelFactory = factory;
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * Initializes the ViewModel with the specific eventId and "invited" status filter.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Retrieve the Event ID passed from the previous screen
        if (getArguments() != null) {
            this.eventId = getArguments().getString("eventId");
        }

        // Initialize the ViewModel Factory if not injected (Production case)
        if (viewModelFactory == null) {
            EntrantListRepositoryImpl repository = new EntrantListRepositoryImpl(getContext());
            GenericViewModelFactory factory = new GenericViewModelFactory();
            factory.put(EntrantListViewModel.class,
                    () -> new EntrantListViewModel(repository, eventId, STATUS_INVITED));
            viewModelFactory = factory;
        }

        // Obtain the ViewModel scoped to this Fragment
        viewModel = new ViewModelProvider(this, viewModelFactory).get(EntrantListViewModel.class);

        return inflater.inflate(R.layout.fragment_manage_invited, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView} has returned.
     * Sets up the UI components, RecyclerView, and LiveData observers.
     *
     * @param view               The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();
        setupObservers();
    }

    /**
     * Initializes UI references from the layout XML.
     *
     * @param view The root view of the fragment.
     */
    private void initializeViews(View view) {
        // IDs match the refactored XML provided previously
        titleTextView = view.findViewById(R.id.titleInvitedUsers);
        recyclerView = view.findViewById(R.id.recyclerViewInvited);
        progressBar = view.findViewById(R.id.progressBarInvited);

        // Set dynamic title based on ViewModel logic
        if (titleTextView != null) {
            titleTextView.setText(viewModel.getCapitalizedStatus());
        }
    }

    /**
     * Configures the RecyclerView with the {@link InvitedEntrantsAdapter}.
     * Defines the callback for the "Cancel" button, delegating the logic to the ViewModel.
     */
    private void setupRecyclerView() {
        // Initialize adapter with an empty list initially.
        // The callback lambda handles the button click: userId -> viewModel.cancelInvite(userId)
        adapter = new InvitedEntrantsAdapter(new ArrayList<>(), userId -> {
            // Delegate the business logic to the ViewModel
            viewModel.cancelInvite(userId);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Sets up observers for the ViewModel's LiveData.
     * Updates the UI automatically when data changes or messages are received.
     */
    private void setupObservers() {
        if (eventId == null) {
            Toast.makeText(getContext(), "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading indicator initially
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // Observe the list of entrants
        viewModel.getEntrants().observe(getViewLifecycleOwner(), entrants -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);

            if (entrants != null) {
                // Update the adapter with the new list of Entrant objects
                adapter.setEntrants(entrants);
            }
        });

        // Observe user feedback messages (e.g., "User returned to waitlist")
        viewModel.getUserMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}