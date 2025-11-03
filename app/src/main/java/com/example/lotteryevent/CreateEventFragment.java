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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateEventFragment extends Fragment {

    private FirebaseFirestore db;
    private TextInputEditText editTextEventName, editTextEventDescription, editTextEventLocation;
    private TextInputEditText editTextEventStartDate, editTextEventStartTime;
    private TextInputEditText editTextEventEndDate, editTextEventEndTime;
    private TextInputEditText editTextEventPrice;
    private TextInputEditText editTextMaxAttendees;
    private SwitchMaterial switchGeolocation;
    private Button buttonSave;
    private final Calendar eventStartCalendar = Calendar.getInstance();
    private final Calendar eventEndCalendar = Calendar.getInstance();

    public CreateEventFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

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

    private void setupClickListeners() {
        // Set up the picker dialogs
        editTextEventStartDate.setOnClickListener(v -> showDatePickerDialog(eventStartCalendar, editTextEventStartDate));
        editTextEventStartTime.setOnClickListener(v -> showTimePickerDialog(eventStartCalendar, editTextEventStartTime));

        editTextEventEndDate.setOnClickListener(v -> showDatePickerDialog(eventEndCalendar, editTextEventEndDate));
        editTextEventEndTime.setOnClickListener(v -> showTimePickerDialog(eventEndCalendar, editTextEventEndTime));

        // Set a click listener on the save button
        buttonSave.setOnClickListener(this::saveEventToFirestore);
    }

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

    private void showTimePickerDialog(Calendar calendar, TextInputEditText editText) {
        TimePickerDialog.OnTimeSetListener timeSetListener = (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            updateLabel(editText, calendar, "HH:mm");
        };

        new TimePickerDialog(getContext(), timeSetListener, calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), true).show(); // true for 24-hour view
    }

    private void updateLabel(TextInputEditText editText, Calendar calendar, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        editText.setText(sdf.format(calendar.getTime()));
    }


    /**
     * Reads data from all input fields, validates it, and saves it to Firestore.
     */
    private void saveEventToFirestore(View view) {
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
        event.put("organizerId", "some_user_id"); // TODO: Add logic to get user ID
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

    private com.google.firebase.Timestamp getTimestampFromCalendars(Calendar calendar, TextInputEditText dateText, TextInputEditText timeText) {
        if (dateText.getText().toString().isEmpty() || timeText.getText().toString().isEmpty()) {
            return null; // Return null if either date or time hasn't been picked
        }
        return new com.google.firebase.Timestamp(calendar.getTime());
    }
}

