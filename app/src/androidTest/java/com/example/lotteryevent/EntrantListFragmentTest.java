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

@RunWith(AndroidJUnit4.class)
public class EntrantListFragmentTest {

    private FakeEntrantListRepository fakeRepository;
    private ReusableTestFragmentFactory fragmentFactory;

    @Before
    public void setup() {
        fakeRepository = new FakeEntrantListRepository();
        GenericViewModelFactory viewModelFactory = new GenericViewModelFactory();

        // Inject the Fake Repository
        viewModelFactory.put(EntrantListViewModel.class, () -> new EntrantListViewModel(fakeRepository, "event123"));

        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(EntrantListFragment.class, () -> new EntrantListFragment(viewModelFactory));
    }

    /**
     * Helper method for launching the fragment.
     * Note: 'status' argument was removed because the Fragment logic
     * now defaults to "waiting" and is controlled by Chips, not arguments.
     */
    private FragmentScenario<EntrantListFragment> launchFragment(String eventId) {
        android.os.Bundle args = new android.os.Bundle();
        args.putString("eventId", eventId);
        return FragmentScenario.launchInContainer(EntrantListFragment.class, args, R.style.Theme_LotteryEvent, fragmentFactory);
    }

    @Test
    public void test_displayEntrants_defaultWaiting() {
        // 1. Setup Data: Must set status to "waiting" because that is the default filter
        Entrant e1 = new Entrant();
        e1.setUserId("user1");
        e1.setStatus("waiting"); // CRITICAL: Set status so in-memory filter catches it

        Entrant e2 = new Entrant();
        e2.setUserId("user2");
        e2.setStatus("waiting");

        fakeRepository.setEntrants(Arrays.asList(e1, e2));

        // 2. Launch
        launchFragment("event123");

        // 3. Verify ProgressBar is gone
        onView(withId(R.id.loading_progress_bar))
                .check((view, e) -> {
                    if (e != null) throw e;
                    assertEquals(android.view.View.GONE, view.getVisibility());
                });

        // 4. Verify RecyclerView is visible
        onView(withId(R.id.entrants_recycler_view)).check(matches(isDisplayed()));

        // 5. Verify the new Count TextView is correct
        onView(withId(R.id.entrants_count_text)).check(matches(withText("2 entrants")));
    }

    @Test
    public void test_chipFilterSwitching() {
        // 1. Setup Data: One "waiting", One "accepted"
        Entrant e1 = new Entrant();
        e1.setUserId("user1");
        e1.setStatus("waiting");

        Entrant e2 = new Entrant();
        e2.setUserId("user2");
        e2.setStatus("accepted");

        fakeRepository.setEntrants(Arrays.asList(e1, e2));

        // 2. Launch (Defaults to "Waiting")
        launchFragment("event123");

        // 3. Verify initially we see 1 entrant (the waiting one)
        onView(withId(R.id.entrants_count_text)).check(matches(withText("1 entrant")));

        // 4. Click the "Accepted" Chip
        onView(withId(R.id.chip_accepted)).perform(click());

        // 5. Verify we still see 1 entrant (but now it's the accepted one)
        onView(withId(R.id.entrants_count_text)).check(matches(withText("1 entrant")));
    }

    @Test
    public void test_notifyAll() {
        String eventId = "event123";

        // 1. Setup Data: Entrants must match the current filter ("waiting")
        // to be included in the notification list.
        Entrant e1 = new Entrant();
        e1.setUserId("user1");
        e1.setStatus("waiting");

        Entrant e2 = new Entrant();
        e2.setUserId("user2");
        e2.setStatus("waiting");

        fakeRepository.setEntrants(Arrays.asList(e1, e2));

        launchFragment(eventId);

        // 2. Click "Send Notification" button
        onView(withId(R.id.send_notification_button)).perform(click());

        // 3. Dialog interaction
        onView(withText("Notification Message")).check(matches(isDisplayed()));
        String message = "Test organizer message";
        onView(withHint("Enter message..."))
                .perform(typeText(message), closeSoftKeyboard());

        onView(withText("Notify All")).perform(click());

        // 4. Verify repository received correct calls
        List<Notification> calls = fakeRepository.getNotificationCalls();
        assertEquals(2, calls.size());

        assertEquals("user1", calls.get(0).getRecipientId());
        assertEquals(message, calls.get(0).getMessage());

        assertEquals("user2", calls.get(1).getRecipientId());
        assertEquals(message, calls.get(1).getMessage());
    }
}