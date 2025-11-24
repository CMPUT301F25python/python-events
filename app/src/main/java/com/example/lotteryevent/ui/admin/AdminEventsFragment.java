package com.example.lotteryevent.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;
import com.example.lotteryevent.adapters.AdminEventsAdapter;
import com.example.lotteryevent.repository.AdminEventsRepositoryImpl;
import com.example.lotteryevent.viewmodels.AdminEventsViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.google.android.material.button.MaterialButton;

/**
 * A Fragment designed for the oversight of events created within the app.
 * <p>
 * This view allows administrators to browse all active and past events. It provides
 * controls to remove events that are flagged or deemed invalid, ensuring the quality of app content.
 */
public class AdminEventsFragment extends Fragment {
    private AdminEventsViewModel viewModel;
    private RecyclerView recyclerView;
    private AdminEventsAdapter adapter;

    // Call existing available events xml
    public AdminEventsFragment() { }

    /**
     * Inflates layour for admin events screen
     * <p>
     *     Uses existing fragment_available_events layout, with user buttons hidden for admin view
     * </p>
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_available_events, container, false);
    }

    /**
     * Called after fragment view created
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        // Set up ViewModel factory for admin
        GenericViewModelFactory factory = new GenericViewModelFactory();
        factory.put(AdminEventsViewModel.class,
                () -> new AdminEventsViewModel(new AdminEventsRepositoryImpl()));

        viewModel = new ViewModelProvider(this, factory).get(AdminEventsViewModel.class);

        // Setup UI
        setupRecyclerView(view);
        hideParticipantsButtons(view);
        setupObservers();

        // Allow admin to see all events
        viewModel.fetchAllEvents();
    }

    /**
     * Sets up RecyclerView to display events
     * <p>
     *     Intialize layout manager and attach admin specific adapter
     *     Click navigation added to event details screen
     * </p>
     * @param view
     */
    private void setupRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.events_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new AdminEventsAdapter(event -> {
            if (event.getEventId() == null) {
                Toast.makeText(requireContext(), "Invalid event id", Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle args = new Bundle();
            args.putString("eventId", event.getEventId());

            NavController nav = NavHostFragment.findNavController(AdminEventsFragment.this);
            nav.navigate(R.id.action_adminEventsFragment_to_eventDetailsFragment, args);
        });

        recyclerView.setAdapter(adapter);
    }

    /**
     * Hides UI buttons intended only for user view: "Available today" and "new Event" Buttons
     * Filter button also temporarily hidden but can be later added
     * @param view
     * View containing buttons
     */
    private void hideParticipantsButtons(View view) {
        MaterialButton availableToday = view.findViewById(R.id.available_today_button);

        availableToday.setVisibility(view.GONE);

        // Filter button depending on what we decide later
        view.findViewById(R.id.filter_button).setVisibility(View.GONE);
    }

    /**
     * Gets LiveData to update UI
     * <ul>
     *     <li>Updates adapter</li>
     *     <li>Displays toast depending on error</li>
     * </ul>
     */
    private void setupObservers() {
        viewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            if (events != null) {
                adapter.setEvents(events);
            }
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

}
