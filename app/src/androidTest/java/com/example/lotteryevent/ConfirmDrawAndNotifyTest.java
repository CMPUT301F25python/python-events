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

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for flow around pressing the confirm draw and notify button,
 * Checking which screen one goes to
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ConfirmDrawAndNotifyTest {
    private FirebaseFirestore db;
    private FirebaseUser user;
    private Event event;
    private Entrant entrant;

    @Rule
    public ActivityScenarioRule<MainActivity> scenario = new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Sets up db bu making event and event's entrant
     */
    @Before
    public void setUpTests() {
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

            entrant = new Entrant();
            entrant.setUserId("random");
            entrant.setStatus("waiting");
            db.collection("events").document(event.getEventId()).collection("entrants").add(entrant)
                    .addOnSuccessListener(documentReference2 -> {
                        String userId = documentReference2.getId();
                        entrant.setUserId(userId);
                    });
        });

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        scenario.getScenario().onActivity(activity -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getEventId());
            Navigation.findNavController(activity, R.id.nav_host_fragment).navigate(R.id.runDrawFragment, bundle);
        });
    }

    /**
     * Removes data used for testing
     */
    @After
    public void tearDownTests() {
        db.collection("events").document(event.getEventId()).collection("entrants").document(entrant.getUserId()).delete();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        db.collection("events").document(event.getEventId()).delete();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tests for correct values on Confirm and Notify screen and the screen change behaviour when
     * confirm and notify button is selected
     */
    @Test
    public void testConfirmAndNotify() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        onView(withId(R.id.numSelectedEntrants)).perform(click(), typeText("1"), closeSoftKeyboard());
        onView(withId(R.id.runDrawButton)).perform(click());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        onView(withText(containsString("Confirm Draw and Notify"))).check(matches(isDisplayed()));
        onView(withId(R.id.waiting_list_count)).check(matches(withText("0")));
        onView(withId(R.id.available_space_count)).check(matches(withText("2")));
        onView(withId(R.id.selected_users_count)).check(matches(withText("1")));

        onView(withId(R.id.confirm_and_notify_button)).perform(click());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        onView(withText(containsString("Event Details"))).check(matches(isDisplayed()));
        onView(withText(containsString(event.getName()))).check(matches(isDisplayed()));

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tests screen change behaviour around cancelling confirm and notify
     */
    @Test
    public void testConfirmAndNotifyCancel() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        onView(withId(R.id.numSelectedEntrants)).perform(click(), typeText("1"), closeSoftKeyboard());
        onView(withId(R.id.runDrawButton)).perform(click());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        onView(withText(containsString("Confirm Draw and Notify"))).check(matches(isDisplayed()));
        onView(withId(R.id.waiting_list_count)).check(matches(withText("0")));
        onView(withId(R.id.available_space_count)).check(matches(withText("2")));
        onView(withId(R.id.selected_users_count)).check(matches(withText("1")));

        onView(withId(R.id.cancel_button)).perform(click());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        onView(withText(containsString("Event Details"))).check(matches(isDisplayed()));
        onView(withText(containsString(event.getName()))).check(matches(isDisplayed()));

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}