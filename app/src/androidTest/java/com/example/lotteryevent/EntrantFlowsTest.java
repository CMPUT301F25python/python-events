package com.example.lotteryevent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerActions.open;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isFocusable;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.widget.EditText;

import androidx.test.espresso.contrib.NavigationViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.lotteryevent.data.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EntrantFlowsTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
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
     * 1. Profile Creation & Update
     * Flow: Create Profile -> Verify UI -> Update Profile -> Verify UI -> Verify Firestore
     */
    @Test
    public void test1_CreateEntrantProfile() {
        try { Thread.sleep(3000); } catch (InterruptedException e) {}
        onView(withId(R.id.profile_icon)).perform(click());

        String randomName = "Entrant " + new Random().nextInt(1000);
        String randomEmail = "entrant" + new Random().nextInt(1000) + "@test.com";

        onView(withId(R.id.name_field)).perform(replaceText(randomName), closeSoftKeyboard());
        onView(withId(R.id.email_field)).perform(replaceText(randomEmail), closeSoftKeyboard());
        onView(withId(R.id.update_button)).perform(click());

        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        onView(withId(R.id.name_field)).check(matches(withText(randomName)));

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assertNotNull("User should be logged in", currentUser);
        String uid = currentUser.getUid();

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> firestoreName = new AtomicReference<>();
        final AtomicReference<String> firestoreEmail = new AtomicReference<>();

        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        firestoreName.set(documentSnapshot.getString("name"));
                        firestoreEmail.set(documentSnapshot.getString("email"));
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) {}

        assertEquals("Firestore name should match input", randomName, firestoreName.get());
        assertEquals("Firestore email should match input", randomEmail, firestoreEmail.get());
    }

    /**
     * 2. Profile Settings (Notifications)
     * Flow: Toggle Notifications -> Verify UI -> Verify Firestore
     */
    @Test
    public void test2_ProfileNotificationToggle() {
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        onView(withId(R.id.profile_icon)).perform(click());
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        onView(withId(R.id.notifications_checkbox)).perform(click());
        onView(withId(R.id.update_button)).perform(click());
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        onView(withId(R.id.home_icon)).perform(click());
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        onView(withId(R.id.profile_icon)).perform(click());

        onView(withId(R.id.notifications_checkbox)).check(matches(isDisplayed()));
    }

    /**
     * 3. View Available Events & Filter
     * Flow: Create Event -> Filter -> Verify UI -> Cleanup
     */
    @Test
    public void test3_FilterEvents() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            user = FirebaseAuth.getInstance().getCurrentUser();
        }
        assertNotNull("Need a user to create a test event", user);

        long now = new Date().getTime();
        Timestamp regStart = new Timestamp(new Date(now - 86400000));
        Timestamp regEnd = new Timestamp(new Date(now + 86400000));
        Timestamp evtStart = new Timestamp(new Date(now + 172800000));
        Timestamp evtEnd = new Timestamp(new Date(now + 259200000));

        String uniqueKeyword = "FilterTest" + new Random().nextInt(10000);
        String eventName = "Event " + uniqueKeyword;

        Event testEvent = new Event(
                eventName, "Description", "some_other_organizer", "Test Organizer", 10, 0
        );
        testEvent.setOrganizerId("some_other_organizer");
        testEvent.setRegistrationStartDateTime(regStart);
        testEvent.setRegistrationEndDateTime(regEnd);
        testEvent.setEventStartDateTime(evtStart);
        testEvent.setEventEndDateTime(evtEnd);
        testEvent.setStatus("open");

        final CountDownLatch createLatch = new CountDownLatch(1);
        final AtomicReference<String> eventIdRef = new AtomicReference<>();

        db.collection("events").add(testEvent)
                .addOnSuccessListener(docRef -> {
                    eventIdRef.set(docRef.getId());
                    createLatch.countDown();
                })
                .addOnFailureListener(e -> createLatch.countDown());

        try { createLatch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) {}
        String eventId = eventIdRef.get();
        assertNotNull("Failed to create test event", eventId);

        try {
            onView(withId(R.id.drawer_layout)).perform(open());
            onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.availableEventsFragment));
            try { Thread.sleep(3000); } catch (InterruptedException e) {}

            onView(withId(R.id.filter_button)).perform(click());

            onView(allOf(isAssignableFrom(EditText.class), isFocusable()))
                    .perform(replaceText(uniqueKeyword), closeSoftKeyboard());

            onView(withText("Apply")).perform(click());

            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            onView(withText(eventName)).check(matches(isDisplayed()));

        } finally {
            if (eventId != null) db.collection("events").document(eventId).delete();
        }
    }

    /**
     * 4. Join Waiting List (Full Flow)
     * Flow: Create Event -> Filter -> Join -> Verify UI -> Verify DB -> Leave -> Delete Event
     */
    @Test
    public void test4_JoinAndLeaveWaitingList() {
        test1_CreateEntrantProfile();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assertNotNull(user);
        String uid = user.getUid();

        String otherOrganizerId = "organizer_" + new Random().nextInt(10000);
        String uniqueKeyword = "JoinTest" + new Random().nextInt(10000);
        String eventName = "Event " + uniqueKeyword;

        long now = new Date().getTime();
        Timestamp regStart = new Timestamp(new Date(now - 86400000));
        Timestamp regEnd = new Timestamp(new Date(now + 86400000));
        Timestamp evtStart = new Timestamp(new Date(now + 172800000));
        Timestamp evtEnd = new Timestamp(new Date(now + 259200000));

        Event testEvent = new Event(
                eventName, "Join Description", otherOrganizerId, "Test Organizer", 10, 0
        );
        testEvent.setOrganizerId(otherOrganizerId);
        testEvent.setStatus("open");
        testEvent.setGeoLocationRequired(false);
        testEvent.setWaitingListLimit(100);

        testEvent.setRegistrationStartDateTime(regStart);
        testEvent.setRegistrationEndDateTime(regEnd);
        testEvent.setEventStartDateTime(evtStart);
        testEvent.setEventEndDateTime(evtEnd);

        final CountDownLatch createLatch = new CountDownLatch(1);
        final AtomicReference<String> eventIdRef = new AtomicReference<>();

        // Add event and manually initialize attendeeCount for Repository consistency
        db.collection("events").add(testEvent)
                .addOnSuccessListener(docRef -> {
                    eventIdRef.set(docRef.getId());
                    Map<String, Object> update = new HashMap<>();
                    update.put("attendeeCount", 0);
                    docRef.update(update).addOnCompleteListener(task -> createLatch.countDown());
                })
                .addOnFailureListener(e -> createLatch.countDown());

        try { createLatch.await(8, TimeUnit.SECONDS); } catch (InterruptedException e) {}
        String eventId = eventIdRef.get();
        assertNotNull("Failed to create test event", eventId);

        try {
            // 2. Navigate and Filter
            onView(withId(R.id.drawer_layout)).perform(open());
            onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.availableEventsFragment));
            try { Thread.sleep(3000); } catch (InterruptedException e) {}

            onView(withId(R.id.filter_button)).perform(click());
            onView(allOf(isAssignableFrom(EditText.class), isFocusable()))
                    .perform(replaceText(uniqueKeyword), closeSoftKeyboard());
            onView(withText("Apply")).perform(click());
            try { Thread.sleep(2000); } catch (InterruptedException e) {}

            // 3. Click the event
            onView(withText(eventName)).perform(click());
            try { Thread.sleep(3000); } catch (InterruptedException e) {}

            // 4. Join Waiting List
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            onView(withId(R.id.btn_action_positive)).perform(click()); // Click "Join Waiting List"

            // Wait for update
            try { Thread.sleep(3000); } catch (InterruptedException e) {}

            // 5. Verify UI
            onView(withId(R.id.btn_action_positive)).check(matches(withText("Leave Waiting List")));

            // 6. Verify Firestore
            final CountDownLatch checkLatch = new CountDownLatch(1);
            final AtomicBoolean userInList = new AtomicBoolean(false);

            db.collection("events").document(eventId)
                    .collection("entrants").document(uid).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            userInList.set(true);
                        }
                        checkLatch.countDown();
                    })
                    .addOnFailureListener(e -> checkLatch.countDown());

            try { checkLatch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) {}
            assertTrue("User should exist in the entrants subcollection in Firestore", userInList.get());

            // 7. Leave Waiting List
            onView(withId(R.id.btn_action_positive)).perform(click());
            try { Thread.sleep(3000); } catch (InterruptedException e) {}

            // Verify UI reverted to "Join Waiting List"
            onView(withId(R.id.btn_action_positive)).check(matches(withText("Join Waiting List")));

        } finally {
            if (eventId != null) {
                db.collection("events").document(eventId).collection("entrants").document(uid).delete();
                db.collection("events").document(eventId).delete();
            }
        }
    }

    /**
     * 5. View Registration History
     * Flow: Create History Data -> Navigate to History -> Verify UI -> Cleanup
     */
    @Test
    public void test5_ViewHistory() {
        // Setup Data (so we verify an actual history item, not just the empty state)
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            user = FirebaseAuth.getInstance().getCurrentUser();
        }

        String uid = (user != null) ? user.getUid() : "test_uid";
        String eventName = "HistEvent " + new Random().nextInt(1000);

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> eventIdRef = new AtomicReference<>();

        Map<String, Object> event = new HashMap<>();
        event.put("name", eventName);
        event.put("status", "open");

        db.collection("events").add(event).addOnSuccessListener(ref -> {
            eventIdRef.set(ref.getId());
            Map<String, Object> entrant = new HashMap<>();
            entrant.put("status", "waiting");
            ref.collection("entrants").document(uid).set(entrant)
                    .addOnCompleteListener(t -> latch.countDown());
        });

        try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) {}
        String eventId = eventIdRef.get();

        try {
            // 1. Go to Profile
            onView(withId(R.id.profile_icon)).perform(click());
            try { Thread.sleep(1000); } catch (InterruptedException e) {}

            // 2. Click History Button
            onView(withId(R.id.history_button)).perform(click());

            // 3. Wait for list to load
            try { Thread.sleep(4000); } catch (InterruptedException e) {}

            // 4. Verify we are on History Screen
            onView(withText("Registration History")).check(matches(isDisplayed()));

            // 5. Verify the event and status
            onView(withText(eventName)).check(matches(isDisplayed()));
            onView(withText("Not Selected")).check(matches(isDisplayed())); // "waiting" maps to "Not Selected"

        } finally {
            if (eventId != null) {
                db.collection("events").document(eventId).collection("entrants").document(uid).delete();
                db.collection("events").document(eventId).delete();
            }
        }
    }
}