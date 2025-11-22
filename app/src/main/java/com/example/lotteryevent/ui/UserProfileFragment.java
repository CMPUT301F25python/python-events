package com.example.lotteryevent.ui;

import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.lotteryevent.R;
import com.example.lotteryevent.repository.IUserRepository;
import com.example.lotteryevent.repository.UserRepositoryImpl;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.example.lotteryevent.viewmodels.UserProfileViewModel;

/**
 * A {@link Fragment} subclass that allows users to view and update their profile information.
 * This Fragment follows MVVM principles, delegating all data and business logic
 * to the {@link UserProfileViewModel}. Its sole responsibility is to display data and
 * forward user events to the ViewModel.
 */
public class UserProfileFragment extends Fragment {

    // --- UI Components ---
    private EditText nameField;
    private EditText emailField;
    private EditText phoneField;
    private Button updateInfoButton;
    private Button deleteProfileButton;

    // --- ViewModel ---
    private UserProfileViewModel viewModel;
    private ViewModelProvider.Factory viewModelFactory;

    /**
     * Required empty public constructor.
     */
    public UserProfileFragment() {}

    /**
     * Constructor for testing. Alows us to inject a custom ViewModelFactory
     * @param factory The factory to use for creating the ViewModel.
     */
    public UserProfileFragment(GenericViewModelFactory factory) {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_profile, container, false);
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

        // --- ViewModel Initialization ---
        // If a factory was not injected (production), create the default one.
        if (viewModelFactory == null) {
            IUserRepository userRepository = new UserRepositoryImpl();
            GenericViewModelFactory factory = new GenericViewModelFactory();
            factory.put(UserProfileViewModel.class, () -> new UserProfileViewModel(userRepository));
            viewModelFactory = factory;
        }

        // Get the ViewModel instance using the determined factory.
        viewModel = new ViewModelProvider(this, viewModelFactory).get(UserProfileViewModel.class);

        // --- The rest of the method is the same ---
        bindViews(view);
        setupObservers();
        setupClickListeners();
    }

    /**
     * Finds and binds all the UI components from the layout file.
     * @param view The root view of the fragment.
     */
    private void bindViews(@NonNull View view) {
        nameField = view.findViewById(R.id.name_field);
        emailField = view.findViewById(R.id.email_field);
        phoneField = view.findViewById(R.id.phone_field);
        updateInfoButton = view.findViewById(R.id.update_button);
        deleteProfileButton = view.findViewById(R.id.delete_button);

        // Add text formatting watcher
        phoneField.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
    }

    /**
     * Sets up observers on the ViewModel's LiveData.
     * This is the core of the reactive UI.
     */
    private void setupObservers() {
        // Observe the current user's profile data
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                // When the User object is loaded or updated, populate the fields
                nameField.setText(user.getName());
                emailField.setText(user.getEmail());
                phoneField.setText(user.getPhone());
            } else {
                // If the user becomes null (e.g., logged out elsewhere), handle it gracefully
                Toast.makeText(getContext(), "User not found. Returning to previous screen.", Toast.LENGTH_LONG).show();
                Navigation.findNavController(requireView()).popBackStack();
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                updateInfoButton.setEnabled(false);
                deleteProfileButton.setEnabled(false);
            } else {
                updateInfoButton.setEnabled(true);
                deleteProfileButton.setEnabled(true);
            }
        });

        // Observe messages (for errors or success confirmations)
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

                // If a successful deletion message is received, navigate back
                if (message.contains("successfully deleted")) {
                    Navigation.findNavController(requireView()).popBackStack();
                }
            }
        });
    }

    /**
     * Sets up the OnClickListener for buttons to delegate actions to the ViewModel.
     */
    private void setupClickListeners() {
        updateInfoButton.setOnClickListener(v -> handleUpdateProfile());
        deleteProfileButton.setOnClickListener(v -> confirmProfileDeletion());
    }

    /**
     * Gathers user input and asks the ViewModel to perform the update.
     */
    private void handleUpdateProfile() {
        String name = nameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();

        // The ViewModel handles the validation and the repository call
        String validationError = viewModel.updateUserProfile(name, email, phone);

        if (validationError != null) {
            // If the ViewModel finds a validation error, show it
            Toast.makeText(getContext(), validationError, Toast.LENGTH_SHORT).show();
        }
        // If successful, the `getError` observer will show the success Toast.
    }

    /**
     * Shows a confirmation dialog before asking the ViewModel to delete the user.
     */
    private void confirmProfileDeletion() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Profile")
                .setMessage("This will clear your profile and delete all associated data. This action cannot be undone. Are you sure?")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    // Tell the ViewModel to start the deletion process
                    viewModel.deleteUserProfile();
                })
                .setNegativeButton("No", null)
                .show();
    }
}