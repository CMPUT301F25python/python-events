package com.example.lotteryevent;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A {@link Fragment} subclass that provides a user interface for creating a new event.
 * Users can input event details such as name, location, date, time, and other settings.
 * The created event is then saved to a Firestore database.
 */
public class CreateEventFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextInputEditText editTextEventName, editTextEventDescription, editTextEventLocation;
    private TextInputEditText editTextEventStartDate, editTextEventStartTime;
    private TextInputEditText editTextEventEndDate, editTextEventEndTime;
    private TextInputEditText editTextEventPrice;
    private TextInputEditText editTextMaxAttendees;
    private SwitchMaterial switchGeolocation;
    private Button buttonSave;
    private final Calendar eventStartCalendar = Calendar.getInstance();
    private final Calendar eventEndCalendar = Calendar.getInstance();

    /**
     * Required empty public constructor for fragment instantiation.
     */
    public CreateEventFragment() {
    }


    /**
     * Inflates the layout for this fragment.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_event, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has returned,
     * but before any saved state has been restored in to the view. This is where UI initialization and setup occurs.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();


        // Find all UI elements by their ID
        editTextEventName = view.findViewById(R.id.edit_text_event_name);
        editTextEventDescription = view.findViewById(R.id.edit_text_event_description);
        editTextEventLocation = view.findViewById(R.id.edit_text_event_location);
        editTextEventPrice = view.findViewById(R.id.edit_text_event_price);

        editTextEventStartDate = view.findViewById(R.id.edit_text_event_start_date);
        editTextEventStartTime = view.findViewById(R.id.edit_text_event_start_time);

        editTextEventEndDate = view.findViewById(R.id.edit_text_event_end_date);
        editTextEventEndTime = view.findViewById(R.id.edit_text_event_end_time);

        editTextMaxAttendees = view.findViewById(R.id.edit_text_max_attendees);

        switchGeolocation = view.findViewById(R.id.switch_geolocation);
        buttonSave = view.findViewById(R.id.button_save);

        // Set up click listeners for date/time pickers and the save button
        setupClickListeners();
    }

    /**
     * Sets up OnClickListeners for all interactive UI elements in the fragment,
     * such as date/time fields and the save button.
     */
    private void setupClickListeners() {
        // Set up the picker dialogs
        editTextEventStartDate.setOnClickListener(v -> showDatePickerDialog(eventStartCalendar, editTextEventStartDate));
        editTextEventStartTime.setOnClickListener(v -> showTimePickerDialog(eventStartCalendar, editTextEventStartTime));

        editTextEventEndDate.setOnClickListener(v -> showDatePickerDialog(eventEndCalendar, editTextEventEndDate));
        editTextEventEndTime.setOnClickListener(v -> showTimePickerDialog(eventEndCalendar, editTextEventEndTime));

        // Set a click listener on the save button
        buttonSave.setOnClickListener(this::saveEventToFirestore);
    }


    /**
     * Displays a {@link DatePickerDialog} to allow the user to select a date.
     * The selected date is then stored in the provided Calendar object and displayed in the EditText.
     * @param calendar The Calendar instance to update with the selected date.
     * @param editText The TextInputEditText field to update with the formatted date string.
     */
    private void showDatePickerDialog(Calendar calendar, TextInputEditText editText) {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel(editText, calendar, "yyyy-MM-dd");
        };

        new DatePickerDialog(getContext(), dateSetListener, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }


    /**
     * Displays a {@link TimePickerDialog} to allow the user to select a time.
     * The selected time is then stored in the provided Calendar object and displayed in the EditText.
     * @param calendar The Calendar instance to update with the selected time.
     * @param editText The TextInputEditText field to update with the formatted time string.
     */
    private void showTimePickerDialog(Calendar calendar, TextInputEditText editText) {
        TimePickerDialog.OnTimeSetListener timeSetListener = (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            updateLabel(editText, calendar, "HH:mm");
        };

        new TimePickerDialog(getContext(), timeSetListener, calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), true).show(); // true for 24-hour view
    }

    /**
     * Updates the text of a {@link TextInputEditText} with a formatted date or time string from a Calendar object.
     * @param editText The view to update.
     * @param calendar The calendar containing the date/time to format.
     * @param format The desired date/time format (e.g., "yyyy-MM-dd" or "HH:mm").
     */
    private void updateLabel(TextInputEditText editText, Calendar calendar, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        editText.setText(sdf.format(calendar.getTime()));
    }


    /**
     * Reads all data from the input fields, performs validation, and saves the new event to Firestore.
     * Displays a toast message indicating success or failure and navigates back on success.
     * @param view The view that triggered the method call, used for navigation.
     */
    private void saveEventToFirestore(View view) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        // Get text from basic input fields
        String eventName = editTextEventName.getText().toString().trim();
        String description = editTextEventDescription.getText().toString().trim();
        String location = editTextEventLocation.getText().toString().trim();
        String priceStr = editTextEventPrice.getText().toString().trim();
        String maxAttendeesStr = editTextMaxAttendees.getText().toString().trim();
        boolean isGeoLocationRequired = switchGeolocation.isChecked();

        // Simple validation
        if (eventName.isEmpty() || location.isEmpty()) {
            Toast.makeText(getContext(), "Please fill out Event Name and Location", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Data Conversion with Error Handling ---
        Double price = priceStr.isEmpty() ? null : Double.parseDouble(priceStr);
        Integer capacity = maxAttendeesStr.isEmpty() ? null : Integer.parseInt(maxAttendeesStr);

        // Combine Date and Time Calendars into Firestore Timestamps
        // If date or time is not set, the timestamp will be null
        com.google.firebase.Timestamp eventStartDateTime = getTimestampFromCalendars(eventStartCalendar, editTextEventStartDate, editTextEventStartTime);
        com.google.firebase.Timestamp eventEndDateTime = getTimestampFromCalendars(eventEndCalendar, editTextEventEndDate, editTextEventEndTime);

        // Create a Map to hold the event data
        Map<String, Object> event = new HashMap<>();
        event.put("name", eventName);
        event.put("description", description);
        event.put("organizerId", userId);
        event.put("organizerName", "Organizer Name"); // TODO: Add logic to get organizer's name
        event.put("location", location);
        event.put("price", price);

        event.put("eventStartDateTime", eventStartDateTime);
        event.put("eventEndDateTime", eventEndDateTime);
        event.put("capacity", capacity);
        event.put("isGeoLocationRequired", isGeoLocationRequired);

        // Default values for new events
        event.put("status", "upcoming");
        event.put("qrCodeData", null);
        event.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        event.put("waitingListCount", 0);
        event.put("attendeeCount", 0);

        // Add the new event to the "events" collection
        db.collection("events")
                .add(event)
                .addOnSuccessListener(documentReference -> {
                    Log.d("FIRESTORE_SUCCESS", "Event saved with ID: " + documentReference.getId());
                    Toast.makeText(getContext(), "Event Created!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).popBackStack(); // Go back to home
                })
                .addOnFailureListener(e -> {
                    Log.w("FIRESTORE_ERROR", "Error adding document", e);
                    Toast.makeText(getContext(), "Error creating event. Please try again.", Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Creates a Firestore {@link com.google.firebase.Timestamp} from a Calendar object.
     * Returns null if the date or time fields have not been filled out by the user.
     * @param calendar The Calendar instance holding the selected date and time.
     * @param dateText The EditText for the date.
     * @param timeText The EditText for the time.
     * @return A Firestore Timestamp object, or null if the date/time is incomplete.
     */
    private com.google.firebase.Timestamp getTimestampFromCalendars(Calendar calendar, TextInputEditText dateText, TextInputEditText timeText) {
        if (dateText.getText().toString().isEmpty() || timeText.getText().toString().isEmpty()) {
            return null; // Return null if either date or time hasn't been picked
        }
        return new com.google.firebase.Timestamp(calendar.getTime());
    }
}

