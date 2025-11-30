package com.example.lotteryevent.ui.admin;

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

import com.example.lotteryevent.R;
import com.example.lotteryevent.adapters.AdminProfilesAdapter;
import com.example.lotteryevent.repository.AdminProfilesRepositoryImpl;
import com.example.lotteryevent.viewmodels.AdminProfilesViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

/**
 * A Fragment that handles the administration of user profiles.
 * <p>
 * This screen displays a list of registered users. It enables administrators to find specific
 * users and remove profiles that violate community guidelines or terms of service.
 */
public class AdminProfilesFragment extends Fragment {

    private AdminProfilesViewModel viewModel;
    private RecyclerView recycler;
    private AdminProfilesAdapter adapter;

    /**
     * Default constructor for production use by the Android Framework.
     */
    public AdminProfilesFragment() { }

    /**
     * Called by the system to have the fragment instantiate its user interface view.
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrant_list, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has returned,
     * but before any saved state has been restored in to the view.
     * Sets up view and its components.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        GenericViewModelFactory factory = new GenericViewModelFactory();
        factory.put(AdminProfilesViewModel.class,
                () -> new AdminProfilesViewModel(new AdminProfilesRepositoryImpl()));

        viewModel = new ViewModelProvider(this, factory).get(AdminProfilesViewModel.class);

        TextView title = view.findViewById(R.id.entrant_list_title);
        title.setText("All Users");

        // Hide notification button
        Button notifyBtn = view.findViewById(R.id.send_notification_button);
        notifyBtn.setVisibility(view.GONE);

        recycler = view.findViewById(R.id.entrants_recycler_view);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Add navigation to user profile details
        /**
         * Handles clicks on a user item in the list and navigates to that user's profile
         * @param user selected user
         */
        adapter = new AdminProfilesAdapter(user -> {
            // 2. Create the Bundle with the userId
            Bundle bundle = new Bundle();
            bundle.putString("userId", user.getId());

            // 3. Navigate to the new Profile page
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_adminProfiles_to_userProfile, bundle);
        });

        recycler.setAdapter(adapter);

        setupObservers();
        viewModel.fetchProfiles();
    }

    /**
     * Sets up LiveData observers for AdminProfileViewModel
     * <p>
     *     Listens for updates to list of user profiles and UI messages
     *     displays this latest list of user until an error is encountered, where it toasts the admin
     * </p>
     */
    private void setupObservers() {
        /**
         * Observes profiles list, updates adapter on change
         * @param list contains profiles
         */
        viewModel.getProfiles().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                adapter.setProfiles(list);
            }
        });

        /**
         * Observes message, if updates makes toast
         * @param msg message to show
         */
        viewModel.getMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
