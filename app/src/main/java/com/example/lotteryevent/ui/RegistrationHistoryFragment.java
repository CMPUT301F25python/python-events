package com.example.lotteryevent.ui;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lotteryevent.R;
import com.example.lotteryevent.data.RegistrationHistoryItem;
import com.example.lotteryevent.repository.IRegistrationHistoryRepository;
import com.example.lotteryevent.repository.RegistrationHistoryRepositoryImpl;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.example.lotteryevent.viewmodels.RegistrationHistoryViewModel;

import java.util.List;

/**
 * Fragment responsible for displaying the current user's registration history
 * for lottery events. Observes a {@link RegistrationHistoryViewModel} to render
 * a list of past registrations and their statuses in a vertical layout, and
 * shows error or info messages via Toasts.
 */

public class RegistrationHistoryFragment extends Fragment {

    private RegistrationHistoryViewModel viewModel;
    private ViewModelProvider.Factory viewModelFactory;
    private TextView drawResults;
    private TextView noHistoryText;
    private LinearLayout containerDrawResults;

    /**
     * This is the required empty constructor
     */
    public RegistrationHistoryFragment(){
    }

    /**
     * Constructor for testing. Allows us to inject a custom ViewModelFactory
     * @param factory The factory to use for creating the ViewModel.
     */
    public RegistrationHistoryFragment(GenericViewModelFactory factory) {
        this.viewModelFactory = factory;
    }

    /**
     * Inflates the fragment's layout.
     *
     * @param inflater The LayoutInflater object to inflate views.
     * @param container The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_registration_history, container, false);
    }

    /**
     * Called after the view has been created. This is where the fragment's UI is initialized.
     * This method sets up the ViewModel, binds UI components, and configures observers and
     * click listeners.
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);

        // --- ViewModel Initialization ---
        // If a factory was not injected (production), create the default one.
        if (viewModelFactory == null) {
            IRegistrationHistoryRepository historyRepository = new RegistrationHistoryRepositoryImpl();
            GenericViewModelFactory factory = new GenericViewModelFactory();
            factory.put(RegistrationHistoryViewModel.class, () -> new RegistrationHistoryViewModel(historyRepository));
            viewModelFactory = factory;
        }

        // Get the ViewModel instance using the determined factory.
        viewModel = new ViewModelProvider(this, viewModelFactory).get(RegistrationHistoryViewModel.class);

        setupObservers();
    }

    /**
     * Finds and binds all the UI components from the layout file.
     * @param view The root view of the fragment.
     */
    private void bindViews(@NonNull View view) {
        drawResults = view.findViewById(R.id.draw_results_title);
        containerDrawResults = view.findViewById(R.id.container_draw_results);
        noHistoryText = view.findViewById(R.id.no_history_text);
    }

    /**
     * Sets up observers on the ViewModel's LiveData.
     * This is the core of the reactive UI.
     */
    private void setupObservers(){
        /**
         * Callback invoked whenever the registration history list changes. Delegates
         * to {@link #showRegistrationHistory(List)} to render the updated list in the UI.
         * @param historyList the latest list of {@link RegistrationHistoryItem} objects
         *                    representing the user's registration history
         */
        viewModel.getUserHistory().observe(getViewLifecycleOwner(), historyList ->{
            showRegistrationHistory(historyList);
        });

        /**
         * Callback invoked when the ViewModel emits a user-facing message. Displays
         * the message as a Toast if it is not null or empty, providing feedback about
         * loading states, errors, or other events.
         * @param message the text message to show to the user, or null/empty if no
         *                message should be displayed
         */
        viewModel.getUserMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Renders the user's registration history inside the container layout. Clears
     * any existing rows, maps each valid {@link RegistrationHistoryItem} to a user
     * friendly status label, inflates a row view for each, and sets the appropriate
     * text and color styling.
     * <p>If the list is null or empty, or if no items meet the display criteria,
     * the "no history" message is shown instead.</p>
     * @param historyList the list of registration history records for the current user
     */
    private void showRegistrationHistory(List<RegistrationHistoryItem> historyList){
        containerDrawResults.removeAllViews(); // reset

        // user has no registration history
        if(historyList == null || historyList.isEmpty()){
            noHistoryText.setVisibility(View.VISIBLE);
            return;
        }

        boolean displayedAtLeastOne = false; // flag for setting up rows and columns of events

        // displaying each item in the list in a new row
        for(RegistrationHistoryItem item: historyList){
            if(item == null || item.getStatus() == null){
                continue;
            }

            String databaseStatus = item.getStatus();

            // assigning user facing statusText
            String statusText;
            if ("invited".equalsIgnoreCase(databaseStatus)){
                statusText = "Selected";
            } else if("waiting".equalsIgnoreCase(databaseStatus)){
                statusText = "Not Selected";
            } else if("cancelled".equalsIgnoreCase(databaseStatus)){
                statusText = "Cancelled";
            } else if("declined".equalsIgnoreCase(databaseStatus)){
                statusText = "Declined";
            } else if("accepted".equalsIgnoreCase(databaseStatus)){
                statusText = "Accepted";
            } else {
                continue; // skip all other statuses
            }

            displayedAtLeastOne = true;

            // create row view
            View row = getLayoutInflater().inflate(R.layout.item_registration_history, containerDrawResults, false);

            // setting event name and user status
            TextView eventNameText = row.findViewById(R.id.history_event_name);
            TextView statusTextView = row.findViewById(R.id.history_event_status);

            String eventName;
            if(item.getEventName() != null){
                eventName = item.getEventName();
            } else {
                eventName = "Unnamed event";
            }

            eventNameText.setText(eventName);
            statusTextView.setText(statusText);

            // setting status text color
            if (statusText.equals("Selected")) {
                statusTextView.setTextColor(getResources().getColor(R.color.red));
            } else if (statusText.equals("Not Selected")) {
                statusTextView.setTextColor(getResources().getColor(R.color.primary_green));
            } else {
                statusTextView.setTextColor(getResources().getColor(R.color.grey));
            }

            containerDrawResults.addView(row);
        }

        // showing or hiding text based on if user has any waitlisted events
        if (!displayedAtLeastOne) {
            noHistoryText.setVisibility(View.VISIBLE);
        } else {
            noHistoryText.setVisibility(View.GONE);
        }
    }

}