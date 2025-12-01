package com.example.lotteryevent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.PickerActions.setDate;
import static androidx.test.espresso.contrib.PickerActions.setTime;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;

import android.os.Build;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerFlowsTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        // Grant permissions to avoid dialogs blocking the UI
        if (Build.VERSION.SDK_INT >= 33) {
            getInstrumentation().getUiAutomation().executeShellCommand(
                    "pm grant com.example.lotteryevent android.permission.POST_NOTIFICATIONS");
        }
        getInstrumentation().getUiAutomation().executeShellCommand(
                "pm grant com.example.lotteryevent android.permission.ACCESS_FINE_LOCATION");
        getInstrumentation().getUiAutomation().executeShellCommand(
                "pm grant com.example.lotteryevent android.permission.ACCESS_COARSE_LOCATION");
    }

    /**
     * Test Event Creation Flow:
     * Flow: Home -> Click FAB -> Fill Form (Texts + Dates) -> Save -> Verify in List -> Verify in Firestore
     */
    @Test
    public void testCreateEvent() {
        // 1. Wait for App Initialization
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // 2. Click "Add Event" Floating Action Button on HomeFragment
        onView(withId(R.id.fab_add_event)).perform(click());

        // 3. Define unique test data
        String uniqueSuffix = "" + new Random().nextInt(10000);
        String eventName = "Espresso Event " + uniqueSuffix;
        String eventDesc = "Description for " + uniqueSuffix;

        // 4. Fill Basic Text Fields
        onView(withId(R.id.edit_text_event_name))
                .perform(replaceText(eventName), closeSoftKeyboard());
        onView(withId(R.id.edit_text_event_description))
                .perform(replaceText(eventDesc), closeSoftKeyboard());
        onView(withId(R.id.edit_text_event_location))
                .perform(replaceText("Test Location"), closeSoftKeyboard());
        // Use scrollTo() for fields that might be off-screen
        onView(withId(R.id.edit_text_max_attendees))
                .perform(scrollTo(), replaceText("10"), closeSoftKeyboard());
        onView(withId(R.id.edit_text_waiting_list_limit))
                .perform(scrollTo(), replaceText("20"), closeSoftKeyboard());

        // 5. Handle Date & Time Pickers
        // We set dates in the future to satisfy validation logic:
        // Reg Start < Reg End < Event Start < Event End
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1); // Start tomorrow

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // PickerActions expects 1-12 for month
        int day = cal.get(Calendar.DAY_OF_MONTH);

        // -- Registration Start (Tomorrow) --
        setDateOnPicker(R.id.edit_text_registration_start_date, year, month, day);
        setTimeOnPicker(R.id.edit_text_registration_start_time, 10, 0);

        // -- Registration End (Tomorrow + 1 hour) --
        setTimeOnPicker(R.id.edit_text_registration_end_time, 11, 0);
        setDateOnPicker(R.id.edit_text_registration_end_date, year, month, day);

        // -- Event Start (Tomorrow + 2 hours) --
        setTimeOnPicker(R.id.edit_text_event_start_time, 12, 0);
        setDateOnPicker(R.id.edit_text_event_start_date, year, month, day);

        // -- Event End (Tomorrow + 3 hours) --
        setTimeOnPicker(R.id.edit_text_event_end_time, 13, 0);
        setDateOnPicker(R.id.edit_text_event_end_date, year, month, day);

        // 6. Click Save
        onView(withId(R.id.button_save)).perform(scrollTo(), click());

        // 7. Verify Navigation back to Home
        try { Thread.sleep(3000); } catch (InterruptedException e) {}
        onView(withId(R.id.fab_add_event)).check(matches(isDisplayed()));

        // 8. Verify Event appears in RecyclerView
        onView(withId(R.id.events_recycler_view))
                .check(matches(hasDescendant(withText(eventName))));

        // 9. Verify Firestore & Cleanup
        verifyAndCleanupEvent(eventName);
    }

    /**
     * Helper to open a DatePicker via the EditText ID, set the date, and click OK.
     */
    private void setDateOnPicker(int viewId, int year, int month, int day) {
        // Click the EditText to open the dialog
        onView(withId(viewId)).perform(scrollTo(), click());

        // Interact with the DatePicker widget
        onView(withClassName(equalTo(DatePicker.class.getName())))
                .perform(setDate(year, month, day));

        // Click OK (Standard dialog positive button)
        onView(withId(android.R.id.button1)).perform(click());
    }

    /**
     * Helper to open a TimePicker via the EditText ID, set the time, and click OK.
     */
    private void setTimeOnPicker(int viewId, int hour, int minute) {
        // Click the EditText to open the dialog
        onView(withId(viewId)).perform(scrollTo(), click());

        // Interact with the TimePicker widget
        onView(withClassName(equalTo(TimePicker.class.getName())))
                .perform(setTime(hour, minute));

        // Click OK
        onView(withId(android.R.id.button1)).perform(click());
    }

    /**
     * Checks Firestore for the event name, asserts it exists, and deletes it.
     */
    private void verifyAndCleanupEvent(String eventName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assertNotNull("User should be logged in", user);

        final CountDownLatch latch = new CountDownLatch(1);

        // Query for the event by name and organizer
        db.collection("events")
                .whereEqualTo("organizerId", user.getUid())
                .whereEqualTo("name", eventName)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Assert we found the event
                    if (!querySnapshot.isEmpty()) {
                        // Cleanup: Delete the found documents
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            doc.getReference().delete();
                        }
                    } else {
                        throw new AssertionError("Event not found in Firestore: " + eventName);
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    latch.countDown();
                    throw new AssertionError("Firestore query failed: " + e.getMessage());
                });

        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            if (!completed) throw new AssertionError("Timeout waiting for Firestore verification");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}