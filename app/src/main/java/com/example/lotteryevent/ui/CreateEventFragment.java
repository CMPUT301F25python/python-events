package com.example.lotteryevent.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.example.lotteryevent.R;
import com.example.lotteryevent.repository.EventRepositoryImpl;
import com.example.lotteryevent.repository.IEventRepository;
import com.example.lotteryevent.viewmodels.CreateEventViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
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

    // --- UI Components ---
    private EditText editTextEventName, editTextEventDescription, editTextEventLocation,
            editTextEventStartDate, editTextEventStartTime, editTextEventEndDate,
            editTextEventEndTime, editTextRegistrationStartDate, editTextRegistrationStartTime,
            editTextRegistrationEndDate, editTextRegistrationEndTime, editTextEventPrice,
            editTextMaxAttendees, editTextWaitingListLimit;
    private CheckBox checkboxGeolocation;
    private Button buttonSave;

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
        // Observe loading state to enable/disable the save button.
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            buttonSave.setEnabled(!isLoading);
        });

        // Observe messages to show success/error toasts and handle navigation.
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
                eventStartCalendar, eventEndCalendar, registrationStartCalendar, registrationEndCalendar,
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
    }

    /**
     * Sets up OnClickListeners for all interactive UI elements in the fragment,
     * such as date/time fields and the save button.
     */
    private void setupClickListeners() {
        // Set up the picker dialogs for each date and time field
        editTextEventStartDate.setOnClickListener(v -> showDatePickerDialog(eventStartCalendar, editTextEventStartDate));
        editTextEventStartTime.setOnClickListener(v -> showTimePickerDialog(eventStartCalendar, editTextEventStartTime));

        editTextEventEndDate.setOnClickListener(v -> showDatePickerDialog(eventEndCalendar, editTextEventEndDate));
        editTextEventEndTime.setOnClickListener(v -> showTimePickerDialog(eventEndCalendar, editTextEventEndTime));

        editTextRegistrationStartDate.setOnClickListener(v -> showDatePickerDialog(registrationStartCalendar, editTextRegistrationStartDate));
        editTextRegistrationStartTime.setOnClickListener(v -> showTimePickerDialog(registrationStartCalendar, editTextRegistrationStartTime));

        editTextRegistrationEndDate.setOnClickListener(v -> showDatePickerDialog(registrationEndCalendar, editTextRegistrationEndDate));
        editTextRegistrationEndTime.setOnClickListener(v -> showTimePickerDialog(registrationEndCalendar, editTextRegistrationEndTime));

        // Set a click listener on the save button to trigger the ViewModel action
        buttonSave.setOnClickListener(v -> handleSaveEvent());
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

        DatePickerDialog dialog = new DatePickerDialog(requireContext(), dateSetListener, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // Add a "Clear" button
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
        TimePickerDialog.OnTimeSetListener timeSetListener = (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            updateLabel(editText, calendar, "HH:mm");
        };

        TimePickerDialog dialog = new TimePickerDialog(requireContext(), timeSetListener, calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), true);

        // Add "Clear" button to the dialog
        dialog.setButton(TimePickerDialog.BUTTON_NEUTRAL, "Clear", (d, which) -> {
            editText.setText(""); // Clear the visible text in the EditText
        });

        dialog.show();
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
}