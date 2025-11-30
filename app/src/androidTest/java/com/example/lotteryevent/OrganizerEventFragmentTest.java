package com.example.lotteryevent;

import android.os.Bundle;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.FakeOrganizerEventRepository;
import com.example.lotteryevent.ui.organizer.OrganizerEventFragment;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.example.lotteryevent.viewmodels.OrganizerEventViewModel;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented UI Test for {@link OrganizerEventFragment}.
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerEventFragmentTest {

    private static final String TEST_EVENT_ID = "evt_123";
    private FakeOrganizerEventRepository fakeRepository;
    private ReusableTestFragmentFactory fragmentFactory;
    private TestNavHostController navController;

    @Before
    public void setup() {
        fakeRepository = new FakeOrganizerEventRepository();
        GenericViewModelFactory viewModelFactory = new GenericViewModelFactory();
        viewModelFactory.put(OrganizerEventViewModel.class, () -> new OrganizerEventViewModel(fakeRepository));

        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(OrganizerEventFragment.class, () -> new OrganizerEventFragment(viewModelFactory));
    }

    /**
     * Helper method to launch the Fragment with a given eventId and attach a NavController.
     */
    private void launchFragment() {
        Bundle args = new Bundle();
        args.putString("eventId", TEST_EVENT_ID);

        navController = new TestNavHostController(ApplicationProvider.getApplicationContext());

        FragmentScenario<OrganizerEventFragment> scenario = FragmentScenario.launchInContainer(
                OrganizerEventFragment.class, args, R.style.Theme_LotteryEvent, fragmentFactory
        );

        scenario.onFragment(fragment -> {
            navController.setGraph(R.navigation.nav_graph);
            navController.setCurrentDestination(R.id.organizerEventPageFragment);
            Navigation.setViewNavController(fragment.requireView(), navController);
        });
    }

    @Test
    public void eventIsOpen_showsAllActionButtons() {
        // Arrange
        Event openEvent = new Event();
        openEvent.setStatus("open");
        openEvent.setName("Annual Fair");
        fakeRepository.setEventToReturn(openEvent);

        // Act
        launchFragment();

        // Assert
        onView(withId(R.id.event_name_label)).check(matches(withText("Annual Fair")));
        onView(withId(R.id.organizer_button_container)).check(matches(isDisplayed()));
        onView(withId(R.id.btnRunDraw)).check(matches(isDisplayed()));
        onView(withId(R.id.btnFinalize)).check(matches(isDisplayed()));
        onView(withId(R.id.organizer_button_container)).check(matches(withEffectiveVisibility(VISIBLE)));
    }

    @Test
    public void eventIsUpcoming_hidesButtonContainer() {
        // Arrange
        Event upcomingEvent = new Event();
        upcomingEvent.setRegistrationStartDateTime(new Timestamp(System.currentTimeMillis() / 1000 + 86400, 0));
        fakeRepository.setEventToReturn(upcomingEvent);

        // Act
        launchFragment();

        // Assert
        onView(withId(R.id.organizer_button_container)).check(matches(not(isDisplayed())));
    }

    @Test
    public void eventIsFinalized_hidesDrawAndFinalizeButtons() {
        // Arrange
        Event finalizedEvent = new Event();
        finalizedEvent.setStatus("finalized");
        fakeRepository.setEventToReturn(finalizedEvent);

        // Act
        launchFragment();

        // Assert
        onView(withId(R.id.organizer_button_container)).check(matches(isDisplayed()));
        onView(withId(R.id.btnRunDraw)).check(matches(not(isDisplayed())));
        onView(withId(R.id.btnFinalize)).check(matches(not(isDisplayed())));
        onView(withId(R.id.btnExportCSV)).check(matches(isDisplayed()));
    }

    @Test
    public void clickFinalizeButton_showsDialogAndFinalizesEvent() {
        // Arrange: Event is open
        Event openEvent = new Event();
        openEvent.setStatus("open");
        fakeRepository.setEventToReturn(openEvent);
        launchFragment();

        // Act: Click Finalize
        onView(withId(R.id.btnFinalize)).perform(scrollTo(), click());

        // Assert: Dialog appears
        onView(withText("Finalize Event")).check(matches(isDisplayed()));
        onView(withText("Are you sure you want to finalize this event? \n\n" +
                "This will prevent further entrants from joining and invited entrants from accepting their invitations. " +
                "This action cannot be undone."))
                .check(matches(isDisplayed()));

        // Act: Click "Finalize" inside the dialog
        onView(withId(android.R.id.button1))
                .inRoot(isDialog())
                .perform(click());

        // Assert: Repository method was called
        assertTrue("Repository finalizeEvent should have been called", fakeRepository.wasFinalizeCalled);
    }

    @Test
    public void clickFinalizeButton_cancelDialog_doesNotFinalize() {
        // Arrange
        Event openEvent = new Event();
        openEvent.setStatus("open");
        fakeRepository.setEventToReturn(openEvent);
        launchFragment();

        // Act
        onView(withId(R.id.btnFinalize)).perform(scrollTo(), click());
        // Click Cancel (Standard Negative Button is android.R.id.button2)
        onView(withId(android.R.id.button2))
                .inRoot(isDialog())
                .perform(click());

        // Assert
        assertFalse("Repository finalizeEvent should NOT have been called", fakeRepository.wasFinalizeCalled);
    }

    @Test
    public void eventAtCapacity_disablesRunDrawButton() {
        // Arrange
        fakeRepository.setButtonEnabled(false);

        // Act
        launchFragment();

        // Assert
        onView(withId(R.id.btnRunDraw)).check(matches(not(isEnabled())));
    }

    @Test
    public void clickWaitingListButton_navigatesToEntrantList() {
        // Arrange
        launchFragment();

        // Act
        onView(withId(R.id.btnViewEntrants)).perform(click());

        // Assert
        assertEquals(R.id.entrantListFragment, navController.getCurrentDestination().getId());
        Bundle args = navController.getBackStackEntry(R.id.entrantListFragment).getArguments();
        assertNotNull(args);
        assertEquals(TEST_EVENT_ID, args.getString("eventId"));
    }

    @Test
    public void clickRunDrawButton_navigatesToRunDrawFragment() {
        // Arrange
        launchFragment();

        // Act
        onView(withId(R.id.btnRunDraw)).perform(scrollTo(), click());

        // Assert
        assertEquals(R.id.runDrawFragment, navController.getCurrentDestination().getId());
        Bundle args = navController.getBackStackEntry(R.id.runDrawFragment).getArguments();
        assertNotNull(args);
        assertEquals(TEST_EVENT_ID, args.getString("eventId"));
    }

    /**
     * Verifies that when an event already has a posterImageUrl, the button
     * text changes to "Update Poster".
     */
    @Test
    public void eventWithPoster_showsUpdatePosterText() {
        // Arrange
        Event eventWithPoster = new Event();
        eventWithPoster.setStatus("open");
        eventWithPoster.setName("Event With Poster");
        // Any non-empty string indicates a saved poster.
        eventWithPoster.setPosterImageUrl("aGVsbG8="); // "hello" in Base64, value itself doesn't matter here.
        fakeRepository.setEventToReturn(eventWithPoster);

        // Act
        launchFragment();

        // Assert
        onView(withId(R.id.upload_poster_button))
                .check(matches(withText("Update Poster")));
    }

    /**
     * Verifies that when an event has no posterImageUrl, the button text
     * remains "Upload Poster".
     */
    @Test
    public void eventWithoutPoster_showsUploadPosterText() {
        // Arrange
        Event eventWithoutPoster = new Event();
        eventWithoutPoster.setStatus("open");
        eventWithoutPoster.setName("Event Without Poster");
        eventWithoutPoster.setPosterImageUrl(null);  // or ""
        fakeRepository.setEventToReturn(eventWithoutPoster);

        // Act
        launchFragment();

        // Assert
        onView(withId(R.id.upload_poster_button))
                .check(matches(withText("Upload Poster")));
    }
}