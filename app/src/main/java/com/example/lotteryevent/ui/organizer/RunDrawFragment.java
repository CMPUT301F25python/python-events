package com.example.lotteryevent.ui.organizer;

import android.os.Bundle;
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
import androidx.navigation.Navigation;

import com.example.lotteryevent.R;
import com.example.lotteryevent.repository.RunDrawRepositoryImpl;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.example.lotteryevent.viewmodels.RunDrawViewModel;

/**
 * This fragment allows the organizer to run the draw for an event.
 *
 * <p>
 *     MVVM refactored version, this Fragment now ONLY handles:
 *     - Rendering the UI
 *     - Receiving user input
 *     - Observing LiveData from the ViewModel
 *     - Navigating to the next screen on success
 *
 *     All Firestore operations, selection logic, and entrant updates
 *     are now handled inside the ViewModel + Repository layers.
 * </p>
 */
public class RunDrawFragment extends Fragment {


    private EditText numSelectedEntrants;
    private TextView waitingListCountText;
    private TextView availableSpaceCountText;
    private ProgressBar progressBar;

    // Buttons
    private Button runDrawButton;
    private Button cancelButton;

    // EventId passed from previous fragment
    private String eventId = "temporary filler for event ID";
    private String oldEntrantsStatus;
    private String newChosenEntrants;
    private String newUnchosenEntrants;

    // ViewModel
    private RunDrawViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_run_draw, container, false);
    }

    /**
     * Called after the view is created. This is where:
     * - Views are initialized
     * - ViewModel is created with dependency injection
     * - Observers are connected to LiveData
     * - Button click listeners are set
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retrieve eventId passed from organizer event page
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }

        initializeViews(view);
        initializeViewModel();
        setupObservers();
        setupListeners(view);

        // Fetch metrics from Firestore through ViewModel to Repository
        viewModel.loadMetrics(eventId);
    }

    /**
     * Initializes and binds UI components using XML IDs.
     *
     */
    private void initializeViews(View v) {
        numSelectedEntrants = v.findViewById(R.id.numSelectedEntrants);
        waitingListCountText = v.findViewById(R.id.waiting_list_count);
        availableSpaceCountText = v.findViewById(R.id.available_space_count);

        runDrawButton = v.findViewById(R.id.runDrawButton);
        cancelButton = v.findViewById(R.id.cancel_button);

    }

    /**
     * Sets up the ViewModel using the GenericViewModelFactory.
     * This allows unit tests to inject fake repositories.
     */
    private void initializeViewModel() {
        RunDrawRepositoryImpl repo = new RunDrawRepositoryImpl(requireContext());

        GenericViewModelFactory factory = new GenericViewModelFactory();
        factory.put(RunDrawViewModel.class, () -> new RunDrawViewModel(repo));

        viewModel = new ViewModelProvider(this, factory).get(RunDrawViewModel.class);
    }

    /**
     * Observes all LiveData from the ViewModel.
     * Any UI updates are made here, keeping the Fragment "dumb".
     *
     * This ensures:
     * - No business logic in Fragment
     * - Only rendering UI based on observable state
     */
    private void setupObservers() {

        /**
         * Observes for waiting list count and shows on screen
         * @param count count to show
         */
        viewModel.waitingListCount.observe(getViewLifecycleOwner(), count -> {
            if (count != null) {
                waitingListCountText.setText(String.valueOf(count));
            }
        });

        /**
         * Observes for available space count and shows on screen
         * @param count count to show
         */
        viewModel.availableSpaceCount.observe(getViewLifecycleOwner(), count -> {
            if (count == null || count < 0) {
                availableSpaceCountText.setText("No Limit");
            } else {
                availableSpaceCountText.setText(String.valueOf(count));
            }
        });

        /**
         * Observes message, shows toast on value
         * @param msg to show
         */
        viewModel.message.observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        /**
         * Observes loading, shows draw button conditionally
         * @param isLoading boolean for loading
         */
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            runDrawButton.setEnabled(!isLoading);
        });

        /**
         * Observes old entrants status
         * @param oldEntrantStatus old status of entrants
         */
        viewModel.oldEntrantsStatus.observe(getViewLifecycleOwner(), oldEntrantsStatus -> {
            this.oldEntrantsStatus = oldEntrantsStatus;
        });

        /**
         * Observes new chosen entrants status
         * @param newChosenEntrants new chosen entrants
         */
        viewModel.newChosenEntrants.observe(getViewLifecycleOwner(), newChosenEntrants -> {
            this.newChosenEntrants = newChosenEntrants;
        });

        /**
         * Observes new unchosen entrants status
         * @param newUnchosenEntrants new unchosen entrants
         */
        viewModel.newUnchosenEntrants.observe(getViewLifecycleOwner(), newUnchosenEntrants -> {
            this.newUnchosenEntrants = newUnchosenEntrants;
        });

        /**
         * Navigation trigger on successful draw
         * @param success boolean for success
         */
        viewModel.drawSuccess.observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", eventId);
                bundle.putString("oldEntrantsStatus", oldEntrantsStatus);
                bundle.putString("newChosenEntrants", newChosenEntrants);
                bundle.putString("newUnchosenEntrants", newUnchosenEntrants);

                Navigation.findNavController(requireView())
                        .navigate(R.id.action_runDrawFragment_to_confirmDrawAndNotifyFragment, bundle);
            }
        });

        /**
         * Navigation trigger on successful cancel draw
         * @boolean success boolean for success
         */
        viewModel.cancelSuccess.observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Navigation.findNavController(requireView()).navigateUp();
            }
        });
    }

    /**
     * Sets up click listeners for the Run Draw and Cancel buttons.
     * Validates input and forwards commands to the ViewModel.
     */
    private void setupListeners(View root) {

        /**
         * Gets input for draw and validates before running draw
         * @param v view clicked
         */
        runDrawButton.setOnClickListener(v -> {

            String inputText = numSelectedEntrants.getText().toString().trim();

            if (inputText.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a number", Toast.LENGTH_SHORT).show();
                return;
            }

            int numToSelect;
            try {
                numToSelect = Integer.parseInt(inputText);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (numToSelect <= 0) {
                Toast.makeText(getContext(), "Number of participants must be greater than zero.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Ensure participants can't be more than available space
            Integer availableSpaces = viewModel.availableSpaceCount.getValue();
            if (availableSpaces != null && numToSelect > availableSpaces) {
                Toast.makeText(getContext(), "You cannot select more participants than available spaces (" + availableSpaces + ").", Toast.LENGTH_LONG).show();
                return;
            }

            // Ensure participants selected can't be greater than entrants on wait list
            // Ensure participants can't be more than available space
            Integer waitlistSize = viewModel.waitingListCount.getValue();
            if (waitlistSize != null && numToSelect > waitlistSize) {
                Toast.makeText(getContext(), "You cannot select more participants than are on the waiting list (" + waitlistSize + ").", Toast.LENGTH_LONG).show();
                return;
            }

            // Execute draw through ViewModel
            viewModel.runDraw(eventId, numToSelect);
        });

        /**
         * Navigates back
         * @param v view clicked
         */
        cancelButton.setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp()
        );
    }
}
