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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.os.Environment;
import com.example.lotteryevent.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.OutputStream;

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
    private String eventId;
    private Button qrCodeRequest;
    private LinearLayout buttonContainer;
    private Button btnViewWaitingList, btnViewEntrantMap, btnAcceptedParticipants;
    private Button btnInvitedParticipants, btnCancelledParticipants, btnRunDraw, btnFinalize;

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

        // Retrieve the eventId from navigation arguments
        if (getArguments() != null) {
            eventId = OrganizerEventPageFragmentArgs.fromBundle(getArguments()).getEventId();
        }
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
        setupClickListeners();
    }

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
    }

    /**
     * Binds click listeners to the interactive UI elements on the page.
     */
    private void setupClickListeners() {
        qrCodeRequest.setOnClickListener(this::generateQrCode);

        btnRunDraw.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);
            Navigation.findNavController(v)
                    .navigate(R.id.action_organizerEventPageFragment_to_runDrawFragment, bundle);
        });

        // Add placeholder listeners for the other new buttons
        View.OnClickListener notImplementedListener = v -> {
            Button b = (Button) v;
            Toast.makeText(getContext(), b.getText().toString() + " not implemented yet.", Toast.LENGTH_SHORT).show();
        };

        btnViewWaitingList.setOnClickListener(notImplementedListener);
        btnViewEntrantMap.setOnClickListener(notImplementedListener);
        btnAcceptedParticipants.setOnClickListener(notImplementedListener);
        btnInvitedParticipants.setOnClickListener(notImplementedListener);
        btnCancelledParticipants.setOnClickListener(notImplementedListener);
        btnFinalize.setOnClickListener(notImplementedListener);
    }


    /**
     * Fetches the details of the event from the Firestore database using the eventId.
     * On success, it updates the UI with the event's name.
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
                            updateUi(name, status);
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

    /**
     * Updates the UI elements with the fetched event data.
     * Specifically, sets the title in the activity's action bar.
     * @param title The title of the event.
     */
    private void updateUi(String title, String status) {
        if (getView() != null) {
            TextView eventNameLabel = getView().findViewById(R.id.event_name_label);
            eventNameLabel.setText(title);
        }

        if (status == null) {
            buttonContainer.setVisibility(View.GONE);
            return;
        }

        switch (status) {
            case "open":
            case "closed":
                buttonContainer.setVisibility(View.VISIBLE);
                setButtonStates(true, true, false, false, false, true, false);
                break;
            case "drawing_complete":
                buttonContainer.setVisibility(View.VISIBLE);
                setButtonStates(true, true, true, true, true, true, true);
                break;
            case "finished":
            case "upcoming":
            default:
                buttonContainer.setVisibility(View.GONE);
                break;
        }
    }

    private void setButtonStates(boolean waitingList, boolean map, boolean accepted,
                                 boolean invited, boolean cancelled, boolean runDraw, boolean finalize) {
        btnViewWaitingList.setEnabled(waitingList);
        btnViewEntrantMap.setEnabled(map);
        btnAcceptedParticipants.setEnabled(accepted);
        btnInvitedParticipants.setEnabled(invited);
        btnCancelledParticipants.setEnabled(cancelled);
        btnRunDraw.setEnabled(runDraw);
        btnFinalize.setEnabled(finalize);
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