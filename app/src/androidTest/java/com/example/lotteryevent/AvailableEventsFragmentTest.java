package com.example.lotteryevent;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.widget.DatePicker;
import androidx.test.espresso.contrib.PickerActions;

import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.FakeAvailableEventsRepository;
import com.example.lotteryevent.ui.AvailableEventsFragment;
import com.example.lotteryevent.viewmodels.AvailableEventsViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.Date;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

/***
 * Unit tests for {@link AvailableEventsFragment}.
 */
@RunWith(AndroidJUnit4.class)
public class AvailableEventsFragmentTest {

    private FakeAvailableEventsRepository fakeRepository;
    private ReusableTestFragmentFactory fragmentFactory;
    private TestNavHostController navController;

    @Before
    public void setup() {
        fakeRepository = new FakeAvailableEventsRepository();

        // Wire ViewModel to fake repo
        GenericViewModelFactory viewModelFactory = new GenericViewModelFactory();
        viewModelFactory.put(
                AvailableEventsViewModel.class,
                () -> new AvailableEventsViewModel(fakeRepository)
        );

        // Use your reusable fragment factory
        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(
                AvailableEventsFragment.class,
                () -> new AvailableEventsFragment(viewModelFactory)
        );
    }

    /**
     * Helper to launch the fragment and attach a TestNavHostController, same pattern as your
     * UserProfileFragmentTest.
     */
    private FragmentScenario<AvailableEventsFragment> launchFragment() {
        navController = new TestNavHostController(
                ApplicationProvider.getApplicationContext()
        );

        FragmentScenario<AvailableEventsFragment> scenario =
                FragmentScenario.launchInContainer(
                        AvailableEventsFragment.class,
                        null,
                        R.style.Theme_LotteryEvent,
                        fragmentFactory
                );

        scenario.onFragment(fragment -> {
            navController.setGraph(R.navigation.nav_graph);
            navController.setCurrentDestination(R.id.availableEventsFragment);
            Navigation.setViewNavController(fragment.requireView(), navController);
        });

        return scenario;
    }

    private static Matcher<View> withIndex(final Matcher<View> matcher, final int index) {
        return new TypeSafeMatcher<View>() {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description) {
                description.appendText("with index: " + index + " ");
                matcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(View view) {
                if (!matcher.matches(view)) return false;
                return currentIndex++ == index;
            }
        };
    }

    /**
     * Verifies that when the fragment's view is created, it triggers a fetch of available events
     * through the repository.
     * This ensures that the ViewModel correctly calls the repository's fetch method as part of the
     * fragment's initialization.
     */
    @Test
    public void onViewCreated_callsFetchAvailableEventsOnRepository() {
        launchFragment();
        assertTrue(fakeRepository.wasFetchCalled());
    }

    /**
     * This test verifies that events returned by the repository are displayed in the RecyclerView.
     */
    @Test
    public void eventsFromRepository_areShownInRecyclerView() {
        // Arrange fake events
        List<Event> events = new ArrayList<>();
        Event e1 = new Event();
        e1.setEventId("id1");
        e1.setName("Event One");
        events.add(e1);

        Event e2 = new Event();
        e2.setEventId("id2");
        e2.setName("Event Two");
        events.add(e2);

        fakeRepository.setEventsToReturn(events);

        FragmentScenario<AvailableEventsFragment> scenario = launchFragment();

        // Assert adapter count
        scenario.onFragment(fragment -> {
            RecyclerView rv = fragment.requireView().findViewById(R.id.events_recycler_view);
            assertNotNull(rv.getAdapter());
            assertEquals(2, rv.getAdapter().getItemCount());
        });

        // If your item layout shows the event name, you can assert via Espresso:
        onView(withText("Event One")).check(matches(isDisplayed()));
        onView(withText("Event Two")).check(matches(isDisplayed()));
    }

    /**
     * This test verifies that when a repository returns an empty list of events, the RecyclerView
     * is empty.
     */
    @Test
    public void emptyEventsList_recyclerViewHasNoItems() {
        fakeRepository.setEventsToReturn(new ArrayList<>());

        FragmentScenario<AvailableEventsFragment> scenario = launchFragment();

        scenario.onFragment(fragment -> {
            RecyclerView rv = fragment.requireView().findViewById(R.id.events_recycler_view);
            assertNotNull(rv.getAdapter());
            assertEquals(0, rv.getAdapter().getItemCount());
        });
    }

    /**
     * This test verifies that when the repository reports an error, the RecyclerView displays an
     * empty list of events.
     */
    @Test
    public void repositoryError_resultsInEmptyRecyclerView() {
        String error = "Failed to load events. Please check your connection.";
        fakeRepository.setError(error);

        FragmentScenario<AvailableEventsFragment> scenario = launchFragment();

        scenario.onFragment(fragment -> {
            RecyclerView rv = fragment.requireView().findViewById(R.id.events_recycler_view);
            assertNotNull(rv.getAdapter());
            // Error path in FakeAvailableEventsRepository emits an empty list
            assertEquals(0, rv.getAdapter().getItemCount());
        });
    }

    /**
     * This test verifies that when an event is clicked in the RecyclerView, it navigates to the
     * event details screen.
     */
    @Test
    public void clickValidEvent_navigatesToEventDetails_withEventIdArgument() {
        List<Event> events = new ArrayList<>();
        Event e = new Event();
        e.setEventId("abc123");
        e.setName("My Event");
        events.add(e);

        fakeRepository.setEventsToReturn(events);

        launchFragment();

        // Click first item in RecyclerView
        onView(withId(R.id.events_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Assert navigation
        assertEquals(
                R.id.eventDetailsFragment,
                navController.getCurrentDestination().getId()
        );

        Bundle args = navController
                .getBackStackEntry(R.id.eventDetailsFragment)
                .getArguments();
        assertNotNull(args);
        assertEquals("abc123", args.getString("eventId"));
    }

    /**
     * This test verifies that when an event is clicked in the RecyclerView, it does not navigates
     * away from the AvailableEventsFragment.
     */
    @Test
    public void clickEventWithInvalidId_doesNotNavigate() {
        List<Event> events = new ArrayList<>();
        Event e = new Event();
        // invalid ID: null / blank / "null"
        e.setEventId(null);
        e.setName("Broken Event");
        events.add(e);

        fakeRepository.setEventsToReturn(events);

        launchFragment();

        onView(withId(R.id.events_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Still on AvailableEventsFragment (no navigation)
        assertEquals(
                R.id.availableEventsFragment,
                navController.getCurrentDestination().getId()
        );
    }

    /**
     * This test verifies that when the "Available Today" button is clicked, only events that are
     * currently available to register (open, registration window active, event not started, and
     * waiting list not at capacity) are shown in the RecyclerView.
     */
    @Test
    public void availableTodayButton_filtersEventsToCurrentlyAvailableEvents() {
        List<Event> events = new ArrayList<>();

        Date now = new Date();
        Calendar calendar = Calendar.getInstance();

        // Registration start: yesterday
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        Date regStartDate = calendar.getTime();
        Timestamp regStartTimestamp = new Timestamp(regStartDate);

        // Registration end: tomorrow
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date regEndDate = calendar.getTime();
        Timestamp regEndTimestamp = new Timestamp(regEndDate);

        // Event start: tomorrow
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date eventStartDate = calendar.getTime();
        Timestamp eventStartTimestamp = new Timestamp(eventStartDate);

        // Event that IS currently available
        Event availableEvent = new Event();
        availableEvent.setEventId("available-id");
        availableEvent.setName("Available Event");
        availableEvent.setStatus("open");
        availableEvent.setRegistrationStartDateTime(regStartTimestamp);
        availableEvent.setRegistrationEndDateTime(regEndTimestamp);
        availableEvent.setEventStartDateTime(eventStartTimestamp);
        availableEvent.setWaitingListLimit(10);
        events.add(availableEvent);

        // Event that is NOT currently available: registration has already ended
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_YEAR, -2);
        Date pastRegEndDate = calendar.getTime();
        Timestamp pastRegEndTimestamp = new Timestamp(pastRegEndDate);

        Event closedRegistrationEvent = new Event();
        closedRegistrationEvent.setEventId("closed-reg-id");
        closedRegistrationEvent.setName("Closed Registration Event");
        closedRegistrationEvent.setStatus("open");
        closedRegistrationEvent.setRegistrationStartDateTime(regStartTimestamp);
        closedRegistrationEvent.setRegistrationEndDateTime(pastRegEndTimestamp);
        closedRegistrationEvent.setEventStartDateTime(eventStartTimestamp);
        closedRegistrationEvent.setWaitingListLimit(10);
        events.add(closedRegistrationEvent);

        fakeRepository.setEventsToReturn(events);

        FragmentScenario<AvailableEventsFragment> scenario = launchFragment();

        // Both events should be visible before applying the filter
        onView(withText("Available Event")).check(matches(isDisplayed()));
        onView(withText("Closed Registration Event")).check(matches(isDisplayed()));

        // Click "Available Today" button to enable the availability filter
        onView(withId(R.id.available_today_button)).perform(click());

        // Assert: only the currently available event remains visible
        onView(withText("Available Event")).check(matches(isDisplayed()));
        onView(withText("Closed Registration Event")).check(doesNotExist());
    }

    /**
     * This test verifies that when the "Filter" button is clicked, the keyword filter dialog is
     * shown.
     */
    @Test
    public void filterButton_filtersEventsByKeywordInNameOrDescription() {
        List<Event> events = new ArrayList<>();

        Event gamesEvent = new Event();
        gamesEvent.setEventId("games-id");
        gamesEvent.setName("Board Games Night");
        gamesEvent.setDescription("An evening of board games.");
        events.add(gamesEvent);

        Event cookingEvent = new Event();
        cookingEvent.setEventId("cook-id");
        cookingEvent.setName("Cooking Workshop");
        cookingEvent.setDescription("Learn to cook with friends.");
        events.add(cookingEvent);

        fakeRepository.setEventsToReturn(events);

        FragmentScenario<AvailableEventsFragment> scenario = launchFragment();

        // Both events visible before filtering
        onView(withText("Board Games Night")).check(matches(isDisplayed()));
        onView(withText("Cooking Workshop")).check(matches(isDisplayed()));

        // Open the keyword filter dialog via the filter button
        onView(withId(R.id.filter_button)).perform(click());

        // Keyword field is the only focusable TextInputEditText in the dialog
        onView(allOf(
            isAssignableFrom(com.google.android.material.textfield.TextInputEditText.class),
            androidx.test.espresso.matcher.ViewMatchers.isFocusable(),
            isDisplayed()
            )).perform(replaceText("game"));

        // Apply the filter
        onView(withText("Apply")).perform(click());

        // Assert: matching event is shown, non-matching event is filtered out
        onView(withText("Board Games Night")).check(matches(isDisplayed()));
        onView(withText("Cooking Workshop")).check(doesNotExist());
    }

    /**
     * This test verifies that when the "Filter" button is clicked, the date range filter dialog is
     * shown.
     */
    @Test
    public void filterButton_filtersEventsByDateRange_onEventStartDate() {
        List<Event> events = new ArrayList<>();

        // Create 3 events with known eventStartDateTime values
        Calendar cal = Calendar.getInstance();

        // Before range: Jan 5, 2025
        cal.set(2025, Calendar.JANUARY, 5, 12, 0, 0);
        Event before = new Event();
        before.setEventId("before-id");
        before.setName("Before Range Event");
        before.setEventStartDateTime(new Timestamp(cal.getTime()));
        events.add(before);

        // In range: Jan 20, 2025
        cal.set(2025, Calendar.JANUARY, 20, 12, 0, 0);
        Event inRange = new Event();
        inRange.setEventId("inrange-id");
        inRange.setName("In Range Event");
        inRange.setEventStartDateTime(new Timestamp(cal.getTime()));
        events.add(inRange);

        // After range: Feb 3, 2025
        cal.set(2025, Calendar.FEBRUARY, 3, 12, 0, 0);
        Event after = new Event();
        after.setEventId("after-id");
        after.setName("After Range Event");
        after.setEventStartDateTime(new Timestamp(cal.getTime()));
        events.add(after);

        fakeRepository.setEventsToReturn(events);

        launchFragment();

        // All visible before filtering
        onView(withText("Before Range Event")).check(matches(isDisplayed()));
        onView(withText("In Range Event")).check(matches(isDisplayed()));
        onView(withText("After Range Event")).check(matches(isDisplayed()));

        // Open filter dialog
        onView(withId(R.id.filter_button)).perform(click());

        // Select From: Jan 15, 2025
        onView(withIndex(allOf(
                isAssignableFrom(com.google.android.material.textfield.TextInputEditText.class),
                not(androidx.test.espresso.matcher.ViewMatchers.isFocusable()),
                isDisplayed()
        ), 0)).perform(click());

        onView(isAssignableFrom(DatePicker.class))
                .perform(PickerActions.setDate(2025, 1, 15)); // month is 1-12 here
        onView(withId(android.R.id.button1)).perform(click()); // OK

        // Select To: Jan 31, 2025
        onView(withIndex(allOf(
                isAssignableFrom(com.google.android.material.textfield.TextInputEditText.class),
                not(androidx.test.espresso.matcher.ViewMatchers.isFocusable()),
                isDisplayed()
        ), 1)).perform(click());

        onView(isAssignableFrom(DatePicker.class))
                .perform(PickerActions.setDate(2025, 1, 31));
        onView(withId(android.R.id.button1)).perform(click()); // OK

        // Apply the filter
        onView(withText("Apply")).perform(click());

        // Assert: only in-range event remains
        onView(withText("In Range Event")).check(matches(isDisplayed()));
        onView(withText("Before Range Event")).check(doesNotExist());
        onView(withText("After Range Event")).check(doesNotExist());
    }

}