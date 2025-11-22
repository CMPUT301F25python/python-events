package com.example.lotteryevent;

import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.FakeAvailableEventsRepository;
import com.example.lotteryevent.ui.AvailableEventsFragment;
import com.example.lotteryevent.viewmodels.AvailableEventsViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AvailableEventsFragmentTest {

    private FakeAvailableEventsRepository fakeRepository;
    private ReusableTestFragmentFactory fragmentFactory;
    private TestNavHostController navController;

    @Before
    public void setup() {
        fakeRepository = new FakeAvailableEventsRepository();

        // Wire ViewModel to fake repo
        GenericViewModelFactory viewModelFactory = new GenericViewModelFactory();
        viewModelFactory.put(
                AvailableEventsViewModel.class,
                () -> new AvailableEventsViewModel(fakeRepository)
        );

        // Use your reusable fragment factory
        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(
                AvailableEventsFragment.class,
                () -> new AvailableEventsFragment(viewModelFactory)
        );
    }

    /**
     * Helper to launch the fragment and attach a TestNavHostController,
     * same pattern as your UserProfileFragmentTest.
     */
    private FragmentScenario<AvailableEventsFragment> launchFragment() {
        navController = new TestNavHostController(
                ApplicationProvider.getApplicationContext()
        );

        FragmentScenario<AvailableEventsFragment> scenario =
                FragmentScenario.launchInContainer(
                        AvailableEventsFragment.class,
                        null,
                        R.style.Theme_LotteryEvent,
                        fragmentFactory
                );

        scenario.onFragment(fragment -> {
            navController.setGraph(R.navigation.nav_graph);
            navController.setCurrentDestination(R.id.availableEventsFragment);
            Navigation.setViewNavController(fragment.requireView(), navController);
        });

        return scenario;
    }

    @Test
    public void onViewCreated_callsFetchAvailableEventsOnRepository() {
        launchFragment();
        assertTrue(fakeRepository.wasFetchCalled());
    }

    @Test
    public void eventsFromRepository_areShownInRecyclerView() {
        // Arrange fake events
        List<Event> events = new ArrayList<>();
        Event e1 = new Event();
        e1.setEventId("id1");
        e1.setName("Event One");
        events.add(e1);

        Event e2 = new Event();
        e2.setEventId("id2");
        e2.setName("Event Two");
        events.add(e2);

        fakeRepository.setEventsToReturn(events);

        // Act
        FragmentScenario<AvailableEventsFragment> scenario = launchFragment();

        // Assert adapter count
        scenario.onFragment(fragment -> {
            RecyclerView rv = fragment.requireView().findViewById(R.id.events_recycler_view);
            assertNotNull(rv.getAdapter());
            assertEquals(2, rv.getAdapter().getItemCount());
        });

        // If your item layout shows the event name, you can assert via Espresso:
        onView(withText("Event One")).check(matches(isDisplayed()));
        onView(withText("Event Two")).check(matches(isDisplayed()));
    }

    @Test
    public void emptyEventsList_recyclerViewHasNoItems() {
        fakeRepository.setEventsToReturn(new ArrayList<>());

        FragmentScenario<AvailableEventsFragment> scenario = launchFragment();

        scenario.onFragment(fragment -> {
            RecyclerView rv = fragment.requireView().findViewById(R.id.events_recycler_view);
            assertNotNull(rv.getAdapter());
            assertEquals(0, rv.getAdapter().getItemCount());
        });
    }

    /**
     * Instead of checking the Toast, we assert the **UI effect** of an error:
     * the adapter shows an empty list when the repository reports an error.
     */
    @Test
    public void repositoryError_resultsInEmptyRecyclerView() {
        String error = "Failed to load events. Please check your connection.";
        fakeRepository.setError(error);

        FragmentScenario<AvailableEventsFragment> scenario = launchFragment();

        scenario.onFragment(fragment -> {
            RecyclerView rv = fragment.requireView().findViewById(R.id.events_recycler_view);
            assertNotNull(rv.getAdapter());
            // Error path in FakeAvailableEventsRepository emits an empty list
            assertEquals(0, rv.getAdapter().getItemCount());
        });
    }

    @Test
    public void clickValidEvent_navigatesToEventDetails_withEventIdArgument() {
        List<Event> events = new ArrayList<>();
        Event e = new Event();
        e.setEventId("abc123");
        e.setName("My Event");
        events.add(e);

        fakeRepository.setEventsToReturn(events);

        launchFragment();

        // Click first item in RecyclerView
        onView(withId(R.id.events_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Assert navigation
        assertEquals(
                R.id.eventDetailsFragment,
                navController.getCurrentDestination().getId()
        );

        Bundle args = navController
                .getBackStackEntry(R.id.eventDetailsFragment)
                .getArguments();
        assertNotNull(args);
        assertEquals("abc123", args.getString("eventId"));
    }

    @Test
    public void clickEventWithInvalidId_doesNotNavigate() {
        List<Event> events = new ArrayList<>();
        Event e = new Event();
        // invalid ID: null / blank / "null"
        e.setEventId(null);
        e.setName("Broken Event");
        events.add(e);

        fakeRepository.setEventsToReturn(events);

        launchFragment();

        onView(withId(R.id.events_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Still on AvailableEventsFragment (no navigation)
        assertEquals(
                R.id.availableEventsFragment,
                navController.getCurrentDestination().getId()
        );
        // No Toast check here on purpose
    }

    @Test
    public void onCleared_callsRemoveListenerOnRepository() {
        FragmentScenario<AvailableEventsFragment> scenario = launchFragment();

        // Destroy the fragment (and ViewModel) to trigger onCleared()
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED);

        assertTrue(fakeRepository.wasRemoveListenerCalled());
    }
}
