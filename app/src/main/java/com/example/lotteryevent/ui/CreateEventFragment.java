package com.example.lotteryevent.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.example.lotteryevent.R;
import com.example.lotteryevent.repository.EventRepositoryImpl;
import com.example.lotteryevent.repository.IEventRepository;
import com.example.lotteryevent.viewmodels.CreateEventViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * A {@link Fragment} that provides a UI for creating a new event.
 * This class follows MVVM principles, delegating all business and data logic
 * to the {@link CreateEventViewModel}. Its sole responsibility is to manage the UI
 * and forward user events to the ViewModel.
 */
public class CreateEventFragment extends Fragment {
    private static final String TAG = "CreateEventFragment";

    // --- UI Components ---
    private EditText editTextEventName, editTextEventDescription, editTextEventLocation,
            editTextEventStartDate, editTextEventStartTime, editTextEventEndDate,
            editTextEventEndTime, editTextRegistrationStartDate, editTextRegistrationStartTime,
            editTextRegistrationEndDate, editTextRegistrationEndTime, editTextEventPrice,
            editTextMaxAttendees, editTextWaitingListLimit;
    private CheckBox checkboxGeolocation;
    private Button buttonSave;
    private CardView cardViewPosterSection;

    // Variables for image upload and preview
    private static final int REQUEST_POSTER_IMAGE = 2001;
    private String selectedPosterBase64 = null;
    private ImageView imageViewPoster;
    private Button buttonUploadPoster;
    private Button buttonRemovePoster;

    // --- Calendars for Date/Time Pickers (UI State) ---
    private final Calendar eventStartCalendar = Calendar.getInstance();
    private final Calendar eventEndCalendar = Calendar.getInstance();
    private final Calendar registrationStartCalendar = Calendar.getInstance();
    private final Calendar registrationEndCalendar = Calendar.getInstance();

    // --- ViewModel ---
    private CreateEventViewModel viewModel;
    private ViewModelProvider.Factory viewModelFactory;

    /**
     * Default constructor for production use by the Android Framework.
     */
    public CreateEventFragment() {}

    /**
     * Constructor for testing. Allows us to inject a custom ViewModelFactory.
     * @param factory The factory to use for creating the ViewModel.
     */
    public CreateEventFragment(ViewModelProvider.Factory factory) {
        this.viewModelFactory = factory;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- ViewModel Initialization ---
        if (viewModelFactory == null) {
            // This is the production path.
            IEventRepository eventRepository = new EventRepositoryImpl();
            GenericViewModelFactory factory = new GenericViewModelFactory();
            factory.put(CreateEventViewModel.class, () -> new CreateEventViewModel(eventRepository));
            viewModelFactory = factory;
        }
        viewModel = new ViewModelProvider(this, viewModelFactory).get(CreateEventViewModel.class);

        // --- UI Setup ---
        bindViews(view);
        setupClickListeners();
        setupObservers();
    }

    /**
     * Sets up observers on the ViewModel's LiveData to react to state changes.
     */
    private void setupObservers() {
        /**
         * Observe loading state to enable/disable the save button
         * @param isLoading boolean for loading
         */
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            buttonSave.setEnabled(!isLoading);
        });

        /**
         * Observe messages to show success/error toasts and handle navigation
         * @param message message to show
         */
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                // If the message indicates success, navigate back.
                if (message.contains("successfully")) {
                    Navigation.findNavController(requireView()).popBackStack();
                }
            }
        });
    }

    /**
     * Gathers all input from the UI and passes it to the ViewModel to attempt event creation.
     */
    private void handleSaveEvent() {
        // Gather all raw string data from EditText fields.
        String eventName = editTextEventName.getText().toString().trim();
        String description = editTextEventDescription.getText().toString().trim();
        String location = editTextEventLocation.getText().toString().trim();
        String priceStr = editTextEventPrice.getText().toString().trim();
        String maxAttendeesStr = editTextMaxAttendees.getText().toString().trim();
        String waitingListLimitStr = editTextWaitingListLimit.getText().toString().trim();
        boolean isGeoLocationRequired = checkboxGeolocation.isChecked();

        String startDateText = editTextEventStartDate.getText().toString();
        String startTimeText = editTextEventStartTime.getText().toString();
        String endDateText = editTextEventEndDate.getText().toString();
        String endTimeText = editTextEventEndTime.getText().toString();
        String regStartDateText = editTextRegistrationStartDate.getText().toString();
        String regStartTimeText = editTextRegistrationStartTime.getText().toString();
        String regEndDateText = editTextRegistrationEndDate.getText().toString();
        String regEndTimeText = editTextRegistrationEndTime.getText().toString();

        // Pass all the data to the ViewModel. The ViewModel will handle all validation and logic.
        String validationError = viewModel.attemptToCreateEvent(
                eventName, description, location, priceStr, maxAttendeesStr, waitingListLimitStr, isGeoLocationRequired,
                selectedPosterBase64, eventStartCalendar, eventEndCalendar, registrationStartCalendar, registrationEndCalendar,
                startDateText, startTimeText, endDateText, endTimeText,
                regStartDateText, regStartTimeText, regEndDateText, regEndTimeText
        );

        // If the ViewModel returns an error, it means client-side validation failed. Show the error.
        if (validationError != null) {
            Toast.makeText(getContext(), validationError, Toast.LENGTH_LONG).show();
        }
        // If validationError is null, the creation process has started. The LiveData observers will handle the result.
    }


    /**
     * Finds and assigns all UI component instances from the inflated view.
     * @param view The root view of the fragment.
     */
    private void bindViews(@NonNull View view) {
        editTextEventName = view.findViewById(R.id.edit_text_event_name);
        editTextEventDescription = view.findViewById(R.id.edit_text_event_description);
        editTextEventLocation = view.findViewById(R.id.edit_text_event_location);
        editTextEventPrice = view.findViewById(R.id.edit_text_event_price);

        editTextEventStartDate = view.findViewById(R.id.edit_text_event_start_date);
        editTextEventStartTime = view.findViewById(R.id.edit_text_event_start_time);

        editTextEventEndDate = view.findViewById(R.id.edit_text_event_end_date);
        editTextEventEndTime = view.findViewById(R.id.edit_text_event_end_time);

        editTextRegistrationStartDate = view.findViewById(R.id.edit_text_registration_start_date);
        editTextRegistrationStartTime = view.findViewById(R.id.edit_text_registration_start_time);

        editTextRegistrationEndDate = view.findViewById(R.id.edit_text_registration_end_date);
        editTextRegistrationEndTime = view.findViewById(R.id.edit_text_registration_end_time);

        editTextMaxAttendees = view.findViewById(R.id.edit_text_max_attendees);
        editTextWaitingListLimit = view.findViewById(R.id.edit_text_waiting_list_limit);

        checkboxGeolocation = view.findViewById(R.id.checkbox_geolocation);
        buttonSave = view.findViewById(R.id.button_save);

        imageViewPoster = view.findViewById(R.id.image_view_poster_preview);
        buttonUploadPoster = view.findViewById(R.id.button_upload_poster);
        buttonRemovePoster = view.findViewById(R.id.button_remove_poster);
        cardViewPosterSection = view.findViewById(R.id.card_view_poster_section);
    }

    /**
     * Sets up OnClickListeners for all interactive UI elements in the fragment.
     * This includes:* <ul>
     *     <li>Date and time pickers for event and registration windows.</li>
     *     <li>Poster image upload and removal buttons.</li>
     *     <li>The save button to trigger event creation.</li>
     * </ul>
     */
    private void setupClickListeners() {
        // Set up the picker dialogs for each date and time field
        /**
         * Shows date picker dialog on click
         * @param view clicked
         */
        editTextEventStartDate.setOnClickListener(v -> showDatePickerDialog(eventStartCalendar, editTextEventStartDate));
        /**
         * Shows time picker dialog on click
         * @param view clicked
         */
        editTextEventStartTime.setOnClickListener(v -> showTimePickerDialog(eventStartCalendar, editTextEventStartTime));
        /**
         * Shows date picker dialog on click
         * @param view clicked
         */
        editTextEventEndDate.setOnClickListener(v -> showDatePickerDialog(eventEndCalendar, editTextEventEndDate));
        /**
         * Shows time picker dialog on click
         * @param view clicked
         */
        editTextEventEndTime.setOnClickListener(v -> showTimePickerDialog(eventEndCalendar, editTextEventEndTime));
        /**
         * Shows date picker dialog on click
         * @param view clicked
         */
        editTextRegistrationStartDate.setOnClickListener(v -> showDatePickerDialog(registrationStartCalendar, editTextRegistrationStartDate));
        /**
         * Shows time picker dialog on click
         * @param view clicked
         */
        editTextRegistrationStartTime.setOnClickListener(v -> showTimePickerDialog(registrationStartCalendar, editTextRegistrationStartTime));
        /**
         * Shows date picker dialog on click
         * @param view clicked
         */
        editTextRegistrationEndDate.setOnClickListener(v -> showDatePickerDialog(registrationEndCalendar, editTextRegistrationEndDate));
        /**
         * Shows time picker dialog on click
         * @param view clicked
         */
        editTextRegistrationEndTime.setOnClickListener(v -> showTimePickerDialog(registrationEndCalendar, editTextRegistrationEndTime));

        /**
         * Shows poster picker on click
         * @param view clicked
         */
        buttonUploadPoster.setOnClickListener(v -> openPosterPicker());

        /**
         * Removes poster, resets UI
         * @param view clicked
         */
        buttonRemovePoster.setOnClickListener(v -> {
            // 1. Clear the variable
            selectedPosterBase64 = null;

            // 2. Reset UI
            imageViewPoster.setImageResource(R.drawable.ic_launcher_background); // Or your placeholder ID
            cardViewPosterSection.setVisibility(View.GONE);

        });

        /**
         * Set a click listener on the save button to trigger the ViewModel action
         * @param view clicked
         */
        buttonSave.setOnClickListener(v -> handleSaveEvent());
    }

    /**
     * Displays a {@link DatePickerDialog} to allow the user to select a date.
     * The selected date is then stored in the provided Calendar object and displayed in the EditText.
     * @param calendar The Calendar instance to update with the selected date.
     * @param editText The EditText field to update with the formatted date string.
     */
    private void showDatePickerDialog(Calendar calendar, EditText editText) {
        /**
         * Handles the date selected from the DatePicker dialog and updates the UI field
         * @param view date picker view
         * @param year selected year
         * @param month selected month (0-indexed)
         * @param dayOfMonth selected day of the month
         */
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel(editText, calendar, "yyyy-MM-dd");
        };

        DatePickerDialog dialog = new DatePickerDialog(requireContext(), dateSetListener, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        /**
         * Clears input
         * @param d dialog interface
         * @param which button identifier
         */
        dialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, "Clear", (d, which) -> {
            editText.setText(""); // Clear the visible text
        });

        dialog.show();
    }

    /**
     * Displays a {@link TimePickerDialog} to allow the user to select a time.
     * The selected time is then stored in the provided Calendar object and displayed in the EditText.
     * @param calendar The Calendar instance to update with the selected time.
     * @param editText The EditText field to update with the formatted time string.
     */
    private void showTimePickerDialog(Calendar calendar, EditText editText) {
        /**
         * Handles the time selected from the TimePicker dialog and updates the UI field
         * @param view time picker view
         * @param hourOfDay selected hour (24-hour format)
         * @param minute selected minute
         */
        TimePickerDialog.OnTimeSetListener timeSetListener = (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            updateLabel(editText, calendar, "HH:mm");
        };

        TimePickerDialog dialog = new TimePickerDialog(requireContext(), timeSetListener, calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), true);

        /**
         * Clears input
         * @param d dialog interface
         * @param which button identifier
         */
        dialog.setButton(TimePickerDialog.BUTTON_NEUTRAL, "Clear", (d, which) -> {
            editText.setText(""); // Clear the visible text in the EditText
        });

        dialog.show();
    }

    /**
     * Updates the text of a {@link EditText} with a formatted date or time string from a Calendar object.
     * @param editText The view to update.
     * @param calendar The calendar containing the date/time to format.
     * @param format   The desired date/time format (e.g., "yyyy-MM-dd" or "HH:mm").
     */
    private void updateLabel(EditText editText, Calendar calendar, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        editText.setText(sdf.format(calendar.getTime()));
    }

    /**
     * Launches an intent to open the device's image gallery.
     * The result is handled in {@link #onActivityResult(int, int, Intent)}, where the
     * selected image is decoded and displayed.
     */
    private void openPosterPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_POSTER_IMAGE);
    }

    /**
     * Encodes the given {@link Bitmap} as a Base64 string.
     * @param bitmap The bitmap to encode.
     * @return A Base64-encoded representation of the bitmap.
     */
    private String encodeBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] bytes = baos.toByteArray();
        return android.util.Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    /**
     * Handles results from started activities, specifically the image picker.
     * <p>
     * If a valid image is selected:
     * <ul>
     *     <li>The bitmap is retrieved and displayed in the preview ImageView.</li>
     *     <li>The bitmap is encoded to a Base64 string for storage.</li>
     *     <li>The poster preview section is made visible.</li>
     * </ul>
     * </p>
     *
     * @param requestCode The original request code (e.g., REQUEST_POSTER_IMAGE).
     * @param resultCode  The result code returned by the activity (RESULT_OK).
     * @param data        The returned intent data containing the selected image Uri.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_POSTER_IMAGE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);

                // Update the UI preview
                imageViewPoster.setImageBitmap(bitmap);
                selectedPosterBase64 = encodeBitmapToBase64(bitmap);

                cardViewPosterSection.setVisibility(View.VISIBLE);
                buttonUploadPoster.setText("Change Image");

            } catch (Exception e) {
                Log.e(TAG, "Failed to load poster image", e);
                Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

}