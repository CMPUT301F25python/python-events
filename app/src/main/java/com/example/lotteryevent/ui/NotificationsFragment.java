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

    /**
     * Called by the system to have the fragment instantiate its user interface view.
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
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has returned,
     * but before any saved state has been restored in to the view.
     * Sets up view and its components.
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button markAsSeenBtn = view.findViewById(R.id.mark_as_seen_btn);

        boolean isAdminView = false;
        if (getArguments() != null) {
            isAdminView = getArguments().getBoolean("isAdminView", false);
        }

        if (isAdminView) {
            markAsSeenBtn.setVisibility(View.GONE);
        } else {
            markAsSeenBtn.setVisibility(View.VISIBLE);
        }

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
        setupRecyclerView(view, isAdminView);
        setupObservers(view, eventIdFilter);

        // --- Initial Action ---
        // Pass the notificationId from arguments (if any) to the ViewModel to process.
        String notificationId = (getArguments() != null) ? getArguments().getString("notificationId") : null;

        if (!isAdminView) {
            viewModel.processInitialNotification(notificationId, notificationCustomManager);
        }
    }

    /**
     * Initializes the RecyclerView and its Adapter.
     * <p>
     * This method configures the adapter based on the {@code isAdminView} flag:
     * <ul>
     *     <li><b>Admin Mode:</b> Sets the adapter to admin mode. This visually locks the background
     *     color (disabling read/unread toggling) and changes the click behavior to fetch
     *     and display the recipient's name instead of navigating.</li>
     *     <li><b>User Mode:</b> Sets standard behavior where clicking marks a notification
     *     as seen (changing color) and navigates to the event details.</li>
     * </ul>
     *
     * @param view        The root view of the fragment.
     * @param isAdminView True if the fragment is being viewed by an admin, false otherwise.
     */
    private void setupRecyclerView(@NonNull View view, boolean isAdminView) {
        markSeenBtn = view.findViewById(R.id.mark_as_seen_btn);
        recyclerView = view.findViewById(R.id.notifications_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new NotificationAdapter(R.layout.item_notification);
        adapter.setAdminView(isAdminView);
        recyclerView.setAdapter(adapter);

        /**
         * Marks all messages as seen and clears notification banners
         * @param v view clicked
         */
        markSeenBtn.setOnClickListener(v -> {
            viewModel.onMarkAllSeenClicked(notificationCustomManager);
            notificationCustomManager.clearNotifications();
        });
        /**
         * On notif click, marks it as seen and if lottery win, navigates to event
         * @param notification notif clicked
         */
        adapter.setOnItemClickListener(notification -> {
            if (isAdminView) {
                // 1. Admin Mode: Fetch the name using the ViewModel/Repository
                String userId = notification.getRecipientId();

                viewModel.fetchUserName(userId, name -> {
                    // 2. Show the Toast with the retrieved name
                    Toast.makeText(getContext(), "Sent to: " + name, Toast.LENGTH_SHORT).show();
                });
            } else {
                // User Mode: Proceed with normal logic (toggle seen, navigate to event)
                viewModel.onNotificationClicked(notification, notificationCustomManager);
            }
        });
    }

    /**
     * Sets up observers on the ViewModel's LiveData to react to data and state changes.
     */
    private void setupObservers(@NonNull View view, String eventIdFilter) {
        /**
         * Observe the list of notifications and submit it to the adapter when it changes.
         * @param notifications list of notifs
         */
        if(eventIdFilter != null) {
            viewModel.loadNotificationsForEvent(eventIdFilter);

            /**
             * Observe the list of notifications and submit it to the adapter when it changes.
             * @param notifications list of notifs
             */
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

        /**
         * Observe for user-facing messages (errors, etc.) and show them in a Toast.
         * @param message message to show
         */
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        /**
         * Observe the navigation event for navigation
         * @param eventId event to go to
         */
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