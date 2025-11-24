package com.example.lotteryevent.ui.organizer;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.lotteryevent.R;
import com.example.lotteryevent.repository.IOrganizerEventRepository;
import com.example.lotteryevent.repository.OrganizerEventRepositoryImpl;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.example.lotteryevent.viewmodels.OrganizerEventViewModel;

import java.io.OutputStream;

/**
 * Displays info about a single event for the organizer to view.
 * <p>
 *     This fragment allows the organizer to view event information
 *     and run the participant draw. It follows the MVVM pattern, delegating
 *     all logic and data operations to the OrganizerEventViewModel.
 * </p>
 */
public class OrganizerEventFragment extends Fragment {

    private static final String TAG = "OrganizerEventPage";
    private OrganizerEventViewModel viewModel;
    private String eventId;
    private ViewModelProvider.Factory viewModelFactory;

    // UI Components
    private TextView eventNameLabel;
    private Button qrCodeRequest;
    private LinearLayout buttonContainer;
    private Button btnViewWaitingList, btnViewEntrantMap, btnAcceptedParticipants;
    private Button btnInvitedParticipants, btnCancelledParticipants, btnRunDraw, btnFinalize;

    public OrganizerEventFragment() { } // Required empty public constructor

    /**
     * Constructor used for injecting a ViewModelProvider.Factory, primarily for testing.
     * @param viewModelFactory The factory to use for creating the ViewModel.
     */
    public OrganizerEventFragment(ViewModelProvider.Factory viewModelFactory) {
        this.viewModelFactory = viewModelFactory;
    }

    /**
     * Called to do initial creation of a fragment.
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If no factory was injected (production path), create one.
        if (viewModelFactory == null) {
            GenericViewModelFactory factory = new GenericViewModelFactory();
            IOrganizerEventRepository repository = new OrganizerEventRepositoryImpl();
            factory.put(OrganizerEventViewModel.class, () -> new OrganizerEventViewModel(repository));
            viewModelFactory = factory;
        }

        // Use the factory (either from production or a test) to create the ViewModel.
        viewModel = new ViewModelProvider(this, viewModelFactory).get(OrganizerEventViewModel.class);

        // Retrieve the eventId from navigation arguments
        if (getArguments() != null) {
            eventId = OrganizerEventFragmentArgs.fromBundle(getArguments()).getEventId();
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_event_page, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupClickListeners();
        setupObservers();

        // Tell the ViewModel to load the data
        if (eventId != null && !eventId.isEmpty()) {
            viewModel.loadEvent(eventId);
        } else {
            Log.e(TAG, "Event ID is null or empty.");
            Toast.makeText(getContext(), "Error: Event ID not found.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sets up observers on the ViewModel's LiveData to reactively update the UI.
     */
    private void setupObservers() {
        // Observer for event details (e.g., event name)
        viewModel.getEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                eventNameLabel.setText(event.getName());
            }
        });

        // Observer for the overall UI state (determines which buttons are visible)
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            // Reset visibility before applying the new state
            buttonContainer.setVisibility(View.VISIBLE);
            btnRunDraw.setVisibility(View.VISIBLE);
            btnFinalize.setVisibility(View.VISIBLE);

            switch (state) {
                case UPCOMING:
                    buttonContainer.setVisibility(View.GONE);
                    break;
                case FINALIZED:
                    btnRunDraw.setVisibility(View.GONE);
                    btnFinalize.setVisibility(View.GONE);
                    break;
                case OPEN:
                    btnFinalize.setEnabled(true);
                    break;
                case ERROR:
                    Toast.makeText(getContext(), "Error loading event state.", Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        // Observer to enable/disable the "Run Draw" button based on capacity
        viewModel.isRunDrawButtonEnabled().observe(getViewLifecycleOwner(), isEnabled -> {
            btnRunDraw.setEnabled(isEnabled);
        });

        // Observer to show the QR code dialog when a bitmap is generated
        viewModel.getQrCodeBitmap().observe(getViewLifecycleOwner(), bitmap -> {
            if (bitmap != null) {
                showQrCodeDialog(bitmap);
                viewModel.onQrCodeShown(); // Inform ViewModel the QR code has been handled
            }
        });

        // Observer for any messages (e.g., errors) from the ViewModel
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Initializes the UI components by finding them in the view hierarchy.
     * @param view The root view of the fragment.
     */
    private void initializeViews(View view) {
        eventNameLabel = view.findViewById(R.id.event_name_label);
        qrCodeRequest = view.findViewById(R.id.request_qr_code);
        buttonContainer = view.findViewById(R.id.organizer_button_container);
        btnViewWaitingList = view.findViewById(R.id.btnViewWaitingList);
        btnViewEntrantMap = view.findViewById(R.id.btnViewEntrantMap);
        btnAcceptedParticipants = view.findViewById(R.id.btnAcceptedParticipants);
        btnInvitedParticipants = view.findViewById(R.id.btnInvitedParticipants);
        btnCancelledParticipants = view.findViewById(R.id.btnCancelledParticipants);
        btnRunDraw = view.findViewById(R.id.btnRunDraw);
        btnFinalize = view.findViewById(R.id.btnFinalize);
    }

    /**
     * Binds click listeners to the interactive UI elements on the page.
     */
    private void setupClickListeners() {
        qrCodeRequest.setOnClickListener(v -> viewModel.generateQrCode(eventId));

        btnRunDraw.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);
            Navigation.findNavController(v)
                    .navigate(R.id.action_organizerEventPageFragment_to_runDrawFragment, bundle);
        });

        View.OnClickListener entrantListNavListener = v -> {
            String status;
            int id = v.getId();

            if (id == R.id.btnViewWaitingList) {
                status = "waiting";
            } else if (id == R.id.btnAcceptedParticipants) {
                status = "accepted";
            } else if (id == R.id.btnInvitedParticipants) {
                Navigation.findNavController(v).navigate(
                        OrganizerEventFragmentDirections.actionOrganizerEventPageFragmentToManageSelectedFragment(eventId)
                );
                return;
            } else if (id == R.id.btnCancelledParticipants) {
                status = "cancelled";
            } else {
                return;
            }

            OrganizerEventFragmentDirections.ActionOrganizerEventPageFragmentToEntrantListFragment action =
                    OrganizerEventFragmentDirections.actionOrganizerEventPageFragmentToEntrantListFragment(status, this.eventId);

            Navigation.findNavController(v).navigate(action);
        };

        btnViewWaitingList.setOnClickListener(entrantListNavListener);
        btnAcceptedParticipants.setOnClickListener(entrantListNavListener);
        btnInvitedParticipants.setOnClickListener(entrantListNavListener);
        btnCancelledParticipants.setOnClickListener(entrantListNavListener);

        View.OnClickListener notImplementedListener = v -> {
            Button b = (Button) v;
            Toast.makeText(getContext(), b.getText().toString() + " not implemented yet.", Toast.LENGTH_SHORT).show();
        };
        btnViewEntrantMap.setOnClickListener(notImplementedListener);
        btnFinalize.setOnClickListener(v -> showFinalizeConfirmationDialog());
    }

    // --- UI Helper Methods ---

    /**
     * Shows a confirmation dialog ensuring the user wants to finalize the event.
     */
    private void showFinalizeConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Finalize Event")
                .setMessage("Are you sure you want to finalize this event? \n\n" +
                        "This will prevent further entrants from joining and invited entrants from accepting their invitations. " +
                        "This action cannot be undone.")
                .setPositiveButton("Finalize", (dialog, which) -> {
                    // Call the ViewModel to execute the logic
                    viewModel.finalizeEvent(eventId);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .create()
                .show();
    }

    /**
     * Displays a dialog containing the generated QR code bitmap.
     * @param qrCodeBitmap The QR code image to display.
     */
    private void showQrCodeDialog(Bitmap qrCodeBitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_qr_code, null);

        ImageView qrCodeImageView = dialogView.findViewById(R.id.image_view_qr_code);
        Button saveButton = dialogView.findViewById(R.id.button_dialog_save);
        Button shareButton = dialogView.findViewById(R.id.button_dialog_share);
        Button closeButton = dialogView.findViewById(R.id.button_dialog_close);

        qrCodeImageView.setImageBitmap(qrCodeBitmap);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            Uri uri = saveBitmapToPictures(qrCodeBitmap);
            if (uri != null) {
                Toast.makeText(getContext(), "QR Code saved to Photos!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to save QR Code.", Toast.LENGTH_SHORT).show();
            }
        });

        shareButton.setOnClickListener(v -> shareQrCode(qrCodeBitmap));
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * Triggers a system share intent for the QR code image.
     * @param qrCodeBitmap The QR code bitmap to be shared.
     */
    private void shareQrCode(Bitmap qrCodeBitmap) {
        Uri uri = saveBitmapToPictures(qrCodeBitmap);
        if (uri == null) {
            Toast.makeText(getContext(), "Failed to share QR Code.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
    }

    /**
     * Saves a bitmap image to the device's public "Pictures" directory using MediaStore.
     * @param bitmap The bitmap image to save.
     * @return The Uri of the saved image on success, or null on failure.
     */
    private Uri saveBitmapToPictures(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "event_qr_code_" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri uri = getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream outputStream = getContext().getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                return uri;
            } catch (Exception e) {
                Log.e(TAG, "Failed to save bitmap", e);
            }
        }
        return null;
    }


}