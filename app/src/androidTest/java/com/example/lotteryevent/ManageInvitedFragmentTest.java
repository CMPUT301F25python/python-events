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
import com.example.lotteryevent.repository.FakeEntrantListRepository;
import com.example.lotteryevent.ui.organizer.ManageInvitedFragment;
import com.example.lotteryevent.viewmodels.EntrantListViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
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
     * Verifies the cancellation of an invite action.
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
}