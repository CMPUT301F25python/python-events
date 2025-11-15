package com.example.lotteryevent;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lotteryevent.repository.FakeEventRepository;
import com.example.lotteryevent.ui.HomeFragment;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.example.lotteryevent.viewmodels.HomeViewModel;

import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class HomeFragmentTest {

    @Test
    public void successfulDataLoad_displaysEventList() {
        // --- Arrange ---
        // 1. Create the dependencies for our ViewModel.
        FakeEventRepository fakeRepository = new FakeEventRepository();

        // 2. Create the factory for the ViewModel
        GenericViewModelFactory viewModelFactory = new GenericViewModelFactory();

        // 3. Add the recipe for HomeViewModel to the factory.
        viewModelFactory.put(HomeViewModel.class, () -> new HomeViewModel(fakeRepository));

        // 4. Create the factory for the Fragment
        ReusableTestFragmentFactory fragmentFactory = new ReusableTestFragmentFactory();

        // 5. Give the Fragment factory instructions:
        fragmentFactory.put(HomeFragment.class, () -> new HomeFragment(viewModelFactory));

        // --- Act ---
        // Launch the fragment using our fully configured factories.
        FragmentScenario.launchInContainer(HomeFragment.class, null, R.style.Theme_LotteryEvent, fragmentFactory);

        // --- Assert ---
        // Check if the text from our fake data is now visible on the screen.
        onView(withText("Event 1")).check(matches(isDisplayed()));
        onView(withText("Event 2")).check(matches(isDisplayed()));
    }
}