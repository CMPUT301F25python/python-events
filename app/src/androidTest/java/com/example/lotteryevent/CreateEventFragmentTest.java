package com.example.lotteryevent;

import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.FakeEventRepository;
import com.example.lotteryevent.ui.CreateEventFragment;
import com.example.lotteryevent.viewmodels.CreateEventViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented Unit Test for {@link CreateEventFragment}.
 * <p>
 * This class validates the UI behavior, input validation, and data persistence logic
 * of the event creation screen. It uses a {@link FakeEventRepository} to verify that
 * data is correctly passed from the UI -> ViewModel -> Repository without making
 * real network calls.
 * <p>
 * It also utilizes {@code espresso-contrib} to interact with system Date and Time pickers.
 */
@RunWith(AndroidJUnit4.class)
public class CreateEventFragmentTest {

    private FakeEventRepository fakeRepository;
    private ReusableTestFragmentFactory fragmentFactory;
    private TestNavHostController navController;

    /**
     * Sets up the test environment before each test execution.
     * <p>
     * 1. Initializes the {@link FakeEventRepository} (which starts with 2 default events).
     * 2. Configures the {@link GenericViewModelFactory} to inject the fake repo into the ViewModel.
     * 3. Configures the {@link ReusableTestFragmentFactory} to inject the ViewModel into the Fragment.
     */
    @Before
    public void setup() {
        // 1. Create the existing Fake Repo
        fakeRepository = new FakeEventRepository();

        // 2. Create ViewModel Factory
        GenericViewModelFactory viewModelFactory = new GenericViewModelFactory();
        viewModelFactory.put(CreateEventViewModel.class, () -> new CreateEventViewModel(fakeRepository));

        // 3. Create Fragment Factory
        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(CreateEventFragment.class, () -> new CreateEventFragment(viewModelFactory));
    }

    /**
     * Helper method to launch the {@link CreateEventFragment} in an isolated container
     * and attach a {@link TestNavHostController}.
     * <p>
     * This allows us to verify navigation events or ensure the fragment is hosted correctly
     * without launching the full MainActivity.
     */
    private void launchFragment() {
        navController = new TestNavHostController(ApplicationProvider.getApplicationContext());
        FragmentScenario<CreateEventFragment> scenario = FragmentScenario.launchInContainer(
                CreateEventFragment.class, null, R.style.Theme_LotteryEvent, fragmentFactory
        );
        scenario.onFragment(fragment -> {
            navController.setGraph(R.navigation.nav_graph);
            navController.setCurrentDestination(R.id.createEventFragment);
            Navigation.setViewNavController(fragment.requireView(), navController);
        });
    }

    /**
     * Verifies that the system prevents event creation when the "Event Name" field is empty.
     * <p>
     * <b>Scenario:</b> User clicks save without typing a name.
     * <b>Expected Result:</b> The repository size remains unchanged (2 events), indicating the save failed.
     */
    @Test
    public void emptyEventName_doesNotCreateEvent() {
        launchFragment();

        // Act: Click save without entering a name
        onView(withId(R.id.button_save)).perform(scrollTo(), click());

        // Assert:
        // The repo starts with 2 events (from constructor).
        // It should still have 2 events because validation failed.
        assertEquals("Repository size should not change on validation error",
                2, fakeRepository.getInMemoryEvents().size());
    }

    /**
     * Verifies the business logic regarding capacity constraints.
     * <p>
     * <b>Scenario:</b> User enters a Max Attendees count of 0.
     * <b>Expected Result:</b> Validation fails, and the repository size remains unchanged.
     */
    @Test
    public void zeroCapacity_doesNotCreateEvent() {
        launchFragment();

        // Arrange: Enter valid name, but invalid capacity logic
        onView(withId(R.id.edit_text_event_name)).perform(typeText("Zero Cap Event"), closeSoftKeyboard());

        // Capacity = 0
        onView(withId(R.id.edit_text_max_attendees)).perform(scrollTo(), typeText("0"), closeSoftKeyboard());

        // Act: Click save
        onView(withId(R.id.button_save)).perform(scrollTo(), click());

        // Assert: Repo size should still be 2
        assertEquals(2, fakeRepository.getInMemoryEvents().size());
    }

    /**
     * Verifies the business logic regarding capacity constraints.
     * <p>
     * <b>Scenario:</b> User enters a Waiting List limit (5) that is smaller than the Max Attendees (10).
     * <b>Expected Result:</b> Validation fails, and the repository size remains unchanged.
     */
    @Test
    public void invalidCapacity_doesNotCreateEvent() {
        launchFragment();

        // Arrange: Enter valid name, but invalid capacity logic
        onView(withId(R.id.edit_text_event_name)).perform(typeText("Invalid Cap Event"), closeSoftKeyboard());

        // Capacity = 10
        onView(withId(R.id.edit_text_max_attendees)).perform(scrollTo(), typeText("10"), closeSoftKeyboard());
        // Waiting List = 5 (Must be >= Capacity)
        onView(withId(R.id.edit_text_waiting_list_limit)).perform(scrollTo(), typeText("5"), closeSoftKeyboard());

        // Act: Click save
        onView(withId(R.id.button_save)).perform(scrollTo(), click());

        // Assert: Repo size should still be 2
        assertEquals(2, fakeRepository.getInMemoryEvents().size());
    }

    /**
     * Verifies the "Happy Path" where all inputs are valid.
     * <p>
     * <b>Scenario:</b> User enters a valid name, future dates (using Pickers), and valid capacity numbers.
     * <b>Expected Result:</b>
     * 1. The repository size increases to 3.
     * 2. The new event contains the exact data entered in the UI.
     * 3. A success message is posted to the ViewModel.
     */
    @Test
    public void validInputs_createsEvent_andUpdatesRepository() {
        launchFragment();

        // Use helper to fill form
        fillValidFormInputs("Espresso Party");

        // Click Save
        onView(withId(R.id.button_save)).perform(scrollTo(), click());

        // Assert
        List<Event> events = fakeRepository.getInMemoryEvents();
        assertEquals("Repository should have a new event", 3, events.size());

        Event newEvent = events.get(2);
        assertEquals("Espresso Party", newEvent.getName());
        assertEquals(Integer.valueOf(50), newEvent.getCapacity());

        // Verify default behavior for location (assuming default is false)
        assertFalse("GeoLocation should be false by default", newEvent.getGeoLocationRequired());

        assertEquals("Event created successfully!", fakeRepository.getMessage().getValue());
    }

    @Test
    public void geoLocationSwitch_toggledOn_savesEventWithLocationRequired() {
        launchFragment();

        // 1. Fill in valid basic info so validation passes
        fillValidFormInputs("Location Party");

        // 2. Click the Checkbox (Updated ID)
        onView(withId(R.id.checkbox_geolocation)).perform(scrollTo(), click());

        // 3. Click Save
        onView(withId(R.id.button_save)).perform(scrollTo(), click());

        // 4. Assert
        List<Event> events = fakeRepository.getInMemoryEvents();
        Event newEvent = events.get(2); // The 3rd event (index 2)

        assertEquals("Location Party", newEvent.getName());
        // This will check if the fragment actually saved the checkbox state
        assertTrue("GeoLocation should be required after toggle", newEvent.getGeoLocationRequired());
    }

    // --- Helpers ---

    /**
     * Helper to fill out the form with valid data to pass validation.
     * Updated to include Description, Location, and Registration Dates.
     */
    private void fillValidFormInputs(String eventName) {
        // 1. Name
        onView(withId(R.id.edit_text_event_name)).perform(typeText(eventName), closeSoftKeyboard());

        // 2. Description (MANDATORY)
        onView(withId(R.id.edit_text_event_description)).perform(scrollTo(), typeText("Test Description"), closeSoftKeyboard());

        // 3. Location (MANDATORY)
        onView(withId(R.id.edit_text_event_location)).perform(scrollTo(), typeText("Test Location"), closeSoftKeyboard());

        // Calculate dates
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1); // Tomorrow
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // Months are 0-indexed
        int day = cal.get(Calendar.DAY_OF_MONTH);

        // 4. Registration Dates (MANDATORY)
        setDate(R.id.edit_text_registration_start_date, year, month, day);
        setTime(R.id.edit_text_registration_start_time, 9, 0);

        setDate(R.id.edit_text_registration_end_date, year, month, day);
        setTime(R.id.edit_text_registration_end_time, 11, 0);

        // 5. Event Dates (MANDATORY)
        setDate(R.id.edit_text_event_start_date, year, month, day);
        setTime(R.id.edit_text_event_start_time, 12, 0);

        setDate(R.id.edit_text_event_end_date, year, month, day);
        setTime(R.id.edit_text_event_end_time, 14, 0);

        // 6. Capacity (MANDATORY)
        onView(withId(R.id.edit_text_max_attendees)).perform(scrollTo(), typeText("50"), closeSoftKeyboard());

        // 7. Waiting List (OPTIONAL)
        onView(withId(R.id.edit_text_waiting_list_limit)).perform(scrollTo(), typeText("100"), closeSoftKeyboard());
    }

    /**
     * Interacts with the system {@link DatePicker} dialog.
     * This requires the {@code espresso-contrib} library.
     *
     * @param viewId The resource ID of the EditText that triggers the DatePicker.
     * @param year   The year to set.
     * @param month  The month to set (1-12).
     * @param day    The day to set.
     */
    private void setDate(int viewId, int year, int month, int day) {
        // 1. Click the EditText to open the dialog
        onView(withId(viewId)).perform(scrollTo(), click());

        // 2. Find the DatePicker inside the dialog and set the date
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(year, month, day));

        // 3. Click the "OK" button
        onView(withId(android.R.id.button1)).perform(click());
    }

    /**
     * Interacts with the system {@link TimePicker} dialog.
     * This requires the {@code espresso-contrib} library.
     *
     * @param viewId The resource ID of the EditText that triggers the TimePicker.
     * @param hour   The hour to set (0-23).
     * @param minute The minute to set (0-59).
     */
    private void setTime(int viewId, int hour, int minute) {
        // 1. Click the EditText to open the dialog
        onView(withId(viewId)).perform(scrollTo(), click());

        // 2. Find the TimePicker and set the time
        onView(withClassName(equalTo(TimePicker.class.getName())))
                .perform(PickerActions.setTime(hour, minute));

        // 3. Click "OK"
        onView(withId(android.R.id.button1)).perform(click());
    }
}