package com.example.lotteryevent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.containsString;

import android.util.Log;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
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
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NotificationUITest {
    private FirebaseFirestore db;
    private FirebaseUser user;
    private Event event;
    private Notification notification;
    @Rule
    public ActivityScenarioRule<MainActivity> scenario = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void navigateToCreateEventFragment() {
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
        db.collection("events").add(event).addOnSuccessListener(documentReference -> {
            String eventId = documentReference.getId();
            event.setEventId(eventId);

            notification = new Notification(
                    user.getUid(), "Congratulations!",
                    "You have been selected for event " + event.getName(),
                    "lottery_win", String.valueOf(event.getEventId()),
                    event.getName(), String.valueOf(user.getUid()),
                    "John Doe");


            db.collection("notifications")
                    .add(notification).addOnSuccessListener(documentReference2 -> {
                        notification.setNotificationId(documentReference2.getId());
                    });
        });

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        scenario.getScenario().onActivity(activity -> {
             Navigation.findNavController(activity, R.id.nav_host_fragment).navigate(R.id.notificationsFragment);
        });
    }

    @After
    public void deleteEventNotif() {
        db.collection("notifications").document(notification.getNotificationId()).delete();
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

    @Test
    public void testNotificationLeadsToEvent() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        onView(withText(containsString("Tap to accept or decline"))).perform(click());

        onView(withText(containsString("Event Details"))).check(matches(isDisplayed()));
        onView(withText(containsString("Snowball fight"))).check(matches(isDisplayed()));
        onView(withText(containsString("John Doe"))).check(matches(isDisplayed()));
    }

    @Test
    public void testNotificationShows() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        onView(withText(containsString("You've been selected for " + event.getName()))).check(matches(isDisplayed()));
    }
}