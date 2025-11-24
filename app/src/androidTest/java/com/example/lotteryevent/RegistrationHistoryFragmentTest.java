package com.example.lotteryevent;

import static org.junit.Assert.assertEquals;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lotteryevent.ui.RegistrationHistoryFragment;
import com.example.lotteryevent.ui.UserProfileFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI tests for the {@link RegistrationHistoryFragment}.
 *
 * This test verifies that clicking the "registration history" button
 * on the UserProfileFragment navigates to the RegistrationHistoryFragment.
 */
@RunWith(AndroidJUnit4.class)
public class RegistrationHistoryFragmentTest {

    private ReusableTestFragmentFactory fragmentFactory;
    private TestNavHostController navController;

    /**
     * Configures the test environment before each test execution.
     * Registers the {@link UserProfileFragment} in the reusable factory.
     */
    @Before
    public void setup() {
        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(UserProfileFragment.class, UserProfileFragment::new); // Register the fragment
    }

    /**
     * Helper method to launch the {@link UserProfileFragment} and attach a
     * {@link TestNavHostController} to its view.
     */
    private void launchFragment() {
        navController = new TestNavHostController(ApplicationProvider.getApplicationContext());

        FragmentScenario<UserProfileFragment> scenario =
                FragmentScenario.launchInContainer(UserProfileFragment.class, null, R.style.Theme_LotteryEvent, fragmentFactory);

        scenario.onFragment(fragment -> {
            navController.setGraph(R.navigation.nav_graph);
            navController.setCurrentDestination(R.id.userProfileFragment);
            Navigation.setViewNavController(fragment.requireView(), navController);
        });
    }

    /**
     * UI test that verifies clicking the "registration history" button
     * navigates from UserProfileFragment to RegistrationHistoryFragment.
     */
    @Test
    public void clickRegistrationHistoryButton_navigatesToRegistrationHistory() {
        launchFragment(); // inside a test container
        Espresso.onView(ViewMatchers.withId(R.id.history_button)).perform(ViewActions.click());
        assertEquals(R.id.registrationHistoryFragment, navController.getCurrentDestination().getId());
    }
}
