package com.example.lotteryevent.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.app.DatePickerDialog;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.res.ColorStateList;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lotteryevent.R;
import com.example.lotteryevent.adapters.EventAdapter;
import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.AvailableEventsRepositoryImpl;
import com.example.lotteryevent.repository.IAvailableEventsRepository;
import com.example.lotteryevent.viewmodels.AvailableEventsViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * A {@link Fragment} subclass that allows entrants to view a list of available events.
 * It is responsible for displaying all events to the user.
 */
public class AvailableEventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private AvailableEventsViewModel availableEventsViewModel;
    private ViewModelProvider.Factory viewModelFactory;

    // UI-level filter state (the actual filtering is done in the ViewModel)
    private String currentKeyword = "";
    private boolean filterAvailableToday = false;

    @Nullable private Long filterStartDateMs = null;
    @Nullable private Long filterEndDateMs = null;
    @NonNull private List<Event> lastEventsFromViewModel = new ArrayList<>();
    @Nullable private ColorStateList availableTodayDefaultBgTint;
    @Nullable private ColorStateList availableTodayDefaultTextColors;
    @Nullable private ColorStateList availableTodayDefaultStrokeColor;
    private int availableTodayDefaultStrokeWidth;

    public AvailableEventsFragment() { }

    public AvailableEventsFragment(ViewModelProvider.Factory viewModelFactory) {
        this.viewModelFactory = viewModelFactory;
    }

    /**
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to. The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (viewModelFactory == null) {
            GenericViewModelFactory factory = new GenericViewModelFactory();
            IAvailableEventsRepository availableEventsRepository = new AvailableEventsRepositoryImpl();
            factory.put(AvailableEventsViewModel.class, () -> new AvailableEventsViewModel(availableEventsRepository));
            viewModelFactory = factory;
        }

        // Use the factory (either from production or from the test) to create the ViewModel.
        availableEventsViewModel = new ViewModelProvider(this, viewModelFactory).get(AvailableEventsViewModel.class);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_available_events, container, false);
    }

    /**
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView(view);
        setupObservers();
        setupButtons(view);

        // Trigger initial load
        availableEventsViewModel.fetchAvailableEvents();

        // Ensure initial filter state is applied (keyword = "", availableToday = false)
        applyFiltersAndUpdateList();
    }

    /**
     * Sets up the {@link RecyclerView} used to display the list of
     * available events. This method:
     * - Finds the RecyclerView in the layout
     * - Attaches a {@link LinearLayoutManager} for vertical scrolling
     * - Creates an {@link EventAdapter} with the event item layout
     * - Registers a click listener that navigates to the event
     *   details screen when an event is selected
     *
     * @param view the root view of this fragment used to locate the RecyclerView
     */
    public void setupRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.events_recycler_view);

        // use tile layout
        adapter = new EventAdapter(R.layout.tile_event);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(event -> {
            String id = event.getEventId();
            if (id == null || id.trim().isEmpty() || "null".equals(id)) {
                Toast.makeText(requireContext(), "Missing/invalid event id", Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle args = new Bundle();
            args.putString("eventId", id);

            androidx.navigation.NavController nav =
                    androidx.navigation.fragment.NavHostFragment.findNavController(AvailableEventsFragment.this);
            nav.navigate(R.id.eventDetailsFragment, args);
        });
    }

    /**
     * Subscribes to LiveData from {@link AvailableEventsViewModel} so that the UI updates
     * automatically when data changes.
     * <p>
     * This method:
     * - Observes the filtered list of available events and sends it directly to the adapter
     * - Observes user-facing messages and shows them using a {@link Toast}
     */
    public void setupObservers() {
        availableEventsViewModel.getFilteredEvents().observe(getViewLifecycleOwner(), events -> {
            // Keep the latest list from the ViewModel and apply the date filter locally.
            lastEventsFromViewModel = (events == null) ? new ArrayList<>() : events;
            applyLocalFiltersToAdapter();
        });

        availableEventsViewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Sets up click listeners for the "Available Today" button and the keyword filter button.
     * - The "Available Today" button toggles a flag and updates the ViewModel's filter state
     *   so that only events starting today are shown when it is enabled.
     * - The filter button opens a dialog that lets the user enter a keyword to filter events by
     *   name or description.
     *
     * @param view the root view of this fragment used to locate the buttons
     */
    public void setupButtons(View view) {
        MaterialButton availableTodayButton = view.findViewById(R.id.available_today_button);
        View filterButton = view.findViewById(R.id.filter_button);

        // Set initial style
        updateAvailableTodayButtonStyle(availableTodayButton);

        // Toggle "available today" filter + update UI
        availableTodayButton.setOnClickListener(v -> {
            filterAvailableToday = !filterAvailableToday;
            updateAvailableTodayButtonStyle(availableTodayButton);
            applyFiltersAndUpdateList();
        });

        filterButton.setOnClickListener(v -> showFilterDialog());
    }

    /**
     * Notifies the ViewModel of the current filter state (keyword and "available today" flag).
     * <p>
     * All filtering logic is implemented in {@link AvailableEventsViewModel}. This method only
     * forwards the user's filter choices to the ViewModel.
     */
    private void applyFiltersAndUpdateList() {
        availableEventsViewModel.setKeywordFilter(currentKeyword);
        availableEventsViewModel.setFilterAvailableToday(filterAvailableToday);
    }

    /**
     * Applies the currently selected date range filter to {@link #lastEventsFromViewModel}
     * and pushes the result into the adapter.
     */
    private void applyLocalFiltersToAdapter() {
        if (adapter == null) return;
        adapter.setEvents(applyDateRangeFilter(lastEventsFromViewModel));
    }

    /**
     * Filters events by {@link Event#getEventStartDateTime()} using the inclusive day range
     * [filterStartDateMs, filterEndDateMs]. If either bound is null, it is treated as "unbounded".
     */
    @NonNull
    private List<Event> applyDateRangeFilter(@NonNull List<Event> input) {
        if (filterStartDateMs == null && filterEndDateMs == null) return input;

        List<Event> out = new ArrayList<>();
        for (Event e : input) {
            if (e == null || e.getEventStartDateTime() == null) continue;

            long t = e.getEventStartDateTime().toDate().getTime();
            if (filterStartDateMs != null && t < filterStartDateMs) continue;
            if (filterEndDateMs != null && t > filterEndDateMs) continue;

            out.add(e);
        }
        return out;
    }

    private interface DaySelectionCallback {
        void onDaySelected(long startOfDayMs, long endOfDayMs);
    }

    /**
     * Opens a dialog that lets the user select a date.
     */
    private void showDayPickerDialog(
            @Nullable Long existingMs,
            @Nullable Long minDateMs,
            @Nullable Long maxDateMs,
            @NonNull DaySelectionCallback cb
    ) {
        final Calendar cal = Calendar.getInstance();
        if (existingMs != null) cal.setTimeInMillis(existingMs);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(requireContext(), (view, y, m, d) -> {
            cb.onDaySelected(startOfDayMillis(y, m, d), endOfDayMillis(y, m, d));
        }, year, month, day);

        if (minDateMs != null) dlg.getDatePicker().setMinDate(minDateMs);
        if (maxDateMs != null) dlg.getDatePicker().setMaxDate(maxDateMs);

        dlg.show();
    }

    private long startOfDayMillis(int year, int monthZeroBased, int dayOfMonth) {
        Calendar c = Calendar.getInstance();
        c.set(year, monthZeroBased, dayOfMonth, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long endOfDayMillis(int year, int monthZeroBased, int dayOfMonth) {
        Calendar c = Calendar.getInstance();
        c.set(year, monthZeroBased, dayOfMonth, 23, 59, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    private String formatDay(@Nullable Long ms) {
        if (ms == null) return "Click to Select Date";
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        return df.format(new Date(ms));
    }

    /**
     * Opens a dialog that lets the user enter a keyword to filter events by name or description.
     */
    private void showFilterDialog() {
        final int pad = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics()
        );

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, pad, pad, pad);

        // Keyword
        TextView keywordHeader = new TextView(requireContext());
        keywordHeader.setText("Filter by Keyword");
        keywordHeader.setPadding(10, pad, 0, 0);
        container.addView(keywordHeader);

        EditText keywordInput = new EditText(requireContext());
        keywordInput.setHint("Enter a keyword");
        keywordInput.setText(currentKeyword);
        container.addView(keywordInput);

        // Date range
        TextView dateHeader = new TextView(requireContext());
        dateHeader.setText("Filter by Date");
        dateHeader.setPadding(10, pad, 0, 0);
        container.addView(dateHeader);

        final Long[] tempStart = new Long[]{filterStartDateMs};
        final Long[] tempEnd = new Long[]{filterEndDateMs};

        TextView from = new TextView(requireContext());
        from.setText("From: " + formatDay(tempStart[0]));
        from.setPadding(10, pad / 2, 0, 0);
        from.setOnClickListener(v ->
                showDayPickerDialog(
                        tempStart[0],
                        null,
                        null,
                        (s, e) -> {
                            // invalid if user picks a start date after the current end date
                            if (tempEnd[0] != null && s > tempEnd[0]) {
                                Toast.makeText(
                                        requireContext(),
                                        "Start date can’t be after the end date.",
                                        Toast.LENGTH_SHORT
                                ).show();
                                return; // reject selection
                            }

                            tempStart[0] = s;
                            from.setText("From: " + formatDay(tempStart[0]));
                        }
                )
        );

        container.addView(from);

        TextView to = new TextView(requireContext());
        to.setText("To: " + formatDay(tempEnd[0]));
        to.setPadding(10, pad / 2, 0, 0);
        to.setOnClickListener(v ->
                showDayPickerDialog(
                        tempEnd[0],
                        tempStart[0], // if "From" is set, To cannot be before it
                        null,
                        (s, e) -> {
                            tempEnd[0] = e; // end-of-day (inclusive)
                            to.setText("To: " + formatDay(tempEnd[0]));
                        }
                )
        );
        container.addView(to);

        final androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Filter Events")
                    .setView(container)
                    .setPositiveButton("Apply", null)
                    .setNegativeButton("Clear", (d, which) -> {
                        currentKeyword = "";
                        filterStartDateMs = null;
                        filterEndDateMs = null;
                        applyFiltersAndUpdateList();
                        applyLocalFiltersToAdapter();
                    })
                    .setNeutralButton("Cancel", null)
                    .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                currentKeyword = keywordInput.getText().toString();

                Long start = tempStart[0];
                Long end = tempEnd[0];

                // Until cannot be earlier than From
                if (start != null && end != null && end < start) {
                    keywordInput.setError("End date must be on or after the start date.");
                    return;
                }

                filterStartDateMs = start;
                filterEndDateMs = end;

                applyFiltersAndUpdateList();
                applyLocalFiltersToAdapter();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    /**
     * Updates the style of the "Available Today" button.
     * @param button
     */
    private void updateAvailableTodayButtonStyle(@NonNull MaterialButton button) {

        if (availableTodayDefaultTextColors == null) {
            availableTodayDefaultBgTint = button.getBackgroundTintList();
            availableTodayDefaultTextColors = button.getTextColors();
            availableTodayDefaultStrokeColor = button.getStrokeColor();
            availableTodayDefaultStrokeWidth = button.getStrokeWidth();
        }

        if (filterAvailableToday) {
            int pink = Color.parseColor("#F981A5"); // changes button colour to primary pink
            button.setBackgroundTintList(ColorStateList.valueOf(pink));
            button.setTextColor(Color.WHITE);
            button.setStrokeWidth(0);
        } else {
            button.setBackgroundTintList(availableTodayDefaultBgTint);
            if (availableTodayDefaultTextColors != null) button.setTextColor(availableTodayDefaultTextColors);

            button.setStrokeWidth(availableTodayDefaultStrokeWidth);
            button.setStrokeColor(availableTodayDefaultStrokeColor);
        }
    }

}
