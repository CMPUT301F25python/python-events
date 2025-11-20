package com.example.lotteryevent.ui.organizer;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.R;
import com.example.lotteryevent.repository.EventDetailsRepositoryImpl;
import com.example.lotteryevent.repository.EventRepositoryImpl;
import com.example.lotteryevent.repository.IEventDetailsRepository;
import com.example.lotteryevent.repository.IEventRepository;
import com.example.lotteryevent.ui.organizer.ConfirmDrawAndNotifyFragment;
import com.example.lotteryevent.ui.organizer.ConfirmDrawAndNotifyFragmentDirections;
import com.example.lotteryevent.viewmodels.ConfirmDrawAndNotifyViewModel;
import com.example.lotteryevent.viewmodels.EventDetailsViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This fragment allows the organizer to view the number of entrants drawn, confirm draw, and notify the selected entrants.
 * <p>
 *     Organizer can view the number of entrants in waiting list, how many spots available for the event, and how many entrants were drawn.
 *     Organizer can click the "Confirm and Notify" button which confirms the users drawn and notifies the chosen entrants by creating
 *     notification documents for the user in Firebase. Organizer can also press "Cancel" which moves all entrants back to the waiting list.
 * </p>
 */
public class ConfirmDrawAndNotifyFragment extends Fragment {
    private static final String TAG = "ConfirmDrawAndNotifyFragment";
    //    private FirebaseFirestore db;
//    private NotificationCustomManager notifManager;
    private Button btnActionPositive;
    private Button btnActionNegative;
    private TextView textInfoMessage;
    private ProgressBar bottomProgressBar;
    private LinearLayout buttonActionsContainer;
    private TextView waitingListCountText;
    private TextView availableSpaceCountText;
    private TextView selectedUsersCountText;
//    private String eventId = "temporary filler for event ID";

    // --- ViewModel ---
    private ConfirmDrawAndNotifyViewModel viewModel;
    private ViewModelProvider.Factory viewModelFactory;

    private String eventId;

    /**
     * Default constructor for production use by the Android Framework.
     */
    public ConfirmDrawAndNotifyFragment() {
    }

    /**
     * Constructor for testing. Allows us to inject a custom ViewModelFactory.
     *
     * @param factory The factory to use for creating the ViewModel.
     */
    public ConfirmDrawAndNotifyFragment(ViewModelProvider.Factory factory) {
        this.viewModelFactory = factory;
    }


    /**
     * Inflates the layout for this fragment and gets event ID from bundle.
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return The view for the fragment's UI
     *
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_confirm_draw_and_notify, container, false);

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
        return view;
    }

    /**
     * UI initialization and set up including displaying user metrics to the screen
     * and setting up button click listeners.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- ViewModel Initialization ---
        if (viewModelFactory == null) {
            IEventRepository repository = new EventRepositoryImpl();
            GenericViewModelFactory factory = new GenericViewModelFactory();
            factory.put(ConfirmDrawAndNotifyViewModel.class, () -> new ConfirmDrawAndNotifyViewModel(repository, getContext(), getView()));
            viewModelFactory = factory;
        }
        viewModel = new ViewModelProvider(this, viewModelFactory).get(ConfirmDrawAndNotifyViewModel.class);

        initializeViews(view);
        setupClickListeners();
        setupObservers();

        // --- Initial Action ---
        if (getArguments() != null) {
            String eventId = getArguments().getString("eventId");
            viewModel.loadEventAndEntrantsStatus(eventId);
        }

//        confirmAndNotifyButton = view.findViewById(R.id.confirm_and_notify_button);
//        cancelButton = view.findViewById(R.id.cancel_button);
//
//        waitingListCountText = view.findViewById(R.id.waiting_list_count);
//        availableSpaceCountText = view.findViewById(R.id.available_space_count);
//        selectedUsersCountText = view.findViewById(R.id.selected_users_count);
//
//        notifManager = new NotificationCustomManager(getContext());
//
//        fillEntrantMetrics();
//        setupClickListeners(view);

    }

    private void initializeViews(View v) {
        buttonActionsContainer = v.findViewById(R.id.button_actions_container);
        btnActionPositive = v.findViewById(R.id.confirm_and_notify_button);
        btnActionNegative = v.findViewById(R.id.cancel_button);
        textInfoMessage = v.findViewById(R.id.text_info_message);
        bottomProgressBar = v.findViewById(R.id.bottom_progress_bar);
        waitingListCountText = v.findViewById(R.id.waiting_list_count);
        availableSpaceCountText = v.findViewById(R.id.available_space_count);
        selectedUsersCountText = v.findViewById(R.id.selected_users_count);
    }

    private void setupClickListeners() {
        btnActionPositive.setOnClickListener(v -> viewModel.onPositiveButtonClicked());
        btnActionNegative.setOnClickListener(v -> viewModel.onNegativeButtonClicked());
    }

    private void setupObservers() {
        viewModel.waitingListCount.observe(getViewLifecycleOwner(), waitingListCount -> {
            if (waitingListCount != null) {
                bindWaitingListCount(waitingListCount);
            }
        });

        viewModel.selectedUsersCount.observe(getViewLifecycleOwner(), selectedUsersCount -> {
            if (selectedUsersCount != null) {
                bindSelectedUsersCount(selectedUsersCount);
            }
        });

        viewModel.availableSpaceCount.observe(getViewLifecycleOwner(), availableSpaceCount -> {
            if (availableSpaceCount != null) {
                bindAvailableSpaceCount(availableSpaceCount);
            }
        });

        // Observer for any user-facing messages from the repository.
        viewModel.message.observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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

    private void bindWaitingListCount(String waitingListCount) {
        waitingListCountText.setText(waitingListCount);
    }

    private void bindSelectedUsersCount(String selectedUsersCount) {
        selectedUsersCountText.setText(selectedUsersCount);
    }

    private void bindAvailableSpaceCount(String availableSpaceCount) {
        availableSpaceCountText.setText(availableSpaceCount);
    }

    /**
     * Renders the state that the ViewModel has already calculated.
     */
    private void renderBottomUi(ConfirmDrawAndNotifyViewModel.BottomUiState uiState) {
        hideAllBottomActions(); // Start by hiding everything.

        System.out.println("State: " + uiState.type);
        switch (uiState.type) {
            case LOADING:
                bottomProgressBar.setVisibility(View.VISIBLE);
                break;
            case SHOW_INFO_TEXT:
                showInfoText(uiState.infoText);
                break;
            case SHOW_TWO_BUTTONS:
                showTwoButtons(uiState.positiveButtonText, uiState.negativeButtonText);
                break;
        }
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

    private void showTwoButtons(String positiveText, String negativeText) {
        btnActionPositive.setText(positiveText);
        btnActionNegative.setText(negativeText);
        btnActionNegative.setVisibility(View.VISIBLE);
        buttonActionsContainer.setVisibility(View.VISIBLE);
    }

//
//
//    /**
//     * Fills TextViews on the screen with entrant metrics including number of entrants in the waiting list,
//     * number of spaces available in the event, and number of entrants chosen by the lottery.
//     */
//    @SuppressLint("SetTextI18n")
//    private void fillEntrantMetrics() {
//        // Used to find the total number of entrants, adds from the waiting list and the selected list
//        AtomicInteger chosenEntrants = new AtomicInteger();
//
//        // Count invited entrants
//        db.collection("events").document(eventId)
//                .collection("entrants")
//                .whereEqualTo("status", "waiting")
//                .get()
//                .addOnSuccessListener(waitQuery -> {
//
//                    // displays the number of entrants in the waiting list
//                    waitingListCountText.setText(String.valueOf(waitQuery.size()));
//                })
//                .addOnFailureListener(e ->
//                        Toast.makeText(getContext(), "Error loading entrant counts", Toast.LENGTH_SHORT).show()
//                );
//
//        // Count invited entrants
//        db.collection("events").document(eventId)
//                .collection("entrants")
//                .whereEqualTo("status", "invited")
//                .get()
//                .addOnSuccessListener(querySelected -> {
//                    selectedUsersCountText.setText(String.valueOf(querySelected.size()));
//
//                    // Load event capacity and show available spots
//                    db.collection("events").document(eventId).get()
//                            .addOnSuccessListener(document -> {
//                                if (document != null && document.exists()) {
//                                    Long capacityLong = document.getLong("capacity");
//
//                                    if (capacityLong != null) {
//                                        int capacity = capacityLong.intValue();
//                                        // calculates and displays the capacity of the event left after the draw
//                                        int spacesLeft = capacity - querySelected.size();
//                                        if (spacesLeft > 0) {
//                                            availableSpaceCountText.setText(String.valueOf(spacesLeft));
//                                        } else {
//                                            availableSpaceCountText.setText("0");
//                                        }
//                                    } else {
//                                        availableSpaceCountText.setText("No Limit");
//                                    }
//                                } else {
//                                    Log.d(TAG, "No such document");
//                                    Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
//                                }
//                            })
//                            .addOnFailureListener(e -> {
//                                Log.w(TAG, "get failed with ", e);
//                                Toast.makeText(getContext(), "Error loading event's number of spots available", Toast.LENGTH_SHORT).show();
//                            });
//
//                })
//                .addOnFailureListener(e ->
//                        Toast.makeText(getContext(), "Error loading entrant counts", Toast.LENGTH_SHORT).show()
//                );
//    }
//
//    /**
//     * Sets up click listeners for the buttons.
//     * @param view The View from onViewCreated().
//     */
//    private void setupClickListeners(@NonNull View view) {
//        confirmAndNotifyButton.setOnClickListener(v -> confirmSelectedUsersAndNotify(view));
//        cancelButton.setOnClickListener(v -> cancelLottery(view));
//    }
//
//    /**
//     * Confirms that chosen users from the lottery indeed exist and notifies them of their selection.
//     * @param view The View from onViewCreated(). Used to navigate to another fragment
//     */
//    private void confirmSelectedUsersAndNotify(@NonNull View view) {
//        db.collection("events").document(eventId)
//                .collection("entrants")
//                .whereEqualTo("status", "invited")
//                .get()
//                .addOnSuccessListener(query -> {
//
//                    if (query.isEmpty()) {
//                        Toast.makeText(getContext(), "No chosen entrants to confirm and notify.", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//
//                    AtomicInteger completed = new AtomicInteger(0);
//                    int total = query.size();
//
//                    for (DocumentSnapshot d : query.getDocuments()) {
//                        String uid = d.getId();
//
//                        db.collection("users").document(uid).get()
//                                .addOnSuccessListener(user -> {
//                                    // Notify this entrant
//                                    notifyEntrant(uid);
//                                    updateAfterNotification(completed, total, view);
//                                })
//                                .addOnFailureListener(user -> {
//                                    Toast.makeText(getContext(), "User " + uid + " not found. Skipped.", Toast.LENGTH_SHORT).show();
//                                    updateAfterNotification(completed, total, view);
//                                });
//                    }
//                })
//                .addOnFailureListener(e ->
//                        Toast.makeText(getContext(), "Error loading selected entrants", Toast.LENGTH_SHORT).show()
//                );
//    }
//
//    private void updateAfterNotification(AtomicInteger completed, int total, View view) {
//        if (completed.incrementAndGet() == total) {
//            navigateBack(view);
//        }
//    }
//
//
//    /**
//     * Gets event's name, organizer id, organizer name, and creates message to send to notification manager's
//     * sendNotification() method to send the notification
//     * @param uid User ID, used in sending notification.
//     */
//    private void notifyEntrant(String uid) {
//        db.collection("events").document(eventId).get()
//                .addOnSuccessListener(document -> {
//                    if (document != null && document.exists()) {
//                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
//                        String eventName = document.getString("name");
//                        String organizerId = document.getString("organizerId");
//                        String organizerName = document.getString("organizerName");
//                        String title = "Congratulations!";
//                        String message = "You've been selected for " + eventName + "! Tap to accept or decline.";
//                        String type = "lottery_win";
//                        notifManager.sendNotification(uid, title, message, type, eventId, eventName, organizerId, organizerName);
//                    } else {
//                        Log.d(TAG, "No such document");
//                        Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.w(TAG, "get failed with ", e);
//                    Toast.makeText(getContext(), "Error sending notification to chosen entrant", Toast.LENGTH_SHORT).show();
//                });
//    }
//
//    /**
//     * Cancels the lottery by bringing entrants back to the waiting collection and switches fragment to the home fragment
//     * @param view Used to navigate to another fragment
//     */
//    private void cancelLottery(@NonNull View view) {
//        db.collection("events").document(eventId)
//                .collection("entrants")
//                .whereEqualTo("status", "invited")
//                .get()
//                .addOnSuccessListener(query -> {
//                    if (!query.isEmpty()) {
//                        for (DocumentSnapshot entrantDoc : query.getDocuments()) {
//                            entrantDoc.getReference().update("status", "waiting");
//                        }
//                        Toast.makeText(getContext(), "Lottery Cancelled", Toast.LENGTH_SHORT).show();
//                        navigateBack(view);
//
//                    }
//                })
//                .addOnFailureListener(e ->
//                        Toast.makeText(getContext(), "Error cancelling lottery", Toast.LENGTH_SHORT).show()
//                );
//    }
//
//    private void navigateBack(View view) {
//        ConfirmDrawAndNotifyFragmentDirections.ActionConfirmDrawAndNotifyFragmentToOrganizerEventPageFragment action =
//                ConfirmDrawAndNotifyFragmentDirections
//                        .actionConfirmDrawAndNotifyFragmentToOrganizerEventPageFragment(eventId);
//
//        Navigation.findNavController(view).navigate(action);
//    }
}
