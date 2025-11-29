package com.example.lotteryevent.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.R;
import com.example.lotteryevent.adapters.NotificationAdapter;
import com.example.lotteryevent.repository.INotificationRepository;
import com.example.lotteryevent.repository.NotificationRepositoryImpl;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.example.lotteryevent.viewmodels.NotificationsViewModel;

/**
 * A Fragment that displays a list of notifications for the user.
 * This class follows MVVM principles, delegating all business and data logic
 * to the {@link NotificationsViewModel}.
 */
public class NotificationsFragment extends Fragment {

    // --- UI Components ---
    private Button markSeenBtn;
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;

    // --- ViewModel ---
    private NotificationsViewModel viewModel;
    private ViewModelProvider.Factory viewModelFactory;
    private TextView noNotification;
    private NotificationCustomManager notificationCustomManager;

    /**
     * Default constructor for production use by the Android Framework.
     */
    public NotificationsFragment() {}

    /**
     * Constructor for testing. Allows us to inject a custom ViewModelFactory.
     * @param factory The factory to use for creating the ViewModel.
     */
    public NotificationsFragment(ViewModelProvider.Factory factory) {
        this.viewModelFactory = factory;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationCustomManager = new NotificationCustomManager(requireContext());

        noNotification = view.findViewById(R.id.text_no_notifications);

        // Get eventId for for admin
        String eventIdFilter = null;
        if (getArguments() != null) {
            eventIdFilter = getArguments().getString("eventId");
        }

        // --- ViewModel Initialization ---
        if (viewModelFactory == null) {
            // Production path: create dependencies manually.
            INotificationRepository repository = new NotificationRepositoryImpl(getContext());
            GenericViewModelFactory factory = new GenericViewModelFactory();
            factory.put(NotificationsViewModel.class, () -> new NotificationsViewModel(repository));
            viewModelFactory = factory;
        }
        viewModel = new ViewModelProvider(this, viewModelFactory).get(NotificationsViewModel.class);

        // --- UI Setup ---
        setupRecyclerView(view);
        setupObservers(view, eventIdFilter);

        // --- Initial Action ---
        // Pass the notificationId from arguments (if any) to the ViewModel to process.
        String notificationId = (getArguments() != null) ? getArguments().getString("notificationId") : null;
        viewModel.processInitialNotification(notificationId, notificationCustomManager);
    }

    /**
     * Initializes the RecyclerView and its Adapter. The item click listener now
     * delegates the event directly to the ViewModel.
     */
    private void setupRecyclerView(@NonNull View view) {
        markSeenBtn = view.findViewById(R.id.mark_as_seen_btn);
        recyclerView = view.findViewById(R.id.notifications_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationAdapter(R.layout.item_notification);
        recyclerView.setAdapter(adapter);

        markSeenBtn.setOnClickListener(v -> {
            viewModel.onMarkAllSeenClicked(notificationCustomManager);
            notificationCustomManager.clearNotifications();
        });
        adapter.setOnItemClickListener(notification -> viewModel.onNotificationClicked(notification, notificationCustomManager));
    }

    /**
     * Sets up observers on the ViewModel's LiveData to react to data and state changes.
     */
    private void setupObservers(@NonNull View view, String eventIdFilter) {
        if(eventIdFilter != null) {
            viewModel.loadNotificationsForEvent(eventIdFilter);

            // Show notifications for Admin
            viewModel.getNotificationsForEvent().observe(getViewLifecycleOwner(), notifications -> {
                if (notifications != null && !notifications.isEmpty()) {
                    adapter.setNotifications(notifications);
                    recyclerView.setVisibility(View.VISIBLE);
                    noNotification.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.GONE);
                    noNotification.setVisibility(View.VISIBLE);
                }
            });
        } else {
            // Observe the list of notifications and submit it to the adapter when it changes.
            viewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
                if (notifications != null && !notifications.isEmpty()) {
                    adapter.setNotifications(notifications);
                    recyclerView.setVisibility(View.VISIBLE);
                    noNotification.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.GONE);
                    noNotification.setVisibility(View.VISIBLE);
                }
            });
        }

        // Observe for user-facing messages (errors, etc.) and show them in a Toast.
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe the navigation event.
        viewModel.getNavigateToEventDetails().observe(getViewLifecycleOwner(), eventId -> {
            if (eventId != null) {
                // An eventId was posted, so we need to navigate.
                Bundle bundle = new Bundle();
                bundle.putString("eventId", eventId);
                Navigation.findNavController(view)
                        .navigate(R.id.action_notificationsFragment_to_eventDetailsFragment, bundle);

                // Tell the ViewModel that we have handled the navigation event.
                // This prevents the navigation from re-triggering on screen rotation.
                viewModel.onNavigationComplete();
            }
        });
    }
}