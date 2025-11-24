package com.example.lotteryevent.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.lotteryevent.BottomUiState;
import com.example.lotteryevent.R;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.EventDetailsRepositoryImpl;
import com.example.lotteryevent.repository.IEventDetailsRepository;
import com.example.lotteryevent.viewmodels.EventDetailsViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.google.firebase.Timestamp;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * A Fragment that displays the details of a single event.
 * This class follows MVVM principles, delegating all business and data logic
 * to the {@link EventDetailsViewModel}. Its sole responsibility is to render the UI
 * based on the state provided by the ViewModel.
 */
public class EventDetailsFragment extends Fragment {

    // --- UI Components ---
    private ListView detailsList;
    private Button btnActionPositive, btnActionNegative, btnDeleteEvent;
    private TextView textInfoMessage;
    private ProgressBar bottomProgressBar;
    private LinearLayout buttonActionsContainer;

    private ArrayAdapter<String> listAdapter;
    private final ArrayList<String> dataList = new ArrayList<>();

    // --- ViewModel ---
    private EventDetailsViewModel viewModel;
    private ViewModelProvider.Factory viewModelFactory;

    /**
     * Default constructor for production use by the Android Framework.
     */
    public EventDetailsFragment() {
    }

    /**
     * Constructor for testing. Allows us to inject a custom ViewModelFactory.
     *
     * @param factory The factory to use for creating the ViewModel.
     */
    public EventDetailsFragment(ViewModelProvider.Factory factory) {
        this.viewModelFactory = factory;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- ViewModel Initialization ---
        if (viewModelFactory == null) {
            IEventDetailsRepository repository = new EventDetailsRepositoryImpl();
            GenericViewModelFactory factory = new GenericViewModelFactory();
            factory.put(EventDetailsViewModel.class, () -> new EventDetailsViewModel(repository));
            viewModelFactory = factory;
        }
        viewModel = new ViewModelProvider(this, viewModelFactory).get(EventDetailsViewModel.class);

        initializeViews(view);
        listAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, dataList);
        detailsList.setAdapter(listAdapter);
        setupClickListeners();
        setupObservers();

        // Check if user is an admin before showing the delete button
        viewModel.checkAdminStatus();

        viewModel.getIsAdmin().observe(getViewLifecycleOwner(), isAdmin -> {
            if (isAdmin) {
                btnDeleteEvent.setVisibility(View.VISIBLE);
            } else {
                btnDeleteEvent.setVisibility(View.GONE);
            }
        });

        btnDeleteEvent.setOnClickListener(v -> {
            if (getArguments() != null) {
                String eventId = getArguments().getString("eventId");
                if (eventId != null) {
                    showDeleteConfirmationDialog(eventId);
                }
            }
        });

        // --- Initial Action ---
        if (getArguments() != null) {
            String eventId = getArguments().getString("eventId");
            viewModel.loadEventDetails(eventId);
        }
    }

    private void initializeViews(View v) {
        detailsList = v.findViewById(R.id.details_list);
        buttonActionsContainer = v.findViewById(R.id.button_actions_container);
        btnActionPositive = v.findViewById(R.id.btn_action_positive);
        btnActionNegative = v.findViewById(R.id.btn_action_negative);
        btnDeleteEvent = v.findViewById(R.id.btn_remove_event);
        textInfoMessage = v.findViewById(R.id.text_info_message);
        bottomProgressBar = v.findViewById(R.id.bottom_progress_bar);
    }

    private void setupClickListeners() {
        btnActionPositive.setOnClickListener(v -> viewModel.onPositiveButtonClicked());
        btnActionNegative.setOnClickListener(v -> viewModel.onNegativeButtonClicked());
    }

    private void setupObservers() {
        // Observer for the main event data.
        viewModel.eventDetails.observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                bindEventDetails(event);
            }
        });

        // Observer for any user-facing messages from the repository.
        viewModel.message.observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsDeleted().observe(getViewLifecycleOwner(), isDeleted -> {
            if (isDeleted) {
                Navigation.findNavController(requireView()).navigateUp();
            }
        });

        // The primary observer for the dynamic bottom bar.
        // It receives a simple state object and renders the UI accordingly.
        viewModel.bottomUiState.observe(getViewLifecycleOwner(), uiState -> {
            if (uiState != null) {
                renderBottomUi(uiState);
            }
        });
    }

    /**
     * Renders the state that the ViewModel has already calculated.
     */
    private void renderBottomUi(BottomUiState uiState) {
        hideAllBottomActions(); // Start by hiding everything.


        switch (uiState.type) {
            case LOADING:
                bottomProgressBar.setVisibility(View.VISIBLE);
                break;
            case SHOW_INFO_TEXT:
                showInfoText(uiState.infoText);
                break;
            case SHOW_ONE_BUTTON:
                showOneButton(uiState.positiveButtonText);
                break;
            case SHOW_TWO_BUTTONS:
                showTwoButtons(uiState.positiveButtonText, uiState.negativeButtonText);
                break;
        }
    }

    private void bindEventDetails(Event event) {
        dataList.clear();
        addAny("Name", event.getName());
        addAny("Organizer", event.getOrganizerName());
        addAny("Location", event.getLocation());
        addAny("Date and Time", event.getEventStartDateTime());
        addAny("Price", event.getPrice());
        addAny("Description", event.getDescription());
        addAny("Lottery Guidelines", event.getLotteryGuidelines());
        addAny("Max Attendees", event.getCapacity());
        listAdapter.notifyDataSetChanged();
    }

    // --- UI Helper Methods  ---

    private void hideAllBottomActions() {
        buttonActionsContainer.setVisibility(View.GONE);
        textInfoMessage.setVisibility(View.GONE);
        bottomProgressBar.setVisibility(View.GONE);
    }

    private void showInfoText(String message) {
        textInfoMessage.setText(message);
        textInfoMessage.setVisibility(View.VISIBLE);
    }

    private void showOneButton(String text) {
        btnActionNegative.setVisibility(View.GONE);
        btnActionPositive.setText(text);
        buttonActionsContainer.setVisibility(View.VISIBLE);
    }

    private void showTwoButtons(String positiveText, String negativeText) {
        btnActionPositive.setText(positiveText);
        btnActionNegative.setText(negativeText);
        btnActionNegative.setVisibility(View.VISIBLE);
        buttonActionsContainer.setVisibility(View.VISIBLE);
    }

    private final DateFormat DF = new SimpleDateFormat("EEE, MMM d yyyy • h:mm a", Locale.getDefault());

    private void addAny(String label, @Nullable Object raw) {
        if (raw == null) return;
        String v;
        if (raw instanceof Timestamp) {
            v = DF.format(((Timestamp) raw).toDate());
        } else if (raw instanceof Date) {
            v = DF.format((Date) raw);
        } else if (raw instanceof Number && label.equals("Price")) {
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.getDefault());
            v = nf.format(((Number) raw).doubleValue());
        } else {
            v = raw.toString();
        }
        v = v.trim();
        if (v.isEmpty() || "null".equalsIgnoreCase(v)) return;
        dataList.add(label + ": " + v);
    }

    /**
     * Displays a confirmation dialog to the user before deleting an event.
     * <p>
     * If the user selects "Delete", the event deletion process is initiated via the ViewModel.
     * If the user selects "Cancel", the dialog is dismissed without any action.
     *
     * @param eventId The unique identifier of the event to be deleted.
     */
    private void showDeleteConfirmationDialog(String eventId) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // User confirmed, proceed with deletion
                    viewModel.deleteEvent(eventId);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User cancelled, do nothing
                    dialog.dismiss();
                })
                .show();
    }

}