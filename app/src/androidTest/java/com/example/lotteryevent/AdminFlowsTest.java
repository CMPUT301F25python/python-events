package com.example.lotteryevent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.DrawerActions.open;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Build;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.contrib.NavigationViewActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.lotteryevent.data.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminFlowsTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Track resources to clean up even if tests fail
    private List<String> eventsCleanupList = new ArrayList<>();
    private List<String> usersCleanupList = new ArrayList<>();

    @Before
    public void setUp() {
        // 1. Grant Permissions
        if (Build.VERSION.SDK_INT >= 33) {
            getInstrumentation().getUiAutomation().executeShellCommand(
                    "pm grant com.example.lotteryevent android.permission.POST_NOTIFICATIONS");
        }

        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        // 2. Wait for Auth
        if (auth.getCurrentUser() == null) {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
        }
        currentUser = auth.getCurrentUser();
        assertNotNull("User must be logged in for admin tests", currentUser);

        // 3. Promote Current User to Admin in Firestore
        // We set the name so "Welcome Admin User" or similar UI elements don't crash
        updateUserAdminStatus(currentUser.getUid(), true, "Admin User");

        // 4. Restart Activity to refresh Navigation Drawer
        activityRule.getScenario().close();
        ActivityScenario.launch(MainActivity.class);

        try { Thread.sleep(2000); } catch (InterruptedException e) {}
    }

    /**
     * CLEANUP: This runs after every test to ensure the DB is restored.
     */
    @After
    public void tearDown() {
        // We use a latch to ensure the test runner doesn't kill the process
        // before Firestore operations complete.
        int count = eventsCleanupList.size() + usersCleanupList.size() + 1; // +1 for currentUser
        CountDownLatch latch = new CountDownLatch(count);

        // 1. Delete created events (fail-safe if UI deletion failed)
        for (String eventId : eventsCleanupList) {
            db.collection("events").document(eventId).delete()
                    .addOnCompleteListener(t -> latch.countDown());
        }

        // 2. Delete created dummy users
        for (String userId : usersCleanupList) {
            db.collection("users").document(userId).delete()
                    .addOnCompleteListener(t -> latch.countDown());
        }

        // 3. Delete the Current User's Profile Document completely
        // This removes the "Admin User" name and "admin: true" flag, restoring the DB state.
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).delete()
                    .addOnCompleteListener(t -> latch.countDown());
        } else {
            latch.countDown();
        }

        try {
            // Wait up to 5 seconds for cleanup
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper to write admin status to Firestore synchronously-ish
     */
    private void updateUserAdminStatus(String uid, boolean isAdmin, String name) {
        CountDownLatch latch = new CountDownLatch(1);
        Map<String, Object> update = new HashMap<>();
        update.put("admin", isAdmin);
        if (name != null) update.put("name", name);

        db.collection("users").document(uid).set(update, com.google.firebase.firestore.SetOptions.merge())
                .addOnCompleteListener(t -> latch.countDown());

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    /**
     * Test Browse and Delete Images
     * Flow: Admin Images -> Select Image -> Delete -> Verify Deletion
     */
    @Test
    public void testAdminDeleteImage() {
        String fakeEventId = "test_img_" + new Random().nextInt(10000);
        createFakeImageEntry(fakeEventId);

        // 1. Open Drawer -> Admin Images
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.adminImagesFragment));

        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // 2. Click Image
        onView(withId(R.id.admin_images_recycler))
                .perform(actionOnItemAtPosition(0, click()));

        // 3. Click Delete in Dialog
        onView(withId(R.id.dialog_delete_btn)).perform(click());

        // 4. Confirm
        onView(withText("Delete")).perform(click());

        // 5. Verify list is still displayed (indicating we returned)
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        onView(withId(R.id.admin_images_recycler)).check(matches(isDisplayed()));
    }

    /**
     * Test Browse and Delete Users
     * Flow: Admin Profiles -> Select User -> Delete -> Verify Deletion
     */
    @Test
    public void testAdminDeleteUserProfile() {
        String uniqueName = "Bad User " + new Random().nextInt(1000);
        String dummyUserId = "dummy_user_" + new Random().nextInt(10000);
        createDummyUser(dummyUserId, uniqueName);

        // 1. Open Drawer -> Admin Profiles
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.adminProfilesFragment));

        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // 2. Scroll to and Click User
        onView(withId(R.id.entrants_recycler_view))
                .perform(RecyclerViewActions.scrollTo(hasDescendant(withText(uniqueName))));

        onView(withText(uniqueName)).perform(click());

        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // 3. Click Delete User
        onView(withId(R.id.btn_delete_user)).perform(click());

        // 4. Confirm
        onView(withText("Delete")).perform(click());

        // 5. Wait for deletion and navigation back
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // 6. Verify we are back on the list
        onView(allOf(
                withText("Profiles"),
                isDescendantOfA(withId(R.id.toolbar))
        )).check(matches(isDisplayed()));

        // 7. Verify User is GONE
        onView(withText(uniqueName)).check(doesNotExist());
    }

    /**
     * Test Notification from Admin to User
     * Flow: Admin Profiles -> Select User -> Notify -> Send -> Verify Firestore
     */
    @Test
    public void testAdminNotifyUser() {
        String uniqueName = "Notify Target " + new Random().nextInt(1000);
        String dummyUserId = "notify_target_" + new Random().nextInt(10000);
        createDummyUser(dummyUserId, uniqueName);

        String uniqueMessageContent = "Admin Warning " + new Random().nextInt(10000);
        String expectedFullMessage = "Message from the admin: " + uniqueMessageContent;

        // 1. Navigate to Profiles
        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.adminProfilesFragment));

        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // 2. Click User
        onView(withId(R.id.entrants_recycler_view))
                .perform(RecyclerViewActions.scrollTo(hasDescendant(withText(uniqueName))));
        onView(withText(uniqueName)).perform(click());

        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // 3. Click Notify Button
        onView(withId(R.id.btn_notify)).perform(click());

        // 4. Type Message in Dialog
        onView(withHint("Enter message...")).perform(typeText(uniqueMessageContent), closeSoftKeyboard());

        // 5. Click "Notify"
        onView(withText("Notify")).perform(click());

        // 6. Wait for DB write
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // 7. Verify Firestore
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<List<Notification>> resultRef = new AtomicReference<>();

        db.collection("notifications")
                .whereEqualTo("recipientId", dummyUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Notification> notifs = querySnapshot.toObjects(Notification.class);
                    resultRef.set(notifs);
                    latch.countDown();
                })
                .addOnFailureListener(e -> latch.countDown());

        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { e.printStackTrace(); }

        List<Notification> notifications = resultRef.get();
        assertNotNull("Notifications query returned null", notifications);
        assertTrue("Expected at least one notification for this user", notifications.size() > 0);

        boolean messageFound = false;
        for (Notification n : notifications) {
            if (expectedFullMessage.equals(n.getMessage())) {
                messageFound = true;
                assertEquals(currentUser.getUid(), n.getSenderId());
                // Cleanup specific notification
                db.collection("notifications").document(n.getNotificationId()).delete();
            }
        }
        assertTrue("Did not find notification with message: " + expectedFullMessage, messageFound);
    }

    // --- Helpers ---

    private void createFakeImageEntry(String eventId) {
        // Add to cleanup list immediately
        eventsCleanupList.add(eventId);

        CountDownLatch latch = new CountDownLatch(1);
        String tinyBase64 = "R0lGODlhAQABAIAAAAUEBAAAACwAAAAAAQABAAACAkQBADs=";
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("posterImageUrl", tinyBase64);
        eventData.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("events").document(eventId).set(eventData)
                .addOnCompleteListener(t -> latch.countDown());
        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) {}
    }

    private void createDummyUser(String uid, String name) {
        // Add to cleanup list immediately
        usersCleanupList.add(uid);

        CountDownLatch latch = new CountDownLatch(1);
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", "dummy@test.com");
        user.put("admin", false);

        db.collection("users").document(uid).set(user)
                .addOnCompleteListener(t -> latch.countDown());
        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) {}
    }
}