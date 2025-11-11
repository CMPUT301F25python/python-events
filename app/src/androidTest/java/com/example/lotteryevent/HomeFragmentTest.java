package com.example.lotteryevent;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lotteryevent.repository.FakeEventRepository;
import com.example.lotteryevent.viewmodels.HomeViewModelFactory;

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
        // 1. Create the single, reusable factory.
        ReusableTestFragmentFactory factory = new ReusableTestFragmentFactory();

        // 2. Create the dependencies needed for THIS test.
        FakeEventRepository fakeRepository = new FakeEventRepository();
        HomeViewModelFactory viewModelFactory = new HomeViewModelFactory(fakeRepository);

        // 3. Provide the "recipe" for creating a HomeFragment to the factory.
        factory.put(HomeFragment.class, () -> new HomeFragment(viewModelFactory));

        // --- Act ---
        // Launch the fragment, passing our configured factory.
        FragmentScenario.launchInContainer(HomeFragment.class, null, R.style.Theme_LotteryEvent, factory);

        // --- Assert ---
        onView(withText("Event 1")).check(matches(isDisplayed()));
        onView(withText("Event 2")).check(matches(isDisplayed()));
    }
}