package com.example.lotteryevent.organizer;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.os.Environment;
import com.bumptech.glide.Glide;

import com.example.lotteryevent.R;
import com.example.lotteryevent.data.Event;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.Query;
import com.google.firebase.Timestamp;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;

/**
 * Displays info about a single event for the organizer to view.
 * <p>
 *     This fragment allows the organizer to view event information
 *     and run the participant draw
 * </p>
 */
public class OrganizerEventPageFragment extends Fragment {

    private static final String TAG = "OrganizerEventPage";

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String eventId;
    private Event event;
    private Button qrCodeRequest;
    private LinearLayout buttonContainer;
    private Button btnViewWaitingList, btnViewEntrantMap, btnAcceptedParticipants;
    private Button btnInvitedParticipants, btnCancelledParticipants, btnRunDraw, btnFinalize;

    private ImageView posterImage;
    private Button uploadPosterButton;
    private ActivityResultLauncher<String> pickImage;
    private Uri pendingPosterUri; // holds the image the user picked but hasn’t uploaded yet


    /**
     * Required empty public constructor for fragment instantiation.
     */
    public OrganizerEventPageFragment() { }

    /**
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Retrieve the eventId from navigation arguments
        if (getArguments() != null) {
            eventId = OrganizerEventPageFragmentArgs.fromBundle(getArguments()).getEventId();
        }

        pickImage = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;

            // Store it to upload later
            pendingPosterUri = uri;

            // Preview it immediately
            if (posterImage != null) {
                Glide.with(this)
                        .load(uri)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(posterImage);
            }

            // Enable the upload button now that we have a selection
            if (uploadPosterButton != null) uploadPosterButton.setEnabled(true);
        });

    }

    /**
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_organizer_event_page, container, false);
    }

    /**
     *
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);

        setupClickListeners();

        // Fetch and display event data if eventId is available
        if (eventId != null && !eventId.isEmpty()) {
            fetchEventDetails();
        } else {
            Log.e(TAG, "Event ID is null or empty.");
            Toast.makeText(getContext(), "Error: Event ID not found.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Initializes the UI components by finding them in the view hierarchy.
     * This method should be called in {@code onViewCreated} to ensure the view
     * is fully inflated before attempting to find any child views.
     *
     * @param view The root view of the fragment's layout from which to find the UI components.
     */
    private void initializeViews(View view) {
        qrCodeRequest = view.findViewById(R.id.request_qr_code);
        buttonContainer = view.findViewById(R.id.organizer_button_container);
        btnViewWaitingList = view.findViewById(R.id.btnViewWaitingList);
        btnViewEntrantMap = view.findViewById(R.id.btnViewEntrantMap);
        btnAcceptedParticipants = view.findViewById(R.id.btnAcceptedParticipants);
        btnInvitedParticipants = view.findViewById(R.id.btnInvitedParticipants);
        btnCancelledParticipants = view.findViewById(R.id.btnCancelledParticipants);
        btnRunDraw = view.findViewById(R.id.btnRunDraw);
        btnFinalize = view.findViewById(R.id.btnFinalize);

        posterImage = view.findViewById(R.id.poster_image);
        uploadPosterButton = view.findViewById(R.id.upload_poster_button);
    }

    /**
     * Binds click listeners to the interactive UI elements on the page.
     */
    private void setupClickListeners() {
        qrCodeRequest.setOnClickListener(this::generateQrCode);

        // Tap the image to choose a photo
        if (posterImage != null) {
            posterImage.setOnClickListener(v -> pickImage.launch("image/*"));
        }

        // Upload button saves the chosen image
        if (uploadPosterButton != null) {
            uploadPosterButton.setEnabled(false); // disabled until a photo is chosen
            uploadPosterButton.setOnClickListener(v -> {
                if (pendingPosterUri == null) {
                    Toast.makeText(requireContext(), "Tap the image box to choose a photo first.", Toast.LENGTH_SHORT).show();
                    return;
                }
                uploadPoster(pendingPosterUri);
            });
        }

        View.OnClickListener entrantListNavListener = v -> {
            String status;
            int id = v.getId();

            if (id == R.id.btnViewWaitingList) {
                status = "waiting";
            } else if (id == R.id.btnAcceptedParticipants) {
                status = "accepted";
            } else if (id == R.id.btnInvitedParticipants) {
                // Navigate to ManageSelected Fragment
                Navigation.findNavController(v).navigate(
                        OrganizerEventPageFragmentDirections.actionOrganizerEventPageFragmentToManageSelectedFragment(eventId)
                );
                return;
            } else if (id == R.id.btnCancelledParticipants) {
                status = "cancelled";
            } else {
                return;
            }

            OrganizerEventPageFragmentDirections.ActionOrganizerEventPageFragmentToEntrantListFragment action =
                    OrganizerEventPageFragmentDirections.actionOrganizerEventPageFragmentToEntrantListFragment(status, this.eventId);

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
        btnFinalize.setOnClickListener(notImplementedListener);
    }


    /**
     * Fetches the details of the event from the Firestore database using the `eventId`.
     * On a successful fetch, it populates the local {@link Event} object and then calls
     * {@link #updateUi()} to refresh the user interface with the event's data.
     * It also triggers a check to see if the "Run Draw" button should be disabled based on
     * the current number of entrants versus the event's capacity by calling
     * {@link #checkCapacityAndDisableDrawButton()}.
     * If the fetch fails or the event document does not exist, an error message is logged
     * and a Toast is shown to the user.
     */
    private void fetchEventDetails() {
        db.collection("events").document(eventId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String name = document.getString("name");
                            String status = document.getString("status");

                            String posterUrl = document.getString("posterUrl");
                            if (posterImage != null && posterUrl != null && !posterUrl.trim().isEmpty()) {
                                Glide.with(this)
                                        .load(posterUrl)
                                        .placeholder(R.drawable.ic_image_placeholder)
                                        .error(R.drawable.ic_image_placeholder)
                                        .into(posterImage);
                            }

                            updateUi();

                            this.event = document.toObject(Event.class);
                            if (this.event != null) {
                                updateUi();
                                checkCapacityAndDisableDrawButton();
                            } else {
                                Log.w(TAG, "Failed to parse Event object.");
                            }
                        } else {
                            Log.d(TAG, "No such document");
                            Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "get failed with ", task.getException());
                        Toast.makeText(getContext(), "Failed to load event details.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadPoster(@NonNull Uri uri) {
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Missing event id", Toast.LENGTH_SHORT).show();
            return;
        }
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Not signed in yet – try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(), "Uploading poster…", Toast.LENGTH_SHORT).show();

        String fileName = java.util.UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference()
                .child("posters")
                .child(eventId)
                .child(fileName);

        Log.d(TAG, "putFile to: " + ref.getBucket() + " / " + ref.getPath() + "  uri=" + uri + " (" + uri.getScheme() + ")");

        ref.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    StorageReference uploadedRef = taskSnapshot.getStorage(); // same as 'ref'
                    uploadedRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                Log.d(TAG, "downloadUrl=" + downloadUri);

                                // Use update(...) or set(..., merge()) – both are fine
                                db.collection("events").document(eventId)
                                        .update(
                                                "posterUrl", downloadUri.toString(),
                                                "posterPath", uploadedRef.getPath()
                                        )
                                        .addOnSuccessListener(v -> {
                                            Toast.makeText(requireContext(), "Poster updated", Toast.LENGTH_SHORT).show();
                                            pendingPosterUri = null;
                                            if (uploadPosterButton != null) uploadPosterButton.setEnabled(false);
                                            if (posterImage != null) {
                                                Glide.with(this)
                                                        .load(downloadUri)
                                                        .placeholder(R.drawable.ic_image_placeholder)
                                                        .error(R.drawable.ic_image_placeholder)
                                                        .into(posterImage);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to save URL to Firestore", e);
                                            Toast.makeText(requireContext(), "Saved to Storage, failed to save URL", Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "getDownloadUrl failed", e);
                                Toast.makeText(requireContext(), "Upload ok, but couldn’t get URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "putFile failed", e);
                    Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates the UI based on the event's data and the current time.
     * This method dynamically determines the event's effective state (Upcoming, Open, Closed, Finalized)
     * and adjusts the UI accordingly.
     */
    private void updateUi() {

        if (this.event == null) {
            return;
        }

        // Set the event title
        if (getView() != null) {
            TextView eventNameLabel = getView().findViewById(R.id.event_name_label);
            eventNameLabel.setText(event.getName());
        }

        // Get all necessary data points
        Timestamp now = Timestamp.now();
        Timestamp regStart = event.getRegistrationStartDateTime();
        String dbStatus = event.getStatus();

        // Always reset button visibility to handle state transitions correctly
        btnFinalize.setVisibility(View.VISIBLE);
        btnRunDraw.setVisibility(View.VISIBLE);
        btnRunDraw.setEnabled(false);
        btnFinalize.setEnabled(false);

        // State 1: The event is permanently finalized. This overrides all other logic.
        if ("finalized".equals(dbStatus)) {
            buttonContainer.setVisibility(View.VISIBLE);
            btnFinalize.setVisibility(View.GONE);
            btnRunDraw.setVisibility(View.GONE);
        }
        // State 2: The event registration has not started yet (Upcoming).
        else if (regStart != null && now.compareTo(regStart) < 0) {
            buttonContainer.setVisibility(View.GONE); // Hide all buttons
        }
        // State 3: Default state - The event has not been finalized.
        else {
            buttonContainer.setVisibility(View.VISIBLE);
            btnRunDraw.setEnabled(true);
            btnFinalize.setEnabled(true);
        }
        btnAcceptedParticipants.setEnabled(true);
        btnInvitedParticipants.setEnabled(true);
        btnCancelledParticipants.setEnabled(true);
        btnViewWaitingList.setEnabled(true);
        btnViewEntrantMap.setEnabled(true);
    }

    /**
     * Checks if the number of 'invited' and 'accepted' entrants has reached the event's capacity.
     * If it has, this method disables the "Run Draw" button to prevent over-inviting.
     * This uses an efficient Firestore aggregate query to get the count.
     */
    private void checkCapacityAndDisableDrawButton() {
        // Guard clauses: Can't perform the check without an event, eventId, or capacity.
        if (event == null || eventId == null || event.getCapacity() == null || event.getCapacity() == 0) {
            return;
        }

        // 1. Create a query for all entrants who are either "invited" or "accepted".
        Query query = db.collection("events").document(eventId).collection("entrants")
                .whereIn("status", Arrays.asList("invited", "accepted"));

        // 2. Create an aggregate query to get the COUNT of the documents from the query above.
        AggregateQuery countQuery = query.count();

        // 3. Execute the count query.
        countQuery.get(AggregateSource.SERVER).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                AggregateQuerySnapshot snapshot = task.getResult();
                if (snapshot != null) {
                    long currentCount = snapshot.getCount();
                    Integer capacity = event.getCapacity();

                    Log.d(TAG, "Capacity check: " + currentCount + " invited/accepted entrants. Capacity is " + capacity);

                    // 4. If the count of invited/accepted people is >= capacity, disable the button.
                    if (currentCount >= capacity) {
                        Log.d(TAG, "Event is at capacity. Disabling Run Draw button.");
                        btnRunDraw.setEnabled(false);
                    } else {
                        Log.d(TAG, "Event is not at capacity. Enabling Run Draw button.");
                        btnRunDraw.setEnabled(true);
                    }
                }
            } else {
                Log.w(TAG, "Failed to execute entrant count query.", task.getException());
            }
        });
    }

    /**
     * Displays a dialog containing the generated QR code bitmap.
     * The dialog provides options to save, share, or close the view.
     * @param qrCodeBitmap The QR code image to display.
     */
    private void showQrCodeDialog(Bitmap qrCodeBitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();View dialogView = inflater.inflate(R.layout.dialog_qr_code, null);

        ImageView qrCodeImageView = dialogView.findViewById(R.id.image_view_qr_code);
        Button saveButton = dialogView.findViewById(R.id.button_dialog_save);
        Button shareButton = dialogView.findViewById(R.id.button_dialog_share);
        Button closeButton = dialogView.findViewById(R.id.button_dialog_close);

        qrCodeImageView.setImageBitmap(qrCodeBitmap);

        // Set the view for the builder, but do not add buttons to the builder itself
        builder.setView(dialogView);

        // Create the dialog so we can interact with it
        AlertDialog dialog = builder.create();

        // Set click listeners on custom buttons
        saveButton.setOnClickListener(v -> {
            Uri uri = saveBitmapToPictures(qrCodeBitmap);
            if (uri != null) {
                Toast.makeText(getContext(), "QR Code saved to Photos!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to save QR Code.", Toast.LENGTH_SHORT).show();
            }
        });

        shareButton.setOnClickListener(v -> {
            shareQrCode(qrCodeBitmap);
        });

        closeButton.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Initiates the QR code generation process.
     * It uses the current eventId as the data for the QR code and displays the result in a dialog.
     * @param view The view that triggered this action.
     */
    private void generateQrCode(View view) {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Event ID not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        String qrCodeText = eventId;
        // generate QR code
        Bitmap qrcodeBitMap = generateQRCode(qrCodeText, 600, 600);

        if (qrcodeBitMap == null) {
            Toast.makeText(getContext(), "Error: Failed to generate QR code.", Toast.LENGTH_SHORT).show();
            return;
        }
        showQrCodeDialog(qrcodeBitMap);
    }

    /**
     * Generates a QR code bitmap from a given text.
     *
     * @param text The text to encode in the QR code.
     * @param width The desired width of the QR code bitmap.
     * @param height The desired height of the QR code bitmap.
     * @return The generated QR code as a Bitmap, or null if an error occurred.
     */
    private static Bitmap generateQRCode(String text, int width, int height) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR Code", e); // Also a good idea to log the error
            return null;
        }
    }

    /**
     * Saves a bitmap to storage and then triggers a system share intent.
     * This allows the user to share the QR code image with other apps.
     * @param qrCodeBitmap The QR code bitmap to be shared.
     */
    private void shareQrCode(Bitmap qrCodeBitmap) {
        // Save the bitmap using the helper.
        Uri uri = saveBitmapToPictures(qrCodeBitmap);
        if (uri == null) {
            Toast.makeText(getContext(), "Failed to share QR Code.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and start the share intent.
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
    }

    /**
     * Saves a bitmap image to the device's public "Pictures" directory.
     * <p>
     * This method uses the MediaStore API, which is the modern and recommended approach for
     * saving shared media. The saved image will be visible in gallery apps.
     *
     * @param bitmap The bitmap image to save.
     * @return The Uri of the saved image on success, or null if the save operation failed.
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
                return uri; // Success
            } catch (Exception e) {
                Log.e(TAG, "Failed to save bitmap", e);
            }
        }
        return null; // Failure
    }

}