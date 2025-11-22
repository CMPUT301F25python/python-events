package com.example.lotteryevent;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lotteryevent.ui.LotteryGuidelinesFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI tests for the {@link LotteryGuidelinesFragment}.
 * <p>
 * This test class verifies that the Lottery Guidelines screen initializes
 * correctly and that its key UI components are visible when the fragment
 * is launched within a navigation graph.
 */
@RunWith(AndroidJUnit4.class)
public class LotteryGuidelinesFragmentTest {

    private ReusableTestFragmentFactory fragmentFactory;
    private TestNavHostController navController;

    /**
     * Configures the test environment before each test execution.
     * <p>
     * This method initializes a {@link ReusableTestFragmentFactory} and
     * registers the {@link LotteryGuidelinesFragment} so that it can be
     * instantiated in a controlled and test-friendly manner.
     */
    @Before
    public void setup() {
        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(LotteryGuidelinesFragment.class, LotteryGuidelinesFragment::new); // Register the fragment
    }

    /**
     * Helper method to launch the {@link LotteryGuidelinesFragment} and
     * attach a {@link TestNavHostController} to its view.
     */
    private void launchFragment() {
        navController = new TestNavHostController(ApplicationProvider.getApplicationContext());

        FragmentScenario<LotteryGuidelinesFragment> scenario = FragmentScenario.launchInContainer(LotteryGuidelinesFragment.class, null, R.style.Theme_LotteryEvent, fragmentFactory);

        /**
         * Attaches a {@link TestNavHostController} to the {@link LotteryGuidelinesFragment}
         * after it has been created in the test container.
         */
        scenario.onFragment(fragment -> {
            navController.setGraph(R.navigation.nav_graph); // Attach the NavController to the fragment's view
            navController.setCurrentDestination(R.id.lotteryGuidelines);
            Navigation.setViewNavController(fragment.requireView(), navController);
        });
    }

    /**
     * UI test that verifies the Lottery Guidelines screen is displayed correctly.
     * <p>
     * This test mimics the behavior of navigating to the {@link LotteryGuidelinesFragment}
     * and asserts that the primary motto TextView is visible.
     */
    @Test
    public void navigateToLotteryGuidelines_displaysMotto() {
        launchFragment(); //inside a test container
        // Assert: The motto TextView is displayed
        Espresso.onView(ViewMatchers.withId(R.id.lottery_guidelines_motto)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }
}
