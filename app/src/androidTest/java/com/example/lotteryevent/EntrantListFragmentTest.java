package com.example.lotteryevent;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.data.Notification;
import com.example.lotteryevent.repository.FakeEntrantListRepository;
import com.example.lotteryevent.ui.organizer.EntrantListFragment;
import com.example.lotteryevent.viewmodels.EntrantListViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

/**
 * Instrumented UI test suite for {@link com.example.lotteryevent.ui.organizer.EntrantListFragment}.
 * This class verifies fragment behavior using Espresso, including UI rendering,
 * LiveData observation, and notification dispatch logic. A
 * {@link FakeEntrantListRepository} is injected for deterministic and isolated
 * test scenarios.
 * <p>Tests are executed using the AndroidJUnit4 runner and utilize
 * {@link androidx.fragment.app.testing.FragmentScenario} to launch the fragment
 * in a container.</p>
 */
@RunWith(AndroidJUnit4.class)
public class EntrantListFragmentTest {

    private FakeEntrantListRepository fakeRepository;
    private ReusableTestFragmentFactory fragmentFactory;

    /**
     * Initializes the fake repository, injects it into a ViewModel using a
     * {@link GenericViewModelFactory}, and registers the fragment with a custom
     * {@link ReusableTestFragmentFactory}. This setup ensures that each launched
     * fragment receives controlled test dependencies.
     * <p>Executed before each test case.</p>
     */
    @Before
    public void setup() {
        fakeRepository = new FakeEntrantListRepository();
        GenericViewModelFactory viewModelFactory = new GenericViewModelFactory();
        viewModelFactory.put(EntrantListViewModel.class, () -> new EntrantListViewModel(fakeRepository, "event123", "accepted"));
        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(EntrantListFragment.class, () -> new EntrantListFragment(viewModelFactory));
    }

    /**
     * Helper method for launching {@link EntrantListFragment} inside a test container.
     * Constructs a Bundle with the required navigation arguments ({@code eventId}
     * and {@code status}), and launches the fragment using
     * {@link FragmentScenario#launchInContainer(Class, android.os.Bundle, int, androidx.fragment.app.FragmentFactory)}.
     * @param eventId the event identifier to pass into the fragment arguments
     * @param status the entrant status filter to pass into the fragment arguments
     * @return a FragmentScenario controlling the launched fragment instance
     */
    private FragmentScenario<EntrantListFragment> launchFragment(String eventId, String status) {
        android.os.Bundle args = new android.os.Bundle();
        args.putString("eventId", eventId);
        args.putString("status", status);
        return FragmentScenario.launchInContainer(EntrantListFragment.class, args, R.style.Theme_LotteryEvent, fragmentFactory);
    }

    /**
     * Verifies that entrants are correctly displayed when the fragment loads with
     * a given status. Ensures that:
     * <ul>
     *     <li>the title text is correctly capitalized based on the status,</li>
     *     <li>the loading indicator becomes hidden once data arrives,</li>
     *     <li>the RecyclerView is visible after the data is loaded.</li>
     * </ul>
     */
    @Test
    public void test_fetchEntrantsByStatus() {
        // Prepare fake entrants before launch
        Entrant e1 = new Entrant();
        e1.setUserId("user1");
        Entrant e2 = new Entrant();
        e2.setUserId("user2");
        List<Entrant> entrants = Arrays.asList(e1, e2);
        fakeRepository.setEntrants(entrants);

        launchFragment("event123", "accepted");

        // Title should show "Accepted"
        onView(withId(R.id.entrant_list_title))
                .check(matches(withText("Accepted")));

        // Progress bar should eventually be hidden (visibility != VISIBLE)
        onView(withId(R.id.loading_progress_bar))
                /**
                 * Callback used to manually assert that the loading ProgressBar is no longer
                 * visible. Thrown exceptions are propagated to fail the test when mismatches occur.
                 * @param view the ProgressBar being inspected
                 * @param e any assertion error thrown by Espresso during evaluation
                 */
                .check((view, e) -> {
                    if (e != null) throw e;
                    org.junit.Assert.assertEquals(
                            android.view.View.GONE,
                            view.getVisibility()
                    );
                });

        // RecyclerView is displayed
        onView(withId(R.id.entrants_recycler_view)).check(matches(isDisplayed()));
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
        String eventId = "event123";
        String status = "accepted";

        Entrant e1 = new Entrant();
        e1.setUserId("user1");
        Entrant e2 = new Entrant();
        e2.setUserId("user2");
        fakeRepository.setEntrants(Arrays.asList(e1, e2));

        launchFragment(eventId, status);

        // Click "Send Notification" button
        onView(withId(R.id.send_notification_button)).perform(click());

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
        assertEquals(eventId, calls.get(0).getEventId());
        assertEquals(message, calls.get(0).getMessage());

        assertEquals("user2", calls.get(1).getRecipientId());
        assertEquals(eventId, calls.get(1).getEventId());
        assertEquals(message, calls.get(1).getMessage());

    }
}
