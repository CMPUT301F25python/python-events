package com.example.lotteryevent.ui.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.lotteryevent.NotificationCustomManager;
import com.example.lotteryevent.R;
import com.example.lotteryevent.repository.AdminUserProfileRepositoryImpl;
import com.example.lotteryevent.repository.EventDetailsRepositoryImpl;
import com.example.lotteryevent.repository.IAdminUserProfileRepository;
import com.example.lotteryevent.repository.IEventDetailsRepository;
import com.example.lotteryevent.viewmodels.EventDetailsViewModel;
import com.example.lotteryevent.viewmodels.EventDetailsViewModelFactory;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * A {@link Fragment} that displays the administrative view of a specific user profile.
 * <p>
 * This fragment allows an administrator to view user details and perform critical actions
 * such as permanently deleting the user from the system. It utilizes {@link EventDetailsViewModel}
 * to handle the business logic for the cascading deletion of the user's profile, organized events,
 * and notifications.
 * </p>
 *
 * @see com.example.lotteryevent.viewmodels.EventDetailsViewModel
 */
public class AdminUserProfileFragment extends Fragment {

    private TextView tvEmail, tvPhone;
    private Button sendNotificationButton;
    private Button deleteButton;
    private String userId;
    private EventDetailsViewModel viewModel;
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private ViewModelProvider.Factory viewModelFactory;

    /**
     * Required empty public constructor.
     */
    public AdminUserProfileFragment() {
    }

    /**
     * Constructor for testing. Allows us to inject a custom ViewModelFactory.
     *
     * @param factory The factory to use for creating the ViewModel.
     */
    public AdminUserProfileFragment(ViewModelProvider.Factory factory) {
        this.viewModelFactory = factory;
    }

    /**
     * Inflates the layout for this fragment.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_user_profile, container, false);
    }

    /**
     * Initializes the ViewModel, retrieves the User ID argument, and binds the UI components.
     * <p>
     * This method establishes the following behaviors:
     * <ul>
     *     <li><b>ViewModel Setup:</b> Initializes {@link EventDetailsViewModel} to handle data operations.</li>
     *     <li><b>Data Observation:</b> Observes the user profile LiveData to populate the UI fields (Email, Phone)
     *     and dynamically updates the Activity's Toolbar title with the user's name.</li>
     *     <li><b>Delete Logic:</b> Calls {@link #setupDeleteButton()} to conditionally show/hide the delete option
     *     based on whether the admin is viewing their own profile.</li>
     * </ul>
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize ViewModel
        if (viewModelFactory == null) {
            IEventDetailsRepository repository = new EventDetailsRepositoryImpl();
            IAdminUserProfileRepository userProfileRepository = new AdminUserProfileRepositoryImpl();
            GenericViewModelFactory factory = new GenericViewModelFactory();
            factory.put(EventDetailsViewModel.class, () -> new EventDetailsViewModel(repository, userProfileRepository));
            viewModelFactory = factory;
        }
        viewModel = new ViewModelProvider(this, viewModelFactory).get(EventDetailsViewModel.class);

        // 2. Get the User ID
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
        }

        // 3. Bind Views
        deleteButton = view.findViewById(R.id.btn_delete_user);
        sendNotificationButton = view.findViewById(R.id.btn_notify);
        sendNotificationButton.setOnClickListener(v -> showNotificationDialog());
        tvEmail = view.findViewById(R.id.tv_user_email);
        tvPhone = view.findViewById(R.id.tv_user_phone);

        // 4. Handle Delete Button Logic
        setupDeleteButton();

        // 5. Observe User Profile
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                String name = user.getName() != null ? user.getName() : "Unknown User";

                // Set header title
                if (getActivity() instanceof AppCompatActivity) {
                    AppCompatActivity activity = (AppCompatActivity) getActivity();
                    if (activity.getSupportActionBar() != null) {
                        activity.getSupportActionBar().setTitle(name);
                    }
                }



                tvEmail.setText(user.getEmail() != null ? user.getEmail() : "Not provided");
                tvPhone.setText(user.getPhone() != null ? user.getPhone() : "Not provided");
            }
        });

        if (userId != null) {
            viewModel.loadUserProfile(userId);
        }

        // 6. Observe Delete Status
        viewModel.getIsOrganizerDeleted().observe(getViewLifecycleOwner(), isDeleted -> {
            if (Boolean.TRUE.equals(isDeleted)) {
                Toast.makeText(getContext(), "User deleted successfully", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }
        });
    }

    /**
     * Configures the visibility and behavior of the "Delete User" button.
     * <p>
     * This method serves as a safety check to prevent an administrator from accidentally
     * deleting their own account.
     * <ul>
     *     <li>If the profile being viewed belongs to the currently logged-in user, the button is hidden (GONE).</li>
     *     <li>If the profile belongs to a different user, the button is visible and a click listener is attached
     *         to trigger the deletion confirmation dialog.</li>
     * </ul>
     */
    private void setupDeleteButton() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String currentUserId = (currentUser != null) ? currentUser.getUid() : "";

        if (userId != null && userId.equals(currentUserId)) {
            deleteButton.setVisibility(View.GONE);
        } else {
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(v -> showDeleteConfirmation());
        }
    }

    /**
     * Displays an {@link AlertDialog} to confirm the user's intent to delete the profile.
     * <p>
     * This provides a safety check to prevent accidental deletion of data.
     * </p>
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete User?")
                .setMessage("Are you sure you want to delete this user? This will remove their profile, events, and notifications.")
                .setPositiveButton("Delete", (dialog, which) -> performDelete())
                .setNegativeButton("Cancel", null)
                .show();
    }


    /**
     * Triggers the deletion process via the ViewModel using the current {@code userId}.
     * <p>
     * This method is called only after the administrator confirms the action in the dialog.
     * </p>
     */
    private void performDelete() {
        if (userId != null && !userId.isEmpty()) {
            viewModel.deleteOrganizer(userId);
        } else {
            Toast.makeText(getContext(), "Error: Invalid User ID", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Displays a dialog allowing the organizer to input a message that will be
     * sent as a notification to all entrants currently shown in the list.
     * Provides input validation and triggers the ViewModel's bulk notification
     * method when confirmed.
     */
    private void showNotificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Notification Message");
        final EditText input = new EditText(getContext());
        input.setHint("Enter message...");
        builder.setView(input);
        builder.setPositiveButton("Notify", (dialog, which) -> {
            /**
             * Callback triggered when the "Notify All" button in the notification dialog is
             * pressed. Retrieves the user's input from the EditText and triggers the ViewModel's
             * bulk notification method.
             * @param dialog the dialog that triggered the callback
             */
            String organizerMessage = input.getText().toString().trim();
            NotificationCustomManager manager = new NotificationCustomManager(requireContext());
            viewModel.notifyEntrant(userId, organizerMessage, manager);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}