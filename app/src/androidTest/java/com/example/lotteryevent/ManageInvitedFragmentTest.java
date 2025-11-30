package com.example.lotteryevent;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Notification;
import com.example.lotteryevent.repository.FakeEntrantListRepository;
import com.example.lotteryevent.ui.organizer.ManageInvitedFragment;
import com.example.lotteryevent.viewmodels.EntrantListViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class ManageInvitedFragmentTest {

    private FakeEntrantListRepository fakeRepository;
    private ReusableTestFragmentFactory fragmentFactory;
    private TestNavHostController navController;

    private static final String TEST_EVENT_ID = "event123";
    private static final String TEST_STATUS = "invited";


    /**
     * Sets up the test environment before each test case.
     */
    @Before
    public void setup() {
        // 1. Setup the Fake Repository
        fakeRepository = new FakeEntrantListRepository();
        setupFakeData();

        // 2. Setup ViewModel Factory with dependency injection
        GenericViewModelFactory viewModelFactory = new GenericViewModelFactory();
        viewModelFactory.put(EntrantListViewModel.class,
                () -> new EntrantListViewModel(fakeRepository, TEST_EVENT_ID, TEST_STATUS));

        // 3. Setup Fragment Factory
        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(ManageInvitedFragment.class,
                () -> new ManageInvitedFragment(viewModelFactory));
    }

    /**
     * Populates the fake repository with some sample entrants.
     */
    private void setupFakeData() {
        List<Entrant> entrants = new ArrayList<>();

        Entrant e1 = new Entrant();
        e1.setUserId("user1");
        e1.setUserName("Alice Wonderland");
        e1.setStatus(TEST_STATUS);

        Entrant e2 = new Entrant();
        e2.setUserId("user2");
        e2.setUserName("Bob Builder");
        e2.setStatus(TEST_STATUS);

        entrants.add(e1);
        entrants.add(e2);

        fakeRepository.setEntrants(entrants);
    }

    /**
     * Launches the ManageInvitedFragment for testing.
     */
    private void launchFragment() {
        navController = new TestNavHostController(ApplicationProvider.getApplicationContext());

        // Create arguments bundle (simulating navigation args)
        Bundle args = new Bundle();
        args.putString("eventId", TEST_EVENT_ID);

        FragmentScenario<ManageInvitedFragment> scenario = FragmentScenario.launchInContainer(
                ManageInvitedFragment.class,
                args,
                R.style.Theme_LotteryEvent,
                fragmentFactory
        );

        scenario.onFragment(fragment -> {
            navController.setGraph(R.navigation.nav_graph);
            Navigation.setViewNavController(fragment.requireView(), navController);
        });
    }

    /**
     * Verifies the initial state of the ManageInvitedFragment.
     */
    @Test
    public void initialData_isDisplayedCorrectly() {
        launchFragment();

        // Verify the title is correct (capitalized)
        onView(withId(R.id.titleInvitedUsers)).check(matches(withText("Invited")));

        // Verify RecyclerView is displayed
        onView(withId(R.id.recyclerViewInvited)).check(matches(isDisplayed()));

        // Verify the names exist in the list
        onView(withText("Alice Wonderland")).check(matches(isDisplayed()));
        onView(withText("Bob Builder")).check(matches(isDisplayed()));
    }

    /**
     * Verifies the cancellation of an invite action and its notif send.
     */
    @Test
    public void cancelInvite_updatesRepositoryStatus() {
        launchFragment();

        // 1. Perform the click action
        onView(withId(R.id.recyclerViewInvited))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText("Alice Wonderland")),
                        clickChildViewWithId(R.id.cancelButton)
                ));

        // 2. Get the list directly from the repository's LiveData
        List<Entrant> currentEntrants = fakeRepository.fetchEntrantsByStatus(TEST_EVENT_ID, "invited").getValue();

        // 3. Find Alice and verify her status changed to "waiting"
        Entrant alice = null;
        if (currentEntrants != null) {
            for (Entrant e : currentEntrants) {
                if (e.getUserId().equals("user1")) {
                    alice = e;
                    break;
                }
            }
        }

        assertNotNull("Alice should still be in the database", alice);
        assertEquals("Status should be updated to waiting", "waiting", alice.getStatus());

        // Verify repository received correct calls
        List<Notification> calls = fakeRepository.getNotificationCalls();
        assertEquals(1, calls.size());

        assertEquals(alice.getUserId(), calls.get(0).getRecipientId());
        assertEquals(TEST_EVENT_ID, calls.get(0).getEventId());
        assertEquals("Invitation Update", calls.get(0).getTitle());
        assertEquals("Your invitation to the event something has been withdrawn.", calls.get(0).getMessage());
    }

    /**
     * Verifies the list updates when data changes in the repository.
     */
    @Test
    public void listUpdates_whenDataChanges() {
        launchFragment();

        // Verify initial state
        onView(withText("Alice Wonderland")).check(matches(isDisplayed()));

        // Simulate an external data update (e.g. new data arriving from Firestore)
        List<Entrant> newEntrants = new ArrayList<>();
        Entrant e3 = new Entrant();
        e3.setUserId("user3");
        e3.setUserName("Charlie Chocolate");
        newEntrants.add(e3);

        // Update Fake Repo on UI thread
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            fakeRepository.setEntrants(newEntrants);
        });

        // Check if UI updated
        onView(withText("Charlie Chocolate")).check(matches(isDisplayed()));
    }

    /**
     * Custom ViewAction to click a specific child view (button) inside a RecyclerView item.
     */
    public static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                if (v != null) {
                    v.performClick();
                }
            }
        };
    }

    /**
     * Verifies that the "Notify All" workflow triggers notification calls for all
     * entrants displayed in the fragment. The test:
     * <ul>
     *     <li>injects two mock entrants into the fake repository,</li>
     *     <li>launches the fragment,</li>
     *     <li>opens the notification dialog,</li>
     *     <li>types an organizer message,</li>
     *     <li>presses "Notify All",</li>
     *     <li>asserts that the repository recorded exactly two notification calls
     *         with the correct UID, eventId, and message values.</li>
     * </ul>
     * <p>This test ensures both UI interaction and ViewModel–repository integration
     * behave correctly under test conditions.</p>
     */
    @Test
    public void test_notifyAll() {
        launchFragment();

        // Click "Send Notification" button
        onView(withId(R.id.buttonNotifyInvited)).perform(click());

        // Dialog is shown
        onView(withText("Notification Message")).check(matches(isDisplayed()));

        // Type into the EditText with hint "Enter message..."
        String message = "Test organizer message";
        /**
         * Callback executed when typing text into the dialog's EditText field.
         * Ensures the message input is correctly inserted and that the soft keyboard
         * is closed afterward to allow subsequent interactions.
         * @param message the message being typed into the EditText
         */
        onView(withHint("Enter message..."))
                .perform(typeText(message), closeSoftKeyboard());

        /**
         * Callback triggered when the "Notify All" button inside the dialog is clicked.
         * Commits the organizer message and causes the ViewModel to dispatch simulated
         * notification calls through the fake repository.
         * @param view the button view that was clicked
         */
        onView(withText("Notify All")).perform(click());


        // Verify repository received correct calls
        List<Notification> calls = fakeRepository.getNotificationCalls();
        assertEquals(2, calls.size());

        assertEquals("user1", calls.get(0).getRecipientId());
        assertEquals(TEST_EVENT_ID, calls.get(0).getEventId());
        assertEquals(message, calls.get(0).getMessage());

        assertEquals("user2", calls.get(1).getRecipientId());
        assertEquals(TEST_EVENT_ID, calls.get(1).getEventId());
        assertEquals(message, calls.get(1).getMessage());

    }

    /**
     * Tests that if no entrants in the list, notification dialog is not opened
     */
    @Test
    public void test_notifyNoEntrants() {
        fakeRepository.setEntrants(null);
        launchFragment();

        // Click "Send Notification" button
        onView(withId(R.id.buttonNotifyInvited)).perform(click());

        // Check dialog is not shown
        onView(withText("Notification Message")).check(doesNotExist());
    }
}