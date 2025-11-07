package com.example.lotteryevent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CreateEventFragmentUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> scenario = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void navigateToCreateEventFragment() {
         onView(withId(R.id.fab_add_event)).perform(click());
    }

    @Test
    public void testRegistrationDatePickerIsDisplayed() {
        // Perform a click on the registration start date EditText
        onView(withId(R.id.edit_text_registration_start_date)).perform(click());

        // Verify that the DatePickerDialog is shown by checking for its OK button
        onView(withText("OK")).check(matches(isDisplayed()));
    }

    @Test
    public void testRegistrationTimePickerIsDisplayed() {
        // Perform a click on the registration start time EditText
        onView(withId(R.id.edit_text_registration_start_time)).perform(click());

        // Verify that the TimePickerDialog is shown by checking for its OK button
        onView(withText("OK")).check(matches(isDisplayed()));
    }

    @Test
    public void testEventDatePickerIsDisplayed() {
        // Perform a click on the event start date EditText
        onView(withId(R.id.edit_text_event_start_date)).perform(click());

        // Verify that the DatePickerDialog is shown by checking for its OK button
        onView(withText("OK")).check(matches(isDisplayed()));
    }

    @Test
    public void testEventTimePickerIsDisplayed() {
        // Perform a click on the event start time EditText
        onView(withId(R.id.edit_text_event_start_time)).perform(click());

        // Verify that the TimePickerDialog is shown by checking for its OK button
        onView(withText("OK")).check(matches(isDisplayed()));
    }

    @Test
    public void testEventNameInputIsDisplayed() {
        // Check that the "Event Name" input field is displayed on the screen
        onView(withId(R.id.edit_text_event_name)).check(matches(isDisplayed()));
    }

    @Test
    public void testSaveButtonIsDisplayed() {
        // Scroll to the button to make sure it's visible
        onView(ViewMatchers.withId(R.id.button_save)).perform(ViewActions.scrollTo());

        // Check that the "Create Event" button is displayed
        onView(ViewMatchers.withId(R.id.button_save)).check(matches(isDisplayed()));
    }

    @Test
    public void testWaitingListLimitInputIsDisplayed() {
        // Scroll to the waiting list limit field to ensure it is visible
        onView(withId(R.id.edit_text_waiting_list_limit)).perform(ViewActions.scrollTo());

        // Check that the "Waiting List Limit" input field is displayed on the screen
        onView(withId(R.id.edit_text_waiting_list_limit)).check(matches(isDisplayed()));
    }

}
