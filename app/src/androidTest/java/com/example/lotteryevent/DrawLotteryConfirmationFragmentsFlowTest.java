package com.example.lotteryevent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.allOf;

import android.os.Bundle;
import android.view.View;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.navigation.Navigation;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.util.TreeIterables;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tests for flow from running lottery to confirming lottery or cancelling lottery.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class DrawLotteryConfirmationFragmentsFlowTest {
    private FirebaseFirestore db;
    private FirebaseUser user;
    private Event event;
    private Entrant entrant;

    @Rule
    public ActivityScenarioRule<MainActivity> scenario = new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    /**
     * Helper function for espresso wait
     */
    public static ViewAction waitForView(final Matcher<View> matcher, final long timeoutMs) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isRoot();
            }

            @Override
            public String getDescription() {
                return "Waiting up to " + timeoutMs + "ms for view: " + matcher.toString();
            }

            @Override
            public void perform(UiController uiController, View view) {
                long start = System.currentTimeMillis();
                long end = start + timeoutMs;

                do {
                    for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
                        if (matcher.matches(child)) {
                            return;
                        }
                    }
                    uiController.loopMainThreadForAtLeast(50);
                } while (System.currentTimeMillis() < end);

                throw new PerformException.Builder()
                        .withCause(new TimeoutException())
                        .withActionDescription(this.getDescription())
                        .build();
            }
        };
    }

    /**
     * Sets up db by making event and event's entrant
     */
    @Before
    public void setUpTests() throws InterruptedException {
        getInstrumentation().getUiAutomation().executeShellCommand("pm grant com.example.lotteryevent android.permission.POST_NOTIFICATIONS");

        db = FirebaseFirestore.getInstance();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        user = FirebaseAuth.getInstance().getCurrentUser();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Wait for event and entrant creation
        final CountDownLatch latch = new CountDownLatch(2);

        event = new Event(
                "Snowball fight",
                "Snow day shenanigens",
                String.valueOf(user.getUid()),
                "John Doe",
                3,
                5);
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

        latch.await(10, TimeUnit.SECONDS);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        scenario.getScenario().onActivity(activity -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getEventId());
            Navigation.findNavController(activity, R.id.nav_host_fragment).navigate(R.id.runDrawFragment, bundle);
        });

        // Wait for the page to load
        onView(isRoot()).perform(waitForView(withId(R.id.runDrawButton), 10000));
        onView(withId(R.id.runDrawButton)).check(matches(isDisplayed()));
    }

    /**
     * Removes data used for testing
     */
    @After
    public void tearDownTests() throws InterruptedException {
        // Wait for 4 operations
        final CountDownLatch latch = new CountDownLatch(4);

        db.collection("events").document(event.getEventId()).collection("entrants").document(entrant.getUserId()).delete();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        db.collection("events").document(event.getEventId()).delete().addOnCompleteListener(task ->
                latch.countDown());
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        db.collection("notifications")
            .whereEqualTo("eventId", event.getEventId())
            .whereEqualTo("organizerId", user.getUid())
            .whereEqualTo("type", "lottery_win")      // from your screenshot
            .get().addOnSuccessListener(notifs -> {
                for (DocumentSnapshot notif : notifs) {
                    notif.getReference().delete().addOnCompleteListener(task ->
                            latch.countDown());
                }
                if (notifs.isEmpty()) {
                    latch.countDown();
                }
            });
        latch.await(10, TimeUnit.SECONDS);
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
        // 1. Run the draw
        onView(withId(R.id.numSelectedEntrants)).perform(click(), typeText("1"), closeSoftKeyboard());
        onView(withId(R.id.runDrawButton)).perform(click());

        // 2. Sleep for 5 seconds to allow the fragment to load AND Firebase to fetch data.
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 3. Verify we are on the confirm page
        onView(withText(containsString("Confirm Draw"))).check(matches(isDisplayed()));

        // 4. Check the data
        // We use containsString("0") just in case there is unexpected whitespace (e.g. " 0 ")
        onView(withId(R.id.waiting_list_count)).check(matches(withText(containsString("0"))));
        onView(withId(R.id.available_space_count)).check(matches(withText(containsString("2"))));
        onView(withId(R.id.selected_users_count)).check(matches(withText(containsString("1"))));

        // 5. Click "Confirm and Notify"
        onView(withId(R.id.confirm_and_notify_button)).perform(click());

        // 6. Wait for the navigation to the next screen
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 7. Check we arrived at the Event Details page
        onView(withText(containsString("Event Details"))).check(matches(isDisplayed()));
        onView(withText(containsString(event.getName()))).check(matches(isDisplayed()));
    }

    /**
     * Tests screen change behaviour around cancelling confirm and notify
     */
    /**
     * Tests screen change behaviour around cancelling confirm and notify
     */
    @Test
    public void testConfirmAndNotifyCancel() {
        // Run the draw
        onView(withId(R.id.numSelectedEntrants)).perform(click(), typeText("1"), closeSoftKeyboard());
        onView(withId(R.id.runDrawButton)).perform(click());

        // 1. HARD WAIT: Sleep for 5 seconds to guarantee Firebase has time to return data.
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 2. Verify we are on the confirm page
        onView(withText(containsString("Confirm Draw"))).check(matches(isDisplayed()));

        // 3. Check the data
        onView(withId(R.id.waiting_list_count)).check(matches(withText(containsString("0"))));
        onView(withId(R.id.available_space_count)).check(matches(withText(containsString("2"))));
        onView(withId(R.id.selected_users_count)).check(matches(withText(containsString("1"))));

        // 4. Click cancel button
        onView(withId(R.id.cancel_button)).perform(click());

        // 5. Wait for return
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 6. Check contents of event details
        onView(withText(containsString("Event Details"))).check(matches(isDisplayed()));
        onView(withText(containsString(event.getName()))).check(matches(isDisplayed()));
    }

}