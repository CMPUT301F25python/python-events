package com.example.lotteryevent;

import androidx.fragment.app.testing.FragmentScenario;
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
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class UserProfileFragmentTest {

    private FakeUserRepository fakeRepository;
    private ReusableTestFragmentFactory fragmentFactory;

    @Before
    public void setup() {
        // --- Arrange ---
        // 1. Create the fake repository that our ViewModel will use.
        fakeRepository = new FakeUserRepository();

        // 2. Create the factory that knows how to create our ViewModel.
        GenericViewModelFactory viewModelFactory = new GenericViewModelFactory();
        viewModelFactory.put(UserProfileViewModel.class, () -> new UserProfileViewModel(fakeRepository));

        // 3. Create the factory that knows how to create our Fragment and inject the ViewModel factory.
        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(UserProfileFragment.class, () -> new UserProfileFragment(viewModelFactory));
    }

    @Test
    public void initialData_isDisplayedCorrectly() {
        // --- Act ---
        // Launch the fragment using our custom factory.
        FragmentScenario.launchInContainer(UserProfileFragment.class, null, R.style.Theme_LotteryEvent, fragmentFactory);

        // --- Assert ---
        // Check that the data from the FakeUserRepository is displayed in the EditText fields.
        onView(withId(R.id.name_field)).check(matches(withText("Joe Bill")));
        onView(withId(R.id.email_field)).check(matches(withText("jabba.test@example.com")));
        onView(withId(R.id.phone_field)).check(matches(withText("123-456-7890")));
    }

    @Test
    public void updateProfile_success_updatesFields() {
        // --- Arrange ---
        String newName = "Jane Doe";
        String newEmail = "jane.doe@example.com";
        FragmentScenario.launchInContainer(UserProfileFragment.class, null, R.style.Theme_LotteryEvent, fragmentFactory);

        // --- Act ---
        // Simulate the user typing new information and clicking the update button.
        onView(withId(R.id.name_field)).perform(clearText(), typeText(newName));
        onView(withId(R.id.email_field)).perform(clearText(), typeText(newEmail));
        onView(withId(R.id.update_button)).perform(click());

        // --- Assert ---
        // The ViewModel gets the update, the FakeRepository updates its LiveData,
        // and the Fragment's observer re-populates the fields. We verify the new text is there.
        onView(withId(R.id.name_field)).check(matches(withText(newName)));
        onView(withId(R.id.email_field)).check(matches(withText(newEmail)));
    }

    @Test
    public void deleteProfile_displaysConfirmationDialog() {
        // --- Arrange ---
        FragmentScenario.launchInContainer(UserProfileFragment.class, null, R.style.Theme_LotteryEvent, fragmentFactory);

        // --- Act ---
        // Simulate the user clicking the delete button.
        onView(withId(R.id.delete_button)).perform(click());

        // --- Assert ---
        // Check that the dialog is displayed by looking for its title and positive button text.
        onView(withText("Delete Profile")).check(matches(isDisplayed()));
        onView(withText("Yes, Delete")).check(matches(isDisplayed()));
    }
}