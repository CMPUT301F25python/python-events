package com.example.lotteryevent.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;
import com.example.lotteryevent.adapters.EventAdapter;
import com.example.lotteryevent.repository.AvailableEventsRepositoryImpl;
import com.example.lotteryevent.repository.IAvailableEventsRepository;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.example.lotteryevent.viewmodels.AdminEventsViewModel;

/**
 * This fragment displays a list of all events for the admin to choose from when viewing notifications sent by organizers
 * Displays recyclerView of all events and navigates to NotificationFragment when event is selected
 */
public class AdminSelectEventFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private AdminEventsViewModel viewModel;

    /**
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_admin_event_select, container, false);
    }

    /**
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.admin_event_select_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new EventAdapter(R.layout.tile_event);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(event -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getEventId());

            Navigation.findNavController(view)
                    .navigate(R.id.action_adminSelectEventFragment_to_notificationsFragment, bundle);
        });

        IAvailableEventsRepository repo = new AvailableEventsRepositoryImpl();
        GenericViewModelFactory factory = new GenericViewModelFactory();
        factory.put(AdminEventsViewModel.class, () -> new AdminEventsViewModel(repo));

        viewModel = new ViewModelProvider(this, factory).get(AdminEventsViewModel.class);

        viewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            if (events != null) adapter.setEvents(events);
        });

        viewModel.fetchEvents();
    }
}
