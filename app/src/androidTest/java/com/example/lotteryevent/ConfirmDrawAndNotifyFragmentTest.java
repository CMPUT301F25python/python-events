package com.example.lotteryevent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.repository.FakeEventRepository;
import com.example.lotteryevent.ui.organizer.ConfirmDrawAndNotifyFragment;
import com.example.lotteryevent.viewmodels.ConfirmDrawAndNotifyViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    }

    /**
     * Test Case 1: User sees waiting, selected, and available space counts when only waiting users exist
     */
    @Test
    public void seesAllCountsAndButtonsForWaiting() {
        // Prepare arguments to pass to the fragment, simulating navigation.
        fragmentArgs = new Bundle();
        fragmentArgs.putString("eventId", "fake-event-id");

        Gson gson = new Gson();
        Map<String, String> oldEntrantsStatus = new HashMap<>();
        ArrayList<String> newChosenEntrants = new ArrayList<>();
        ArrayList<String> newUnchosenEntrants = new ArrayList<>();
        ArrayList<Entrant> inMemoryEntrants = fakeRepository.getInMemoryEntrants();
        for (Entrant entrant : inMemoryEntrants) {
            oldEntrantsStatus.put(String.valueOf(entrant.getUserId()), "waiting");
            newUnchosenEntrants.add(String.valueOf(entrant.getUserId()));
        }
        String oldEntrantsStatusString = gson.toJson(oldEntrantsStatus);
        String newChosenEntrantsString = gson.toJson(newChosenEntrants);
        String newUnchosenEntrantsString = gson.toJson(newUnchosenEntrants);

        fragmentArgs.putString("oldEntrantsStatus", oldEntrantsStatusString);
        fragmentArgs.putString("newChosenEntrants", newChosenEntrantsString);
        fragmentArgs.putString("newUnchosenEntrants", newUnchosenEntrantsString);

        // Act: Launch the fragment.
        FragmentScenario.launchInContainer(ConfirmDrawAndNotifyFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Assert:
        // 1. The list titles are displayed.
        onView(withText(containsString("Draw Successful"))).check(matches(isDisplayed()));
        onView(withText(containsString("Waiting List Count"))).check(matches(isDisplayed()));
        onView(withId(R.id.waiting_list_count)).check(matches(withText("2")));
        onView(withText(containsString("Available Spots"))).check(matches(isDisplayed()));
        onView(withId(R.id.available_space_count)).check(matches(withText("2")));
        onView(withText(containsString("Participants Selected"))).check(matches(isDisplayed()));
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
        // Prepare arguments to pass to the fragment, simulating navigation.
        fragmentArgs = new Bundle();
        fragmentArgs.putString("eventId", "fake-event-id");

        Gson gson = new Gson();
        Map<String, String> oldEntrantsStatus = new HashMap<>();
        ArrayList<String> newChosenEntrants = new ArrayList<>();
        ArrayList<String> newUnchosenEntrants = new ArrayList<>();
        ArrayList<Entrant> inMemoryEntrants = fakeRepository.getInMemoryEntrants();
        for (Entrant entrant : inMemoryEntrants) {
            fakeRepository.updateEntrantAttribute("fake-event-id", entrant.getUserId(), "status", "invited");
            oldEntrantsStatus.put(String.valueOf(entrant.getUserId()), "waiting");
            newChosenEntrants.add(String.valueOf(entrant.getUserId()));
        }
        String oldEntrantsStatusString = gson.toJson(oldEntrantsStatus);
        String newChosenEntrantsString = gson.toJson(newChosenEntrants);
        String newUnchosenEntrantsString = gson.toJson(newUnchosenEntrants);

        fragmentArgs.putString("oldEntrantsStatus", oldEntrantsStatusString);
        fragmentArgs.putString("newChosenEntrants", newChosenEntrantsString);
        fragmentArgs.putString("newUnchosenEntrants", newUnchosenEntrantsString);

        // Act: Launch the fragment.
        FragmentScenario.launchInContainer(ConfirmDrawAndNotifyFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        fakeRepository.fetchEventAndEntrantCounts("fake-event-id");

        // Assert:
        // 1. The list titles are displayed.
        onView(withText(containsString("Waiting List Count"))).check(matches(isDisplayed()));
        onView(withId(R.id.waiting_list_count)).check(matches(withText("0")));
        onView(withText(containsString("Available Spots"))).check(matches(isDisplayed()));
        onView(withId(R.id.available_space_count)).check(matches(withText("0")));
        onView(withText(containsString("Participants Selected"))).check(matches(isDisplayed()));
        onView(withId(R.id.selected_users_count)).check(matches(withText("2")));
    }

    /**
     * Test Case 3: User sees waiting, selected, and available space counts when waiting and selected users exist
     */
    @Test
    public void seesAllCountsAndButtonsForWaitingAndSelected() {
        // Prepare arguments to pass to the fragment, simulating navigation.
        fragmentArgs = new Bundle();
        fragmentArgs.putString("eventId", "fake-event-id");

        Gson gson = new Gson();
        Map<String, String> oldEntrantsStatus = new HashMap<>();
        ArrayList<String> newChosenEntrants = new ArrayList<>();
        ArrayList<String> newUnchosenEntrants = new ArrayList<>();
        ArrayList<Entrant> inMemoryEntrants = fakeRepository.getInMemoryEntrants();
        Entrant singleEntrant = inMemoryEntrants.get(0);
        for (Entrant entrant : inMemoryEntrants) {
            if (singleEntrant == entrant) {
                fakeRepository.updateEntrantAttribute("fake-event-id", entrant.getUserId(), "status", "invited");
                oldEntrantsStatus.put(String.valueOf(entrant.getUserId()), "invited");
                newChosenEntrants.add(String.valueOf(entrant.getUserId()));
            } else {
                oldEntrantsStatus.put(String.valueOf(entrant.getUserId()), "waiting");
                newUnchosenEntrants.add(String.valueOf(entrant.getUserId()));
            }
        }
        String oldEntrantsStatusString = gson.toJson(oldEntrantsStatus);
        String newChosenEntrantsString = gson.toJson(newChosenEntrants);
        String newUnchosenEntrantsString = gson.toJson(newUnchosenEntrants);

        fragmentArgs.putString("oldEntrantsStatus", oldEntrantsStatusString);
        fragmentArgs.putString("newChosenEntrants", newChosenEntrantsString);
        fragmentArgs.putString("newUnchosenEntrants", newUnchosenEntrantsString);

        // Act: Launch the fragment.
        FragmentScenario.launchInContainer(ConfirmDrawAndNotifyFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Assert:
        // 1. The list titles are displayed.
        onView(withText(containsString("Waiting List Count"))).check(matches(isDisplayed()));
        onView(withId(R.id.waiting_list_count)).check(matches(withText("1")));
        onView(withText(containsString("Available Spots"))).check(matches(isDisplayed()));
        onView(withId(R.id.available_space_count)).check(matches(withText("1")));
        onView(withText(containsString("Participants Selected"))).check(matches(isDisplayed()));
        onView(withId(R.id.selected_users_count)).check(matches(withText("1")));
    }
}