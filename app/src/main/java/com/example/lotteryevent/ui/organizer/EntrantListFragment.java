package com.example.lotteryevent.ui.organizer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
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
import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.repository.EventRepositoryImpl;
import com.example.lotteryevent.repository.IEventRepository;
import com.example.lotteryevent.repository.INotificationRepository;
import com.example.lotteryevent.repository.NotificationRepositoryImpl;
import com.example.lotteryevent.viewmodels.EntrantListViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

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
    private Button sendNotificationButton;

    // --- Data ---
    private String eventId;
    private String status;

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

        // --- ViewModel Initialization ---
        // If a factory was not injected (production), create the default one.
        if (viewModelFactory == null) {
            IEventRepository eventRepository = new EventRepositoryImpl();
            INotificationRepository notificationRepository = new NotificationRepositoryImpl(getContext());
            GenericViewModelFactory factory = new GenericViewModelFactory();
            factory.put(EntrantListViewModel.class, () -> new EntrantListViewModel(eventRepository, notificationRepository));
            viewModelFactory = factory;
        }

        // Get the ViewModel instance using the determined factory.
        viewModel = new ViewModelProvider(this, viewModelFactory).get(EntrantListViewModel.class);

        titleTextView.setText(capitalizeFirstLetter(status));

        // --- The rest of the method is the same ---
        setupObservers();
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
        sendNotificationButton = view.findViewById(R.id.send_notification_button);
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
    /**
     * Sets up observers on the ViewModel's LiveData.
     * This is the core of the reactive UI.
     */
    private void setupObservers() {
        if (eventId == null || status == null) {
            Toast.makeText(getContext(), "Error: Event data not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        viewModel.getEntrants(eventId, status).observe(getViewLifecycleOwner(), list -> {
            progressBar.setVisibility(View.GONE);
            if (list != null) {
                adapter.updateEntrants(list);
                Log.d(TAG, "Fetched " + list.size() + " entrants with status: " + status);
                sendNotificationButton.setOnClickListener(v -> showNotificationDialog(list));
            } else {
                Toast.makeText(getContext(), "Failed to load entrants.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Displays notification dialog for organizer to enter a message to send to all entrants
     * in the shown list. When notify all button is pressed, a notif is sent to each entrant
     */
    private void showNotificationDialog(List<Entrant> currentEntrantsList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Notification Message");
        final EditText input = new EditText(getContext());
        input.setHint("Enter message...");
        builder.setView(input);
        builder.setPositiveButton("Notify All", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String organizerMessage = input.getText().toString().trim();
                if (!organizerMessage.isEmpty()) {
                    viewModel.notifyAllEntrants(currentEntrantsList, eventId, organizerMessage);
                } else {
                    Toast.makeText(getContext(), "No message entered to send", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}