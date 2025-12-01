package com.example.lotteryevent.ui.organizer;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ContentResolver;
import android.graphics.Matrix;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
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
import androidx.exifinterface.media.ExifInterface;

import com.example.lotteryevent.R;
import com.example.lotteryevent.repository.IOrganizerEventRepository;
import com.example.lotteryevent.repository.OrganizerEventRepositoryImpl;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.example.lotteryevent.viewmodels.OrganizerEventViewModel;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;

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
    private static final int REQUEST_POSTER_IMAGE = 2001;
    private static final int POSTER_MAX_DIM_PX = 1200; // longest side cap
    private static final int POSTER_JPEG_QUALITY = 80; // keep existing quality

    private OrganizerEventViewModel viewModel;
    private String eventId;
    private ViewModelProvider.Factory viewModelFactory;

    // UI Components
    private TextView eventNameLabel;
    private ImageView posterImage;
    private Button uploadPosterButton;
    private Button qrCodeRequest;
    private LinearLayout buttonContainer;
    private Button btnViewEntrants, btnViewEntrantMap;
    private Button btnRunDraw, btnFinalize;
    private Button btnExportEntrantCSV;

    /**
     * Default constructor for production use by the Android Framework.
     */
    public OrganizerEventFragment() { }

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
        /**
         * Observes event details, displays poster
         * @param event event to show poster
         */
        viewModel.getEvent().observe(getViewLifecycleOwner(), event -> {
                    if (event != null) {
                        eventNameLabel.setText(event.getName());
                        if (!event.getGeoLocationRequired()) {
                            btnViewEntrantMap.setVisibility(View.GONE);

                            // Display poster image if Base64 data is available
                            String posterImageUrl = event.getPosterImageUrl();
                            if (posterImageUrl != null && !posterImageUrl.isEmpty()) {
                                try {
                                    byte[] bytes = Base64.decode(posterImageUrl, Base64.DEFAULT);
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    posterImage.setImageBitmap(bitmap);

                                    // Poster exists -> show "Update Poster"
                                    uploadPosterButton.setText("Update Poster");
                                } catch (IllegalArgumentException e) {
                                    Log.e(TAG, "Invalid poster Base64 data", e);
                                    // Invalid data: keep placeholder and default label
                                    uploadPosterButton.setText("Upload Poster");
                                }
                            } else {
                                // No poster yet -> show default label and placeholder
                                uploadPosterButton.setText("Upload Poster");
                                posterImage.setImageResource(R.drawable.outline_add_photo_alternate_24);
                            }
                        }
                    }
                });

        // Observer for the overall UI state (determines which buttons are visible)
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            // Reset visibility before applying the new state
            buttonContainer.setVisibility(View.VISIBLE);
            btnRunDraw.setVisibility(View.VISIBLE);
            btnFinalize.setVisibility(View.VISIBLE);
            btnExportEntrantCSV.setVisibility(View.GONE);

            switch (state) {
                case UPCOMING:
                    buttonContainer.setVisibility(View.GONE);
                    break;
                case FINALIZED:
                    btnRunDraw.setVisibility(View.GONE);
                    btnFinalize.setVisibility(View.GONE);
                    btnExportEntrantCSV.setVisibility(View.VISIBLE);
                    break;
                case OPEN:
                    btnFinalize.setEnabled(true);
                    break;
                case ERROR:
                    Toast.makeText(getContext(), "Error loading event state.", Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        /**
         * Observer to enable/disable the "Run Draw" button based on capacity
         * @param isEnabled boolean for enabling button
         */
        viewModel.isRunDrawButtonEnabled().observe(getViewLifecycleOwner(), isEnabled -> {
            btnRunDraw.setEnabled(isEnabled);
        });

        /**
         * Observer to show the QR code dialog when a bitmap is generated
         * @param bitmap view of QR code
         */
        viewModel.getQrCodeBitmap().observe(getViewLifecycleOwner(), bitmap -> {
            if (bitmap != null) {
                showQrCodeDialog(bitmap);
                viewModel.onQrCodeShown(); // Inform ViewModel the QR code has been handled
            }
        });

        /**
         * Observer for any messages (e.g., errors) from the ViewModel
         * @param message message to show
         */
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        /**
         * Observer for the CSV content to be exported
         * @param csvContent content for CSV
         */
        viewModel.getCsvContent().observe(getViewLifecycleOwner(), csvContent -> {
            if (csvContent != null) {
                saveCsvToDownloads(csvContent);
                viewModel.onCsvExported(); // Reset the LiveData
            }
        });
    }

    /**
     * Initializes the UI components by finding them in the view hierarchy.
     * @param view The root view of the fragment.
     */
    private void initializeViews(View view) {
        eventNameLabel = view.findViewById(R.id.event_name_label);
        posterImage = view.findViewById(R.id.poster_image);
        uploadPosterButton = view.findViewById(R.id.upload_poster_button);
        qrCodeRequest = view.findViewById(R.id.request_qr_code);
        buttonContainer = view.findViewById(R.id.organizer_button_container);
        btnViewEntrants = view.findViewById(R.id.btnViewEntrants);
        btnViewEntrantMap = view.findViewById(R.id.btnViewEntrantMap);
        btnRunDraw = view.findViewById(R.id.btnRunDraw);
        btnFinalize = view.findViewById(R.id.btnFinalize);
        btnExportEntrantCSV = view.findViewById(R.id.btnExportCSV);
    }

    /**
     * Binds click listeners to the interactive UI elements on the page.
     */
    private void setupClickListeners() {
        qrCodeRequest.setOnClickListener(v -> viewModel.generateQrCode(eventId));
        uploadPosterButton.setOnClickListener(v -> openPosterPicker());
        /**
         * Navigates to run draw fragment
         * @param v view clicked
         */
        btnRunDraw.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);
            Navigation.findNavController(v)
                    .navigate(R.id.action_organizerEventPageFragment_to_runDrawFragment, bundle);
        });

        /**
         * Navigates to entrant list fragment
         * @param v view clicked
         */
        btnViewEntrants.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);
            Navigation.findNavController(v)
                    .navigate(R.id.action_organizerEventPageFragment_to_entrantListFragment, bundle);
        });


        btnViewEntrantMap.setOnClickListener(v -> {
            OrganizerEventFragmentDirections.ActionOrganizerEventPageFragmentToEntrantMapFragment action =
                    OrganizerEventFragmentDirections.actionOrganizerEventPageFragmentToEntrantMapFragment(eventId);
            Navigation.findNavController(v).navigate(action);
        });

        btnFinalize.setOnClickListener(v -> showFinalizeConfirmationDialog());
        btnExportEntrantCSV.setOnClickListener(v -> viewModel.generateEntrantCsv());
    }

    /**
     * Launches an image picker so the organizer can select a poster image for the event.
     */
    private void openPosterPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_POSTER_IMAGE);
    }

    /**
     * Handles results from started activities, including the image picker for the event poster.
     * @param requestCode The original request code used to start the activity.
     * @param resultCode The result code returned by the activity.
     * @param data The returned intent data, which may contain the selected image Uri.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_POSTER_IMAGE
                && resultCode == Activity.RESULT_OK
                && data != null
                && data.getData() != null) {

            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = decodeScaledBitmapFromUri(imageUri, POSTER_MAX_DIM_PX);

                // Update the UI preview
                posterImage.setImageBitmap(bitmap);

                // Encode to Base64 and send to ViewModel to save on the event
                String posterImageUrl = encodeBitmapToBase64(bitmap);
                if (eventId != null && !eventId.isEmpty()) {
                    viewModel.updateEventPoster(eventId, posterImageUrl);
                    Toast.makeText(getContext(), "Event poster updated.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Unable to update poster: missing event ID.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load poster image", e);
                Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Encodes the given {@link Bitmap} as a Base64 string.
     * @param bitmap The bitmap to encode.
     * @return A Base64-encoded representation of the bitmap.
     */
    private String encodeBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, POSTER_JPEG_QUALITY, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
    }

    /**
     * Decodes a scaled bitmap from the given Uri.
     * @param imageUri
     * @param maxDimPx
     * @return Decoded bitmap
     * @throws IOException
     */
    private Bitmap decodeScaledBitmapFromUri(@NonNull Uri imageUri, int maxDimPx) throws IOException {
        ContentResolver resolver = requireContext().getContentResolver();

        // 1) Decode bounds only
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream is = resolver.openInputStream(imageUri)) {
            if (is == null) throw new IOException("Unable to open image stream for bounds.");
            BitmapFactory.decodeStream(is, null, bounds);
        }

        int srcW = bounds.outWidth;
        int srcH = bounds.outHeight;
        if (srcW <= 0 || srcH <= 0) throw new IOException("Invalid image bounds.");

        // 2) Decode with sampling (prevents loading full-res bitmap)
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = calculateInSampleSizeForMaxDim(srcW, srcH, maxDimPx);
        opts.inJustDecodeBounds = false;

        Bitmap decoded;
        try (InputStream is = resolver.openInputStream(imageUri)) {
            if (is == null) throw new IOException("Unable to open image stream for decode.");
            decoded = BitmapFactory.decodeStream(is, null, opts);
        }
        if (decoded == null) throw new IOException("Bitmap decode returned null.");

        // 3) Fix rotation (common for camera photos)
        Bitmap rotated = rotateBitmapIfRequired(imageUri, decoded);
        if (rotated != decoded) decoded.recycle();

        // 4) Final exact scale-down (in case sampling didn’t land under maxDim)
        Bitmap finalBmp = scaleDownToMaxDim(rotated, maxDimPx);
        if (finalBmp != rotated) rotated.recycle();

        return finalBmp;
    }

    /**
     * Calculates the in-sample size for decoding a bitmap.
     * @param width
     * @param height
     * @param maxDimPx
     * @return In-sample size
     */
    private static int calculateInSampleSizeForMaxDim(int width, int height, int maxDimPx) {
        int inSampleSize = 1;
        int max = Math.max(width, height);
        while (max / inSampleSize > maxDimPx) {
            inSampleSize *= 2; // power of two (efficient for BitmapFactory)
        }
        return Math.max(1, inSampleSize);
    }

    /**
     * Scales the given bitmap down to a maximum dimension.
     * @param bitmap
     * @param maxDimPx
     * @return Scaled bitmap
     */
    private static Bitmap scaleDownToMaxDim(@NonNull Bitmap bitmap, int maxDimPx) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int max = Math.max(w, h);
        if (max <= maxDimPx) return bitmap;

        float scale = maxDimPx / (float) max;
        int newW = Math.max(1, Math.round(w * scale));
        int newH = Math.max(1, Math.round(h * scale));
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true);
    }

    /**
     * Rotates the given bitmap if required based on EXIF data.
     * @param imageUri
     * @param bitmap
     * @return Bitmap with rotation applied if required
     */
    private Bitmap rotateBitmapIfRequired(@NonNull Uri imageUri, @NonNull Bitmap bitmap) {
        try (InputStream is = requireContext().getContentResolver().openInputStream(imageUri)) {
            if (is == null) return bitmap;

            ExifInterface exif = new ExifInterface(is);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            int rotationDegrees = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationDegrees = 90;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationDegrees = 180;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationDegrees = 270;

            if (rotationDegrees == 0) return bitmap;

            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);

            return Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true
            );
        } catch (Exception ignored) {
            return bitmap; // if EXIF fails, don't block user
        }
    }

    /**
     * Saves a text string as a .csv file in the public Downloads directory.
     * Handles differences between Android 10+ (API 29) and older versions.
     */
    private void saveCsvToDownloads(String csvContent) {
        String fileName = "entrants_" + eventId + "_" + System.currentTimeMillis() + ".csv";

        // Check if we are running on Android 10 (Q) or higher
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // --- API 29+ Logic (Scoped Storage) ---
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            try {
                Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri)) {
                        if (outputStream != null) {
                            outputStream.write(csvContent.getBytes());
                            Toast.makeText(getContext(), "Exported to Downloads: " + fileName, Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(getContext(), "Could not create file in Downloads.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving CSV (API 29+)", e);
                Toast.makeText(getContext(), "Failed to save CSV file.", Toast.LENGTH_SHORT).show();
            }

        } else {
            // --- API < 29 Logic (Legacy Storage) ---
            try {
                java.io.File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                java.io.File file = new java.io.File(downloadsDir, fileName);

                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    fos.write(csvContent.getBytes());
                    Toast.makeText(getContext(), "Exported to Downloads: " + fileName, Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving CSV (Legacy)", e);
                Toast.makeText(getContext(), "Failed to save CSV. Check Permissions.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Shows a confirmation dialog ensuring the user wants to finalize the event.
     */
    private void showFinalizeConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Finalize Event")
                .setMessage("Are you sure you want to finalize this event? \n\n" +
                        "This will prevent further entrants from joining and invited entrants from accepting their invitations. " +
                        "This action cannot be undone.")
                /**
                 * Call the ViewModel to execute the logic
                 * @param dialog dialog that triggered callback
                 * @param which button identifier
                 */
                .setPositiveButton("Finalize", (dialog, which) -> {
                    viewModel.finalizeEvent(eventId);
                })
                /**
                 * Dismisses dialog
                 * @param dialog dialog that triggered callback
                 * @param which button identifier
                 */
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

        /**
         * Saves bitmap to device's images and makes toast
         * @param v view clicked
         */
        saveButton.setOnClickListener(v -> {
            Uri uri = saveBitmapToPictures(qrCodeBitmap);
            if (uri != null) {
                Toast.makeText(getContext(), "QR Code saved to Photos!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to save QR Code.", Toast.LENGTH_SHORT).show();
            }
        });

        /**
         * Shares QR code
         * @param v view clicked
         */
        shareButton.setOnClickListener(v -> shareQrCode(qrCodeBitmap));
        /**
         * Dismisses dialog
         * @param v view clicked
         */
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