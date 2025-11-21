package com.example.lotteryevent;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lotteryevent.data.Entrant;
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

@RunWith(AndroidJUnit4.class)
public class EntrantListFragmentTest {

    private FakeEntrantListRepository fakeRepository;
    private ReusableTestFragmentFactory fragmentFactory;

    @Before
    public void setup() {
        fakeRepository = new FakeEntrantListRepository();

        GenericViewModelFactory viewModelFactory = new GenericViewModelFactory();
        viewModelFactory.put(EntrantListViewModel.class, () -> new EntrantListViewModel(fakeRepository));

        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(EntrantListFragment.class, () -> new EntrantListFragment(viewModelFactory));
    }

    private FragmentScenario<EntrantListFragment> launchFragment(String eventId, String status) {
        // Build args bundle similar to Safe Args (keys must match your nav graph)
        android.os.Bundle args = new android.os.Bundle();
        args.putString("eventId", eventId);
        args.putString("status", status);

        return FragmentScenario.launchInContainer(
                EntrantListFragment.class,
                args,
                R.style.Theme_LotteryEvent,
                fragmentFactory
        );
    }

    /**
     * checking that entrants with correct status are shown when respective box is clicked
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
     * testing that all users for a specific notification status are notified
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
        onView(withHint("Enter message..."))
                .perform(typeText(message), closeSoftKeyboard());

        // Click "Notify All"
        onView(withText("Notify All")).perform(click());

        // Verify repository received correct calls
        List<FakeEntrantListRepository.NotificationCall> calls = fakeRepository.getNotificationCalls();
        assertEquals(2, calls.size());

        assertEquals("user1", calls.get(0).uid);
        assertEquals(eventId, calls.get(0).eventId);
        assertEquals(message, calls.get(0).message);

        assertEquals("user2", calls.get(1).uid);
        assertEquals(eventId, calls.get(1).eventId);
        assertEquals(message, calls.get(1).message);
    }
}
