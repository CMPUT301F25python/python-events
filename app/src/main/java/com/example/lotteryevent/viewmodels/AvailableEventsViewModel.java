package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.IAvailableEventsRepository;
import com.example.lotteryevent.ui.AvailableEventsFragment;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * ViewModel for the Available Events screen, responsible for preparing and managing the data for the UI.
 * <p>
 * This ViewModel provides the {@link AvailableEventsFragment} with the necessary data,
 * such as a list of available events. It acts as an intermediary between the UI and the data layer
 * (represented by {@link IAvailableEventsRepository}).
 * <p>
 * By using a ViewModel, the UI data survives configuration changes (like screen rotations).
 * This class is decoupled from the specific data source implementation, communicating only with the
 * {@link IAvailableEventsRepository} interface. This design follows the principles of dependency injection,
 * making the ViewModel more testable and maintainable.
 */
public class AvailableEventsViewModel extends ViewModel {

    private final IAvailableEventsRepository availableEventsRepository;

    // Raw events from the repository
    private final LiveData<List<Event>> events;

    // Filtered events exposed to the UI
    private final MediatorLiveData<List<Event>> filteredEvents = new MediatorLiveData<>();

    // Current filter state (set by the Fragment)
    private String currentKeyword = "";
    private boolean filterAvailableToday = false;

    /**
     * Constructs an AvailableEventsViewModel. The repository is "injected" through the constructor,
     * which allows for better testing and follows dependency inversion principles.
     *
     * @param availableEventsRepository An implementation of IAvailableEventsRepository that this ViewModel will use
     *                                  to interact with the data layer.
     */
    public AvailableEventsViewModel(IAvailableEventsRepository availableEventsRepository) {
        this.availableEventsRepository = availableEventsRepository;
        this.events = availableEventsRepository.getAvailableEvents();

        // Re-apply filters whenever the underlying events list changes.
        filteredEvents.addSource(events, list -> applyFilters());
    }

    /**
     * Exposes the raw LiveData of the list of available events from the repository.
     * (Still available if other UI components need it.)
     *
     * @return A LiveData object containing a list of {@link Event}s.
     */
    public LiveData<List<Event>> getEvents() {
        return events;
    }

    /**
     * Exposes the filtered list of events for the UI to observe.
     *
     * @return A LiveData object containing a list of filtered {@link Event}s.
     */
    public LiveData<List<Event>> getFilteredEvents() {
        return filteredEvents;
    }

    /**
     * Updates the keyword filter used to filter events by name/description.
     * Called from the Fragment when the user changes the keyword.
     */
    public void setKeywordFilter(String keyword) {
        this.currentKeyword = (keyword == null) ? "" : keyword.trim();
        applyFilters();
    }

    /**
     * Updates whether the "Available Today" filter is enabled.
     * Called from the Fragment when the user toggles the button.
     */
    public void setFilterAvailableToday(boolean filterAvailableToday) {
        this.filterAvailableToday = filterAvailableToday;
        applyFilters();
    }

    /**
     * Exposes the loading state from the repository.
     * The UI can observe this to show or hide loading indicators.
     *
     * @return A LiveData object containing a boolean that is true if data is loading, false otherwise.
     */
    public LiveData<Boolean> isLoading() {
        return availableEventsRepository.isLoading();
    }

    /**
     * Exposes messages from the repository.
     * The UI can observe this to display error/success messages to the user.
     *
     * @return A LiveData object containing a message string.
     */
    public LiveData<String> getMessage() {
        return availableEventsRepository.getMessage();
    }

    /**
     * Called by the UI to request a refresh of the event data.
     * This call is simply delegated to the repository.
     */
    public void fetchAvailableEvents() {
        availableEventsRepository.fetchAvailableEvents();
    }

    /**
     * Applies the current filters (keyword + "available today") to the
     * underlying list of events and updates {@link #filteredEvents}.
     */
    private void applyFilters() {
        List<Event> source = events.getValue();

        if (source == null) {
            filteredEvents.setValue(Collections.emptyList());
            return;
        }

        String keywordLower = (currentKeyword == null ? "" : currentKeyword.trim().toLowerCase());
        boolean filterByKeyword = !keywordLower.isEmpty();

        Calendar today = Calendar.getInstance();
        int todayYear = today.get(Calendar.YEAR);
        int todayDayOfYear = today.get(Calendar.DAY_OF_YEAR);

        List<Event> result = new ArrayList<>();

        for (Event event : source) {

            // Keyword / interest filter
            if (filterByKeyword) {
                String nameLower = event.getName() == null ? "" : event.getName().toLowerCase();
                String descLower = event.getDescription() == null ? "" : event.getDescription().toLowerCase();

                if (!nameLower.contains(keywordLower) && !descLower.contains(keywordLower)) {
                    continue;
                }
            }

            // "Available today" filter
            if (filterAvailableToday) {
                Timestamp startTs = event.getEventStartDateTime();
                if (startTs == null) {
                    // No start date, cannot be "available today"
                    continue;
                }

                Calendar eventCal = Calendar.getInstance();
                eventCal.setTime(startTs.toDate());

                int eventYear = eventCal.get(Calendar.YEAR);
                int eventDayOfYear = eventCal.get(Calendar.DAY_OF_YEAR);

                if (eventYear != todayYear || eventDayOfYear != todayDayOfYear) {
                    // Event is not today
                    continue;
                }
            }

            result.add(event);
        }

        filteredEvents.setValue(result);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        availableEventsRepository.removeListener();
    }
}
