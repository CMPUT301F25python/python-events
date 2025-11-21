package com.example.lotteryevent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.not;

import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lotteryevent.repository.FakeEventRepository;
import com.example.lotteryevent.ui.ConfirmDrawAndNotifyFragment;
import com.example.lotteryevent.viewmodels.ConfirmDrawAndNotifyViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for valid UI shown based on which states entrants are in
 */
@RunWith(AndroidJUnit4.class)
public class ConfirmDrawAndNotifyFragmentTest {

    private FakeEventRepository fakeRepository;
    private ReusableTestFragmentFactory fragmentFactory;
    private Bundle fragmentArgs;

    @Before
    public void setup() {
        // --- Arrange ---
        // 1. Create the fake repository that our ViewModel will use.
        fakeRepository = new FakeEventRepository();

        // 2. Create the factory that knows how to create our ViewModel.
        GenericViewModelFactory viewModelFactory = new GenericViewModelFactory();
        viewModelFactory.put(ConfirmDrawAndNotifyViewModel.class, () -> new ConfirmDrawAndNotifyViewModel(fakeRepository, new NotificationCustomManager(ApplicationProvider.getApplicationContext())));

        // 3. Create the factory that knows how to create our Fragment.
        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(ConfirmDrawAndNotifyFragment.class, () -> new ConfirmDrawAndNotifyFragment(viewModelFactory));

        // 4. Prepare arguments to pass to the fragment, simulating navigation.
        fragmentArgs = new Bundle();
        fragmentArgs.putString("eventId", "fake-event-id");
    }

    /**
     * Test Case 1: User sees waiting, selected, and available space counts when only waiting users exist
     */
    @Test
    public void seesAllCountsAndButtonsForWaiting() {
        // Act: Launch the fragment.
        FragmentScenario.launchInContainer(ConfirmDrawAndNotifyFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Assert:
        // 1. The list titles are displayed.
        onView(withText(containsString("Draw Successful!"))).check(matches(isDisplayed()));
        onView(withText(containsString("Waiting List:"))).check(matches(isDisplayed()));
        onView(withId(R.id.waiting_list_count)).check(matches(withText("2")));
        onView(withText(containsString("Spots Available:"))).check(matches(isDisplayed()));
        onView(withId(R.id.available_space_count)).check(matches(withText("2")));
        onView(withText(containsString("Users Selected:"))).check(matches(isDisplayed()));
        onView(withId(R.id.selected_users_count)).check(matches(withText("0")));

        // 2. The correct button is visible.
        onView(withId(R.id.confirm_and_notify_button)).check(matches(withText("Confirm and Notify")));
        onView(withId(R.id.cancel_button)).check(matches(withText("Cancel")));
        // 3. The info text and other buttons are hidden.
        onView(withId(R.id.text_info_message)).check(matches(not(isDisplayed())));
    }

    /**
     * Test Case 2: User sees waiting, selected, and available space counts when only selected users exist
     */
    @Test
    public void seesAllCountsAndButtonsForSelected() {
        // Act: Update entrant status, Launch the fragment.
        fakeRepository.updateEntrantsAttributes("fake-event-id", "status", "waiting", "invited");
        FragmentScenario.launchInContainer(ConfirmDrawAndNotifyFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Assert:
        // 1. The list titles are displayed.
        onView(withText(containsString("Waiting List:"))).check(matches(isDisplayed()));
        onView(withId(R.id.waiting_list_count)).check(matches(withText("0")));
        onView(withText(containsString("Spots Available:"))).check(matches(isDisplayed()));
        onView(withId(R.id.available_space_count)).check(matches(withText("0")));
        onView(withText(containsString("Users Selected:"))).check(matches(isDisplayed()));
        onView(withId(R.id.selected_users_count)).check(matches(withText("2")));
    }
}