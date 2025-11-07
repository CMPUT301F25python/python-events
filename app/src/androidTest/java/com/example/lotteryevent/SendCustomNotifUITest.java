package com.example.lotteryevent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;

import android.os.Bundle;

import androidx.navigation.Navigation;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.data.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests flow around sending custom notif to entrants
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SendCustomNotifUITest {
    private FirebaseFirestore db;
    private FirebaseUser user;
    private Event event;

    @Rule
    public ActivityScenarioRule<MainActivity> scenario = new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Sets up data for tests, event
     */
    @Before
    public void setUpTest() {
        db = FirebaseFirestore.getInstance();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        user = FirebaseAuth.getInstance().getCurrentUser();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        event = new Event(
                "Snowball fight",
                "Snow day shenanigens",
                String.valueOf(user.getUid()),
                "John Doe",
                3);
        event.setOrganizerId(user.getUid());
        db.collection("events").add(event).addOnSuccessListener(documentReference -> {
            String eventId = documentReference.getId();
            event.setEventId(eventId);
        });

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * removes data used for test
     */
    @After
    public void tearDownTests() {
        db.collection("events").document(event.getEventId()).delete();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tests send notif button on Waiting list page, testing input field edit, and notify all and cancel
     * buttons in terms of what flow immediately happens next
     */
    @Test
    public void testNotifyButtonWaiting() {
        scenario.getScenario().onActivity(activity -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getEventId());
            bundle.putString("status", "waiting");
            Navigation.findNavController(activity, R.id.nav_host_fragment).navigate(R.id.entrantListFragment, bundle);
        });
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        onView(withText(containsString("Waiting"))).check(matches(isDisplayed()));
        onView(withId(R.id.send_notification_button)).perform(click());

        onView(withText(containsString("Notification Message"))).check(matches(isDisplayed()));
        onView(withHint("Enter message...")).perform(click(), typeText("Test message"), closeSoftKeyboard());
        onView(withText(containsString("Notify All"))).check(matches(isDisplayed()));
        onView(withText(containsString("Notify All"))).perform(click());

        onView(withId(R.id.send_notification_button)).perform(click());
        onView(withText(containsString("Cancel"))).check(matches(isDisplayed()));
        onView(withText(containsString("Cancel"))).perform(click());

    }

    /**
     * Tests send notif button on Accepted list page, testing input field edit, and notify all and cancel
     * buttons in terms of what flow immediately happens next
     */
    @Test
    public void testNotifyButtonAccepted() {
        scenario.getScenario().onActivity(activity -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getEventId());
            bundle.putString("status", "accepted");
            Navigation.findNavController(activity, R.id.nav_host_fragment).navigate(R.id.entrantListFragment, bundle);
        });
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        onView(withText(containsString("Accepted"))).check(matches(isDisplayed()));
        onView(withId(R.id.send_notification_button)).perform(click());

        onView(withText(containsString("Notification Message"))).check(matches(isDisplayed()));
        onView(withHint("Enter message...")).perform(click(), typeText("Test message"), closeSoftKeyboard());
        onView(withText(containsString("Notify All"))).check(matches(isDisplayed()));
        onView(withText(containsString("Notify All"))).perform(click());

        onView(withId(R.id.send_notification_button)).perform(click());
        onView(withText(containsString("Cancel"))).check(matches(isDisplayed()));
        onView(withText(containsString("Cancel"))).perform(click());
    }

    /**
     * Tests send notif button on Invited list page, testing input field edit, and notify all and cancel
     * buttons in terms of what flow immediately happens next
     */
    @Test
    public void testNotifyButtonInvited() {
        scenario.getScenario().onActivity(activity -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getEventId());
            bundle.putString("status", "invited");
            Navigation.findNavController(activity, R.id.nav_host_fragment).navigate(R.id.entrantListFragment, bundle);
        });
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        onView(withText(containsString("Invited"))).check(matches(isDisplayed()));
        onView(withId(R.id.send_notification_button)).perform(click());

        onView(withText(containsString("Notification Message"))).check(matches(isDisplayed()));
        onView(withHint("Enter message...")).perform(click(), typeText("Test message"), closeSoftKeyboard());
        onView(withText(containsString("Notify All"))).check(matches(isDisplayed()));
        onView(withText(containsString("Notify All"))).perform(click());

        onView(withId(R.id.send_notification_button)).perform(click());
        onView(withText(containsString("Cancel"))).check(matches(isDisplayed()));
        onView(withText(containsString("Cancel"))).perform(click());
    }

    /**
     * Tests send notif button on Cancelled list page, testing input field edit, and notify all and cancel
     * buttons in terms of what flow immediately happens next
     */
    @Test
    public void testNotifyButtonCancelled() {
        scenario.getScenario().onActivity(activity -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getEventId());
            bundle.putString("status", "cancelled");
            Navigation.findNavController(activity, R.id.nav_host_fragment).navigate(R.id.entrantListFragment, bundle);
        });
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        onView(withText(containsString("Cancelled"))).check(matches(isDisplayed()));
        onView(withId(R.id.send_notification_button)).perform(click());

        onView(withText(containsString("Notification Message"))).check(matches(isDisplayed()));
        onView(withHint("Enter message...")).perform(click(), typeText("Test message"), closeSoftKeyboard());
        onView(withText(containsString("Notify All"))).check(matches(isDisplayed()));
        onView(withText(containsString("Notify All"))).perform(click());

        onView(withId(R.id.send_notification_button)).perform(click());
        onView(withText(containsString("Cancel"))).check(matches(isDisplayed()));
        onView(withText(containsString("Cancel"))).perform(click());
    }
}