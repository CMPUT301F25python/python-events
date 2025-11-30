package com.example.lotteryevent;

import android.os.Bundle;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lotteryevent.data.Notification;
import com.example.lotteryevent.repository.FakeNotificationRepository;
import com.example.lotteryevent.ui.NotificationsFragment;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.example.lotteryevent.viewmodels.NotificationsViewModel;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented Unit Test for {@link NotificationsFragment}.
 * <p>
 * This class tests the UI logic, data display, and navigation behavior of the notifications screen.
 * It utilizes a {@link FakeNotificationRepository} to isolate the test from Firestore and
 * uses {@link TestNavHostController} to verify navigation actions without launching the full Activity.
 */
@RunWith(AndroidJUnit4.class)
public class NotificationsFragmentTest {

    private FakeNotificationRepository fakeRepository;
    private ReusableTestFragmentFactory fragmentFactory;
    private TestNavHostController navController;

    /**
     * Sets up the test environment before each test execution.
     * <p>
     * 1. Initializes the FakeRepository.
     * 2. Sets up the ViewModelFactory to inject the fake repository into the ViewModel.
     * 3. Sets up the FragmentFactory to inject the ViewModelFactory into the Fragment.
     */
    @Before
    public void setup() {
        fakeRepository = new FakeNotificationRepository();
        GenericViewModelFactory viewModelFactory = new GenericViewModelFactory();
        viewModelFactory.put(NotificationsViewModel.class, () -> new NotificationsViewModel(fakeRepository));
        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(NotificationsFragment.class, () -> new NotificationsFragment(viewModelFactory));
    }

    /**
     * Helper method to launch the Fragment in an isolated container and attach the Navigation Controller.
     * <p>
     * Because Fragments manipulate the {@link androidx.navigation.NavController}, we must attach a
     * {@link TestNavHostController} to the Fragment's view immediately after creation. This allows
     * us to verify navigation events (e.g., checking current destination ID).
     *
     * @param args A {@link Bundle} of arguments to pass to the fragment (can be null).
     */
    private void launchFragment(Bundle args) {
        navController = new TestNavHostController(ApplicationProvider.getApplicationContext());

        // Launch the fragment using the custom factory
        FragmentScenario<NotificationsFragment> scenario = FragmentScenario.launchInContainer(
                NotificationsFragment.class, args, R.style.Theme_LotteryEvent, fragmentFactory
        );

        // Execute on the main thread to attach the NavController to the view
        scenario.onFragment(fragment -> {
            navController.setGraph(R.navigation.nav_graph);
            navController.setCurrentDestination(R.id.notificationsFragment);
            Navigation.setViewNavController(fragment.requireView(), navController);
        });
    }

    /**
     * Verifies that notifications are displayed correctly in the RecyclerView.
     * <p>
     * Since the {@code NotificationAdapter} constructs messages dynamically (e.g., adding emojis
     * or standard prefixes based on the notification type), this test checks for substrings
     * within the displayed text rather than exact matches of raw data fields.
     */
    @Test
    public void displayNotifications_showsFormattedMessages() {
        // --- Arrange ---
        List<Notification> data = new ArrayList<>();

        // 1. Lottery Win: The adapter formats this specific type heavily.
        Notification n1 = new Notification();
        n1.setNotificationId("1");
        n1.setType("lottery_win");
        n1.setEventName("Super Lotto");
        n1.setSeen(false);
        n1.setTimestamp(Timestamp.now());

        // 2. Generic: The adapter uses "Message from organizer..." format.
        Notification n2 = new Notification();
        n2.setNotificationId("2");
        n2.setType("generic");
        n2.setEventName("Town Hall");
        n2.setSeen(false);
        n2.setMessage("Doors open at 5pm.");
        n2.setTimestamp(Timestamp.now());

        data.add(n1);
        data.add(n2);
        fakeRepository.setNotifications(data);

        // --- Act ---
        launchFragment(null);

        // --- Assert ---
        // Use 'containsString' to match the meaningful parts of the text generated by the adapter.
        onView(withId(R.id.notifications_recycler_view))
                .check(matches(hasDescendant(withText(containsString("selected for Super Lotto")))));

        onView(withId(R.id.notifications_recycler_view))
                .check(matches(hasDescendant(withText(containsString("Doors open at 5pm")))));
    }

    /**
     * Verifies that clicking a "Lottery Win" notification triggers navigation to the Event Details screen.
     * <p>
     * It checks:
     * 1. The navigation controller's current destination changes to {@code eventDetailsFragment}.
     * 2. The correct {@code eventId} is passed in the navigation bundle.
     */
    @Test
    public void clickLotteryWin_navigatesToEventDetails() {
        // --- Arrange ---
        Notification n1 = new Notification();
        n1.setNotificationId("win_1");
        n1.setType("lottery_win");
        n1.setEventId("event_999");
        n1.setEventName("Big Event");
        n1.setSeen(false);
        n1.setTimestamp(Timestamp.now());

        List<Notification> data = new ArrayList<>();
        data.add(n1);
        fakeRepository.setNotifications(data);

        launchFragment(null);

        // --- Act ---
        // Click the item based on the text generated by the adapter.
        onView(withText(containsString("selected for Big Event"))).perform(click());

        // --- Assert ---
        // Verify navigation destination
        assertEquals(R.id.eventDetailsFragment, navController.getCurrentDestination().getId());

        // Verify argument passing
        Bundle arguments = navController.getBackStackEntry(R.id.eventDetailsFragment).getArguments();
        assertNotNull("Arguments should not be null", arguments);
        assertEquals("event_999", arguments.getString("eventId"));
    }

    /**
     * Verifies that clicking a standard notification (e.g., "event_update") does NOT trigger navigation.
     * The user should remain on the Notifications screen.
     */
    @Test
    public void clickStandardNotification_doesNotNavigate() {
        // --- Arrange ---
        Notification n1 = new Notification();
        n1.setNotificationId("info_1");
        n1.setType("lottery_loss"); // Different type that implies no navigation
        n1.setEventName("Waitlist Event");
        n1.setSeen(false);
        n1.setTimestamp(Timestamp.now());

        List<Notification> data = new ArrayList<>();
        data.add(n1);
        fakeRepository.setNotifications(data);

        launchFragment(null);

        // --- Act ---
        onView(withText(containsString("You weren't selected"))).perform(click());

        // --- Assert ---
        // Destination should remain 'notificationsFragment'
        assertEquals(R.id.notificationsFragment, navController.getCurrentDestination().getId());
    }

    /**
     * Verifies the business logic side-effect: Clicking any notification should mark it as 'seen'
     * in the repository.
     */
    @Test
    public void clickItem_marksAsSeenInRepository() {
        // --- Arrange ---
        Notification n1 = new Notification();
        n1.setNotificationId("123");
        n1.setType("generic");
        n1.setEventName("Test Event");
        n1.setMessage("Test Message");
        n1.setSeen(false); // Initially unseen
        n1.setTimestamp(Timestamp.now());

        List<Notification> data = new ArrayList<>();
        data.add(n1);
        fakeRepository.setNotifications(data);

        launchFragment(null);

        // --- Act ---
        onView(withText(containsString("Test Message"))).perform(click());

        // --- Assert ---
        // Access the in-memory list in the fake repo to verify the update.
        Notification updated = fakeRepository.getInMemoryList().get(0);
        assertTrue("Notification should be marked as seen after clicking", updated.getSeen());
    }

    /**
     * Verifies startup logic: If the Fragment is launched with a specific "notificationId" argument
     * (simulating a tap on a system tray notification or deep link), that notification should
     * automatically be marked as seen without further user interaction.
     */
    @Test
    public void initialArgument_marksNotificationAsSeen() {
        // --- Arrange ---
        Notification n1 = new Notification();
        n1.setNotificationId("target_id_555");
        n1.setType("generic");
        n1.setEventName("Deep Link Event");
        n1.setMessage("Deep Link Message");
        n1.setSeen(false);
        n1.setTimestamp(Timestamp.now());

        List<Notification> data = new ArrayList<>();
        data.add(n1);
        fakeRepository.setNotifications(data);

        // Prepare arguments simulating a deep link launch
        Bundle args = new Bundle();
        args.putString("notificationId", "target_id_555");

        // --- Act ---
        launchFragment(args);

        // --- Assert ---
        // The ViewModel should handle the argument in 'processInitialNotification' immediately.
        Notification updated = fakeRepository.getInMemoryList().get(0);
        assertTrue("Initial notification passed via arguments should be marked seen", updated.getSeen());
    }
}