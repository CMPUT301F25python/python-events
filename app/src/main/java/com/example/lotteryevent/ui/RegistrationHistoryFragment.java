package com.example.lotteryevent.ui;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lotteryevent.R;
import com.example.lotteryevent.data.RegistrationHistoryItem;
import com.example.lotteryevent.repository.IRegistrationHistoryRepository;
import com.example.lotteryevent.repository.IUserRepository;
import com.example.lotteryevent.repository.RegistrationHistoryRepositoryImpl;
import com.example.lotteryevent.repository.UserRepositoryImpl;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.example.lotteryevent.viewmodels.RegistrationHistoryViewModel;
import com.example.lotteryevent.viewmodels.UserProfileViewModel;

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

        drawResults = view.findViewById(R.id.draw_results_title);
        containerDrawResults = view.findViewById(R.id.container_draw_results);

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

        // user has no registration history
        if(historyList == null || historyList.isEmpty()){
            containerDrawResults.addView(noHistory);
            return;
        }

        // displaying each item in the list in a new row
        for(RegistrationHistoryItem item: historyList){
            if(item == null){
                return;
            }

            String databaseStatus = item.getStatus();
            if(databaseStatus == null){
                continue;
            }

            // assigning user facing statusText (showing only invited or waiting)
            String statusText;
            if ("invited".equalsIgnoreCase(databaseStatus)){
                statusText = "Selected";
            } else if("waiting".equalsIgnoreCase(databaseStatus)){
                statusText = "Not Selected";
            } else {
                continue; // skip all other statuses
            }

            // set up the row
            TextView row = new TextView(getContext());
            row.setPadding(0, 8,0, 8);
            row.setTextSize(16);

            String eventName;
            if(item.getEventName() != null){
                eventName = item.getEventName();
            } else {
                eventName = "Unnamed event";
            }
            // show the data
            String displayText = eventName + " - " + statusText;
            row.setText(displayText);

            containerDrawResults.addView(row);
        }

        // if all results are filtered (i.e. no waiting or invited status)
        if(containerDrawResults.getChildCount() == 0){
            containerDrawResults.addView(noHistory);
        }
    }

}