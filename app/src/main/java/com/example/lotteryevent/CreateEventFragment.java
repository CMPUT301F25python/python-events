package com.example.lotteryevent;
import com.example.lotteryevent.data.Event;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.firestore.DocumentSnapshot;
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
    private EditText editTextEventName, editTextEventDescription, editTextEventLocation;
    private EditText editTextEventStartDate, editTextEventStartTime;
    private EditText editTextEventEndDate, editTextEventEndTime;
    private EditText editTextRegistrationStartDate, editTextRegistrationStartTime;
    private EditText editTextRegistrationEndDate, editTextRegistrationEndTime;
    private EditText editTextEventPrice;
    private EditText editTextMaxAttendees;
    private EditText editTextLotteryGuidelines;
    private CheckBox checkboxGeolocation;
    private Button buttonSave;
    private final Calendar eventStartCalendar = Calendar.getInstance();
    private final Calendar eventEndCalendar = Calendar.getInstance();
    private final Calendar registrationStartCalendar = Calendar.getInstance();
    private final Calendar registrationEndCalendar = Calendar.getInstance();

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

        editTextRegistrationStartDate = view.findViewById(R.id.edit_text_registration_start_date);
        editTextRegistrationStartTime = view.findViewById(R.id.edit_text_registration_start_time);

        editTextRegistrationEndDate = view.findViewById(R.id.edit_text_registration_end_date);
        editTextRegistrationEndTime = view.findViewById(R.id.edit_text_registration_end_time);

        editTextMaxAttendees = view.findViewById(R.id.edit_text_max_attendees);

        editTextLotteryGuidelines = view.findViewById(R.id.edit_text_lottery_guidelines);

        checkboxGeolocation = view.findViewById(R.id.checkbox_geolocation);
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

        editTextRegistrationStartDate.setOnClickListener(v -> showDatePickerDialog(registrationStartCalendar, editTextRegistrationStartDate));
        editTextRegistrationStartTime.setOnClickListener(v -> showTimePickerDialog(registrationStartCalendar, editTextRegistrationStartTime));

        editTextRegistrationEndDate.setOnClickListener(v -> showDatePickerDialog(registrationEndCalendar, editTextRegistrationEndDate));
        editTextRegistrationEndTime.setOnClickListener(v -> showTimePickerDialog(registrationEndCalendar, editTextRegistrationEndTime));

        // Set a click listener on the save button
        buttonSave.setOnClickListener(this::saveEventToFirestore);
    }

    /**
     * Displays a {@link DatePickerDialog} to allow the user to select a date.
     * The selected date is then stored in the provided Calendar object and displayed in the EditText.
     * @param calendar The Calendar instance to update with the selected date.
     * @param editText The EditText field to update with the formatted date string.
     */
    private void showDatePickerDialog(Calendar calendar, EditText editText) {
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
     * @param editText The EditText field to update with the formatted time string.
     */
    private void showTimePickerDialog(Calendar calendar, EditText editText) {
        TimePickerDialog.OnTimeSetListener timeSetListener = (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            updateLabel(editText, calendar, "HH:mm");
        };

        new TimePickerDialog(getContext(), timeSetListener, calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), false).show(); // false for 12-hour view
    }

    /**
     * Updates the text of a {@link EditText} with a formatted date or time string from a Calendar object.
     * @param editText The view to update.
     * @param calendar The calendar containing the date/time to format.
     * @param format The desired date/time format (e.g., "yyyy-MM-dd" or "HH:mm").
     */
    private void updateLabel(EditText editText, Calendar calendar, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        editText.setText(sdf.format(calendar.getTime()));
    }

    /**
     * Gathers and validates all user input from the form fields. If validation passes,
     * it asynchronously fetches the organizer's name from the 'users' collection using the
     * current user's ID. On success, it constructs an {@link Event} object and saves it
     * as a new document in the 'events' collection in Firestore.
     * <p>
     * Validation checks include:
     * <ul>
     *     <li>Ensuring a user is logged in.</li>
     *     <li>Verifying that if a date is selected, the corresponding time is also selected.</li>
     *     <li>Confirming that the event start time is before the event end time.</li>
     * </ul>
     * Displays toast messages for validation errors or network failures. Navigates back
     * to the previous screen on successful event creation.
     *
     * @param view The view that triggered the method call, used for navigation purposes.
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
        String lotteryGuidelinesStr = editTextLotteryGuidelines.getText().toString().trim();
        boolean isGeoLocationRequired = checkboxGeolocation.isChecked();

        // Combine Date and Time Calendars into Firestore Timestamps
        com.google.firebase.Timestamp eventStartDateTime = getTimestampFromCalendars(eventStartCalendar, editTextEventStartDate, editTextEventStartTime);
        com.google.firebase.Timestamp eventEndDateTime = getTimestampFromCalendars(eventEndCalendar, editTextEventEndDate, editTextEventEndTime);

        com.google.firebase.Timestamp registrationStartDateTime = getTimestampFromCalendars(registrationStartCalendar, editTextRegistrationStartDate, editTextRegistrationStartTime);
        com.google.firebase.Timestamp registrationEndDateTime = getTimestampFromCalendars(registrationEndCalendar, editTextRegistrationEndDate, editTextRegistrationEndTime);

        String validationError = validateEventInput(
                eventName,
                editTextEventStartDate.getText().toString(),
                editTextEventStartTime.getText().toString(),
                editTextEventEndDate.getText().toString(),
                editTextEventEndTime.getText().toString(),
                eventStartDateTime,
                eventEndDateTime,
                editTextRegistrationStartDate.getText().toString(),
                editTextRegistrationStartTime.getText().toString(),
                editTextRegistrationEndDate.getText().toString(),
                editTextRegistrationEndTime.getText().toString(),
                registrationStartDateTime,
                registrationEndDateTime
        );

        if (validationError != null) {
            Toast.makeText(getContext(), validationError, Toast.LENGTH_SHORT).show();
            return; // Stop if validation fails
        }

        // Convert price and capacity to Double and Integer, respectively
        Double price = priceStr.isEmpty() ? null : Double.parseDouble(priceStr);
        Integer capacity = maxAttendeesStr.isEmpty() ? null : Integer.parseInt(maxAttendeesStr);

        // Obtain the organizer name
        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                String organizerName = "Default Organizer"; // Fallback name
                if (document != null && document.exists()) {
                    // Extract organizer name if the document exists
                    organizerName = document.getString("name");
                } else {
                    Log.d("CreateEventFragment", "User document does not exist, using fallback name.");
                }

                Event event = new Event();
                event.setName(eventName);
                event.setDescription(description);
                event.setOrganizerId(userId);
                event.setOrganizerName(organizerName);
                event.setLocation(location);
                event.setPrice(price);
                event.setRegistrationStartDateTime(registrationStartDateTime);
                event.setRegistrationEndDateTime(registrationEndDateTime);
                event.setEventStartDateTime(eventStartDateTime);
                event.setEventEndDateTime(eventEndDateTime);
                event.setCapacity(capacity);
                event.setLotteryGuidelines(lotteryGuidelinesStr);
                event.setIsgeolocationRequired(isGeoLocationRequired);

                // Default values for new events
                event.setStatus("open");
                event.setWaitinglistCount(0);
                event.setAttendeeCount(0);

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
            else {
                Log.w("CreateEventFragment", "Failed to fetch user data.", task.getException());
                Toast.makeText(getContext(), "Could not retrieve user info. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Validates the user input for the event form.
     *
     * @param eventName The name of the event.
     * @param startDateText The text from the event start date field.
     * @param startTimeText The text from the event start time field.
     * @param endDateText The text from the event end date field.
     * @param endTimeText The text from the event end time field.
     * @param eventStartTimestamp The generated event start timestamp.
     * @param eventEndTimestamp The generated event end timestamp.
     * @param regStartDateText The text from the registration start date field.
     * @param regStartTimeText The text from the registration start time field.
     * @param regEndDateText The text from the registration end date field.
     * @param regEndTimeText The text from the registration end time field.
     * @param regStartTimestamp The generated registration start timestamp.
     * @param regEndTimestamp The generated registration end timestamp.
     * @return A String containing an error message if validation fails, otherwise null.
     */
    public String validateEventInput(String eventName, String startDateText, String startTimeText,
                                     String endDateText, String endTimeText,
                                     com.google.firebase.Timestamp eventStartTimestamp,
                                     com.google.firebase.Timestamp eventEndTimestamp,
                                     String regStartDateText, String regStartTimeText,
                                     String regEndDateText, String regEndTimeText,
                                     com.google.firebase.Timestamp regStartTimestamp,
                                     com.google.firebase.Timestamp regEndTimestamp) {

        if (eventName.isEmpty()) {
            return "Event name is required.";
        }

        // If a registration start date is set, an end date must also be set
        boolean isRegStartDateSet = !regStartDateText.isEmpty();
        boolean isRegEndDateSet = !regEndDateText.isEmpty();
        if (isRegStartDateSet && !isRegEndDateSet) {
            return "A registration end date is required if a start date is set.";
        }

        // All dates require a time or the Firestore timestamp will be null
        boolean isEventStartDateSet = !startDateText.isEmpty();
        boolean isEventStartTimeSet = !startTimeText.isEmpty();
        if (isEventStartDateSet && !isEventStartTimeSet) {
            return "Please select a start time for the selected event start date.";
        }

        boolean isEventEndDateSet = !endDateText.isEmpty();
        boolean isEventEndTimeSet = !endTimeText.isEmpty();
        if (isEventEndDateSet && !isEventEndTimeSet) {
            return "Please select an end time for the selected event end date.";
        }

        boolean isRegStartTimeSet = !regStartTimeText.isEmpty();
        if (isRegStartDateSet && !isRegStartTimeSet) {
            return "Please select a start time for the selected registration start date.";
        }

        boolean isRegEndTimeSet = !regEndTimeText.isEmpty();
        if (isRegEndDateSet && !isRegEndTimeSet) {
            return "Please select an end time for the selected registration end date.";
        }

        // Start before end checks:
        if (eventStartTimestamp != null && eventEndTimestamp != null && eventStartTimestamp.compareTo(eventEndTimestamp) >= 0) {
            return "Event start time must be before event end time.";
        }

        if (regStartTimestamp != null && regEndTimestamp != null && regStartTimestamp.compareTo(regEndTimestamp) >= 0) {
            return "Registration start time must be before registration end time.";
        }

        // Check that registration period is before the event starts
        if (regEndTimestamp != null && eventStartTimestamp != null && regEndTimestamp.compareTo(eventStartTimestamp) > 0) {
            return "Registration must end before the event starts.";
        }

        // Date/time is in the future checks:
        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

        if (regStartTimestamp != null && regStartTimestamp.compareTo(now) < 0) {
            return "Registration start time cannot be in the past.";
        }

        if (eventStartTimestamp != null && eventStartTimestamp.compareTo(now) < 0) {
            return "Event start time cannot be in the past.";
        }

        return null; // Validation successful
    }

    /**
     * Creates a Firestore {@link com.google.firebase.Timestamp} from a Calendar object.
     * Returns null if the date or time fields have not been filled out by the user.
     * @param calendar The Calendar instance holding the selected date and time.
     * @param dateText The EditText for the date.
     * @param timeText The EditText for the time.
     * @return A Firestore Timestamp object, or null if the date/time is incomplete.
     */
    private com.google.firebase.Timestamp getTimestampFromCalendars(Calendar calendar, EditText dateText, EditText timeText) {
        if (dateText.getText().toString().isEmpty() || timeText.getText().toString().isEmpty()) {
            return null; // Return null if either date or time hasn't been picked
        }
        return new com.google.firebase.Timestamp(calendar.getTime());
    }
}

