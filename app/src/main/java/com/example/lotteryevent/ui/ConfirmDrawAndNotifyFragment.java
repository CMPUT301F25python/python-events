package com.example.lotteryevent.ui;

import android.os.Bundle;
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

import com.example.lotteryevent.R;
import com.example.lotteryevent.repository.EventRepositoryImpl;
import com.example.lotteryevent.repository.IEventRepository;
import com.example.lotteryevent.viewmodels.ConfirmDrawAndNotifyViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

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
    private Button btnActionPositive;
    private Button btnActionNegative;
    private TextView textInfoMessage;
    private ProgressBar bottomProgressBar;
    private LinearLayout buttonActionsContainer;
    private TextView waitingListCountText;
    private TextView availableSpaceCountText;
    private TextView selectedUsersCountText;

    // --- ViewModel ---
    private ConfirmDrawAndNotifyViewModel viewModel;
    private ViewModelProvider.Factory viewModelFactory;

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
        return inflater.inflate(R.layout.fragment_confirm_draw_and_notify, container, false);
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
            viewModel.loadEventAndEntrants(eventId);
        }
    }

    /**
     * Initializes view's components on screen
     * @param v view to initialize for
     */
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

    /**
     * Sets up click listeners for the confirm and notify and cancel buttons
     */
    private void setupClickListeners() {
        btnActionPositive.setOnClickListener(v -> viewModel.onPositiveButtonClicked());
        btnActionNegative.setOnClickListener(v -> viewModel.onNegativeButtonClicked());
    }

    /**
     * Sets up observers for counts, message, and screen state to display
     */
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

    /**
     * Binds to waiting list count on screen
     * @param waitingListCount number of entrants in waiting list
     */
    private void bindWaitingListCount(String waitingListCount) {
        waitingListCountText.setText(waitingListCount);
    }

    /**
     * Binds to selected users count on screen
     * @param selectedUsersCount number of entrants selected
     */
    private void bindSelectedUsersCount(String selectedUsersCount) {
        selectedUsersCountText.setText(selectedUsersCount);
    }

    /**
     * Binds to available space count on screen
     * @param availableSpaceCount number of spaces available after draw
     */
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

    /**
     * Hides all UI specific to states
     */
    private void hideAllBottomActions() {
        buttonActionsContainer.setVisibility(View.GONE);
        textInfoMessage.setVisibility(View.GONE);
        bottomProgressBar.setVisibility(View.GONE);
    }

    /**
     * Shows Info text on screen
     * @param message text to show
     */
    private void showInfoText(String message) {
        textInfoMessage.setText(message);
        textInfoMessage.setVisibility(View.VISIBLE);
    }

    /**
     * Shows two buttons
     * @param positiveText text for confirm and notify
     * @param negativeText text for cancel
     */
    private void showTwoButtons(String positiveText, String negativeText) {
        btnActionPositive.setText(positiveText);
        btnActionNegative.setText(negativeText);
        btnActionNegative.setVisibility(View.VISIBLE);
        buttonActionsContainer.setVisibility(View.VISIBLE);
    }
}
