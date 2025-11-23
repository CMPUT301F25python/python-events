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

public class RegistrationHistoryFragment extends Fragment {

    private RegistrationHistoryViewModel viewModel;
    private ViewModelProvider.Factory viewModelFactory;
    private TextView drawResults;
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
    }

    /**
     * Sets up observers on the ViewModel's LiveData.
     * This is the core of the reactive UI.
     */
    private void setupObservers(){
        // Observe the current user's registration history
        viewModel.getUserHistory().observe(getViewLifecycleOwner(), historyList ->{
            showRegistrationHistory(historyList);
        });

        // Observe messages (for errors or success confirmations)
        viewModel.getUserMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * This class shows the list of registration history entries for a user in the LinearLayout
     * @param historyList the list of events a user has waitlisted
     */
    private void showRegistrationHistory(List<RegistrationHistoryItem> historyList){
        containerDrawResults.removeAllViews(); // reset

        TextView noHistory = new TextView(getContext());
        noHistory.setText(R.string.no_draw_results);
        noHistory.setTextSize(16);
        noHistory.setTextColor(getResources().getColor(R.color.primary_green));
        noHistory.setPadding(0, 16, 0, 0);

        // user has no registration history
        if(historyList == null || historyList.isEmpty()){
            containerDrawResults.addView(noHistory);
            return;
        }

        boolean displayedAtLeastOne = false; // flag for setting up rows and columns of events

        // displaying each item in the list in a new row
        for(RegistrationHistoryItem item: historyList){
            if(item == null || item.getStatus() == null){
                continue;
            }

            String databaseStatus = item.getStatus();

            // assigning user facing statusText (showing only invited or waiting)
            String statusText;
            if ("invited".equalsIgnoreCase(databaseStatus)){
                statusText = "Selected";
            } else if("waiting".equalsIgnoreCase(databaseStatus)){
                statusText = "Not Selected";
            } else {
                continue; // skip all other statuses
            }

            displayedAtLeastOne = true;

            View row = getLayoutInflater().inflate(R.layout.item_registration_history, containerDrawResults, false);

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
            } else { // Not Selected
                statusTextView.setTextColor(getResources().getColor(R.color.primary_green));
            }

            containerDrawResults.addView(row);
        }

        if (!displayedAtLeastOne) {
            containerDrawResults.addView(noHistory);
        }
    }

}