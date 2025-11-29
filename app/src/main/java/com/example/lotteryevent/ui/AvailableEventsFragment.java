package com.example.lotteryevent.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextThemeWrapper;
import android.widget.Toast;
import android.app.DatePickerDialog;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;

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
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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

    /**
     * Callback interface for selecting a date range.
     */
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

    /**
     * Converts a date to the start of the day in milliseconds.
     * @param year
     * @param monthZeroBased
     * @param dayOfMonth
     * @return The start of the day in milliseconds for the given date.
     */
    private long startOfDayMillis(int year, int monthZeroBased, int dayOfMonth) {
        Calendar c = Calendar.getInstance();
        c.set(year, monthZeroBased, dayOfMonth, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /**
     * Converts a date to the end of the day in milliseconds.
     * @param year
     * @param monthZeroBased
     * @param dayOfMonth
     * @return The end of the day in milliseconds for the given date.
     */
    private long endOfDayMillis(int year, int monthZeroBased, int dayOfMonth) {
        Calendar c = Calendar.getInstance();
        c.set(year, monthZeroBased, dayOfMonth, 23, 59, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    /**
     * Formats a date as a string.
     * @param ms
     * @return The formatted date string.
     */
    private String formatDay(@Nullable Long ms) {
        if (ms == null) return "";   // important: let the hint show
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        return df.format(new Date(ms));
    }

    /**
     * Converts a dp value to a pixel value.
     * @param v
     * @return The pixel value.
     */
    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()
        );
    }

    /**
     * Opens a dialog that lets the user enter a keyword to filter events by name or description.
     */
    private void showFilterDialog() {
        // Use a Material themed context so TextInputLayout renders correctly
        ContextThemeWrapper themed = new ContextThemeWrapper(
                requireContext(),
                com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog
        );

        LinearLayout root = new LinearLayout(themed);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(16), dp(24), dp(8));

        // ---- Keyword outlined field with search icon ----
        TextInputLayout keywordLayout = new TextInputLayout(themed);
        keywordLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        keywordLayout.setHint("Keyword");
        keywordLayout.setStartIconDrawable(android.R.drawable.ic_menu_search); // swap if you have your own search icon
        keywordLayout.setStartIconVisible(true);

        TextInputEditText keywordInput = new TextInputEditText(themed);
        keywordInput.setSingleLine(true);
        keywordInput.setText(currentKeyword);
        keywordLayout.addView(keywordInput);

        root.addView(keywordLayout, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Dae Range label
        TextView label = new TextView(themed);
        label.setText("Date Range");
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setPadding(0, dp(14), 0, 0);

        int onSurfaceVariant = MaterialColors.getColor(root, com.google.android.material.R.attr.colorOnSurfaceVariant);
        label.setTextColor(onSurfaceVariant);

        root.addView(label);

        // Date range row
        LinearLayout row = new LinearLayout(themed);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, 0);

        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        leftLp.setMarginEnd(dp(12));
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);

        TextInputLayout startLayout = new TextInputLayout(themed);
        startLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        startLayout.setHint("Start Date");
        startLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        startLayout.setEndIconDrawable(R.drawable.calendar_today_24px);

        TextInputEditText startInput = new TextInputEditText(themed);
        startInput.setFocusable(false);
        startInput.setClickable(true);
        startInput.setCursorVisible(false);

        TextInputLayout endLayout = new TextInputLayout(themed);
        endLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        endLayout.setHint("End Date");
        endLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        endLayout.setEndIconDrawable(R.drawable.calendar_today_24px);

        TextInputEditText endInput = new TextInputEditText(themed);
        endInput.setFocusable(false);
        endInput.setClickable(true);
        endInput.setCursorVisible(false);

        startLayout.addView(startInput);
        endLayout.addView(endInput);

        row.addView(startLayout, leftLp);
        row.addView(endLayout, rightLp);
        root.addView(row);

        final Long[] tempStart = new Long[]{filterStartDateMs};
        final Long[] tempEnd = new Long[]{filterEndDateMs};

        startInput.setText(formatDay(tempStart[0]));
        endInput.setText(formatDay(tempEnd[0]));

        Runnable openStart = () -> showDayPickerDialog(
                tempStart[0], null, null,
                (s, e) -> {
                    if (tempEnd[0] != null && s > tempEnd[0]) {
                        Toast.makeText(requireContext(), "Start date can’t be after the end date.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    tempStart[0] = s;
                    startInput.setText(formatDay(s));
                }
        );

        Runnable openEnd = () -> showDayPickerDialog(
                tempEnd[0], tempStart[0], null,
                (s, e) -> {
                    tempEnd[0] = e; // inclusive end-of-day
                    endInput.setText(formatDay(e));
                }
        );

        startInput.setOnClickListener(v -> openStart.run());
        startLayout.setEndIconOnClickListener(v -> openStart.run());

        endInput.setOnClickListener(v -> openEnd.run());
        endLayout.setEndIconOnClickListener(v -> openEnd.run());

        // Title
        TextView titleView = new TextView(themed);
        titleView.setText("Filter Events");
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        titleView.setPadding(dp(24), dp(18), dp(24), 0);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(themed)
                .setCustomTitle(titleView)
                .setView(root)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear", (d, which) -> {
                    currentKeyword = "";
                    filterStartDateMs = null;
                    filterEndDateMs = null;
                    applyFiltersAndUpdateList();
                    applyLocalFiltersToAdapter();
                })
                .setPositiveButton("Apply", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                currentKeyword = (keywordInput.getText() == null) ? "" : keywordInput.getText().toString();

                Long start = tempStart[0];
                Long end = tempEnd[0];

                if (start != null && end != null && end < start) {
                    endLayout.setError("End date must be on or after the start date.");
                    return;
                }
                endLayout.setError(null);

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
