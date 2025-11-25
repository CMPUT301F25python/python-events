package com.example.lotteryevent.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import android.provider.Settings;
import android.content.Intent;
import android.net.Uri;
import android.app.AlertDialog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
    private Button btnActionPositive, btnActionNegative, btnDeleteEvent, btnDeleteOrganizer;
    private TextView textInfoMessage;
    private ProgressBar bottomProgressBar;
    private LinearLayout buttonActionsContainer, adminActionsContainer;

    private ArrayAdapter<String> listAdapter;
    private final ArrayList<String> dataList = new ArrayList<>();

    // --- ViewModel ---
    private EventDetailsViewModel viewModel;
    private ViewModelProvider.Factory viewModelFactory;

    // --- Location Services ---
    private FusedLocationProviderClient fusedLocationClient;

    /**
     * Handles the result of the system permission dialog.
     */
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    fetchLocationAndJoin();
                } else {
                    // The user denied the system dialog just now.
                    // We need to inform the ViewModel to reset the state so the button works again next time.
                    viewModel.onLocationPermissionDenied();

                    // Check if we should show a manual "Go to Settings" dialog
                    // If shouldShowRequestPermissionRationale is false after a denial,
                    // it means "Don't ask again" was checked.
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                        showSettingsDialog();
                    } else {
                        Toast.makeText(getContext(), "Location is required to join this event.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

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

        // --- Initialize Location Client ---
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());


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
                adminActionsContainer.setVisibility(View.VISIBLE);
            } else {
                adminActionsContainer.setVisibility(View.GONE);
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

        btnDeleteOrganizer.setOnClickListener(v -> {
            // Get the current event data from the ViewModel
            Event currentEvent = viewModel.eventDetails.getValue();

            if (currentEvent != null) {
                String organizerId = currentEvent.getOrganizerId();

                if (organizerId != null && !organizerId.isEmpty()) {
                    showDeleteOrganizerConfirmationDialog(organizerId);
                } else {
                    Toast.makeText(getContext(), "Error: Organizer ID not found.", Toast.LENGTH_SHORT).show();
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
        adminActionsContainer = v.findViewById(R.id.admin_actions_container);
        btnDeleteEvent = v.findViewById(R.id.btn_remove_event);
        btnDeleteOrganizer = v.findViewById(R.id.btn_remove_organizer);
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
                Integer count = viewModel.waitingListCount.getValue();
                bindEventDetails(event, count);
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

        viewModel.getIsOrganizerDeleted().observe(getViewLifecycleOwner(), isOrganizerDeleted -> {
            if (isOrganizerDeleted) {
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

        // Listener for location permission requests
        viewModel.requestLocationPermission.observe(getViewLifecycleOwner(), shouldRequest -> {
            if (shouldRequest != null && shouldRequest) {
                checkPermissionAndAct();
            }
        });

        // observer to display waitinglistCount
        viewModel.waitingListCount.observe(getViewLifecycleOwner(), count -> {
            if (count != null && viewModel.eventDetails.getValue() != null) {
                // Rebind details including the count
                bindEventDetails(viewModel.eventDetails.getValue(), count);
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

    private void bindEventDetails(Event event, @Nullable Integer waitingListCount) {
        dataList.clear();
        addAny("Name", event.getName());
        addAny("Organizer", event.getOrganizerName());
        addAny("Location", event.getLocation());
        addAny("Date and Time", event.getEventStartDateTime());
        addAny("Price", event.getPrice());
        addAny("Description", event.getDescription());
        addAny("Max Attendees", event.getCapacity());
        // check if waiting list count has a value otherwise set to loading
        if (waitingListCount == null){
            dataList.add("Waiting List Count: Loading...");
        } else {
            addAny("Waiting List Count", waitingListCount);
        }
        addAny("Geolocation Required", event.getGeoLocationRequired() ? "Yes" : "No");
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

    // --- Location Logic ---
    private void checkPermissionAndAct() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndJoin();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Case 2: User denied it before. Show an explanation dialog BEFORE launching the system dialog.
            new AlertDialog.Builder(requireContext())
                    .setTitle("Location Required")
                    .setMessage("This event requires geolocation verification to join. Please grant location permission.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        viewModel.onLocationPermissionDenied();
                        dialog.dismiss();
                    })
                    .create()
                    .show();
        } else {
            // Case 3: First time asking, OR user checked "Don't ask again" previously.
            // The system handles which one it is.
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Shows a dialog directing the user to Settings if they permanently denied permission
     */
    private void showSettingsDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Permission Denied")
                .setMessage("You have permanently denied location permission. To join this event, you must enable it in the app settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void fetchLocationAndJoin() {
        // Double-check permission before calling location services (Linter requirement)
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        // Pass data back to ViewModel
                        viewModel.onLocationRetrieved(location.getLatitude(), location.getLongitude());
                    } else {
                        // Edge case: GPS is on but no location cached.
                        Toast.makeText(getContext(), "Unable to determine location. Try opening Google Maps first.", Toast.LENGTH_LONG).show();
                        viewModel.onLocationPermissionDenied();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error getting location.", Toast.LENGTH_SHORT).show();
                    viewModel.onLocationPermissionDenied();
                });
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

    /**
     * Displays a confirmation dialog to the user before deleting an organizer.
     * <p>
     * If the user selects "Delete", the organizer deletion process is initiated via the ViewModel.
     * If the user selects "Cancel", the dialog is dismissed without any action.
     *
     * @param organizerId The unique identifier of the organizer to be deleted.
     */
    private void showDeleteOrganizerConfirmationDialog(String organizerId) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Organizer")
                .setMessage("Are you sure you want to delete this organizer? This action cannot be undone. This action will also delete all events organized by this organizer.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // User confirmed, proceed with deletion
                    viewModel.deleteOrganizer(organizerId);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User cancelled, do nothing
                    dialog.dismiss();
                })
                .show();
    }

}