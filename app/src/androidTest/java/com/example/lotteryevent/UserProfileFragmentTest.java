package com.example.lotteryevent;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.lotteryevent.repository.FakeUserRepository;
import com.example.lotteryevent.ui.UserProfileFragment;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.example.lotteryevent.viewmodels.UserProfileViewModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class UserProfileFragmentTest {

    private FakeUserRepository fakeRepository;
    private ReusableTestFragmentFactory fragmentFactory;
    private TestNavHostController navController;

    @Before
    public void setup() {
        fakeRepository = new FakeUserRepository();
        GenericViewModelFactory viewModelFactory = new GenericViewModelFactory();
        viewModelFactory.put(UserProfileViewModel.class, () -> new UserProfileViewModel(fakeRepository));
        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(UserProfileFragment.class, () -> new UserProfileFragment(viewModelFactory));
    }

    /**
     * Helper method to launch the fragment and attach the NavController.
     * All UI-related setup for the NavController happens here.
     */
    private void launchFragment() {
        // Create the NavController here, just before launching.
        navController = new TestNavHostController(ApplicationProvider.getApplicationContext());

        FragmentScenario<UserProfileFragment> scenario = FragmentScenario.launchInContainer(UserProfileFragment.class, null, R.style.Theme_LotteryEvent, fragmentFactory);

        // Move all NavController setup inside onFragment, which runs on the UI thread.
        scenario.onFragment(fragment -> {
            // Set the graph and current destination on the main thread
            navController.setGraph(R.navigation.nav_graph);
            navController.setCurrentDestination(R.id.userProfileFragment);

            // Then, attach the fully configured NavController to the fragment's view
            Navigation.setViewNavController(fragment.requireView(), navController);
        });
    }

    @Test
    public void initialData_isDisplayedCorrectly() {
        launchFragment();
        onView(withId(R.id.name_field)).check(matches(withText("Joe Bill")));
        onView(withId(R.id.email_field)).check(matches(withText("jabba.test@example.com")));
        onView(withId(R.id.phone_field)).check(matches(withText("123-456-7890")));
    }

    @Test
    public void updateProfile_success_updatesFields() {
        String newName = "Jane Doe";
        String newEmail = "jane.doe@example.com";
        launchFragment();
        onView(withId(R.id.name_field)).perform(clearText(), typeText(newName), closeSoftKeyboard());
        onView(withId(R.id.email_field)).perform(clearText(), typeText(newEmail), closeSoftKeyboard());
        onView(withId(R.id.update_button)).perform(click());
        onView(withId(R.id.name_field)).check(matches(withText(newName)));
        onView(withId(R.id.email_field)).check(matches(withText(newEmail)));
    }

    @Test
    public void updateProfile_invalidEmail_staysOnFragment() {
        launchFragment();
        String invalidEmail = "not-an-email";
        onView(withId(R.id.email_field)).perform(clearText(), typeText(invalidEmail), closeSoftKeyboard());
        onView(withId(R.id.update_button)).perform(click());
        onView(withId(R.id.update_button)).check(matches(isDisplayed()));
        onView(withId(R.id.email_field)).check(matches(withText(invalidEmail)));
    }

    @Test
    public void updateProfile_emptyName_staysOnFragment() {
        launchFragment();
        onView(withId(R.id.name_field)).perform(clearText(), closeSoftKeyboard());
        onView(withId(R.id.update_button)).perform(click());
        onView(withId(R.id.update_button)).check(matches(isDisplayed()));
    }

    @Test
    public void deleteProfile_clickCancel_dialogDismissesAndDataRemains() {
        launchFragment();
        onView(withId(R.id.delete_button)).perform(click());
        onView(withText("This will clear your profile and delete all associated data. This action cannot be undone. Are you sure?")).check(matches(isDisplayed()));
        onView(withText("No")).perform(click());
        onView(withText("This will clear your profile and delete all associated data. This action cannot be undone. Are you sure?")).check(doesNotExist());
        onView(withId(R.id.update_button)).check(matches(isDisplayed()));
        assertNotNull(fakeRepository.getInMemoryUser());
    }

    @Test
    public void deleteProfile_clickConfirm_deletesUser() {
        launchFragment();

        onView(withId(R.id.delete_button)).perform(click());
        onView(withText("Yes, Delete")).perform(click());

        // Assert that the user was deleted from the data source.
        assertNull(fakeRepository.getInMemoryUser());
    }
}