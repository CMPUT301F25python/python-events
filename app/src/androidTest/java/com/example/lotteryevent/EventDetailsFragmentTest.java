package com.example.lotteryevent;

import android.os.Bundle;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.repository.FakeEventDetailsRepository;
import com.example.lotteryevent.ui.EventDetailsFragment;
import com.example.lotteryevent.viewmodels.EventDetailsViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.google.firebase.Timestamp;
import android.widget.ImageView;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import java.util.Date;

@RunWith(AndroidJUnit4.class)
public class EventDetailsFragmentTest {

    private FakeEventDetailsRepository fakeRepository;
    private ReusableTestFragmentFactory fragmentFactory;
    private Bundle fragmentArgs;

    @Before
    public void setup() {
        // --- Arrange ---
        // 1. Create the fake repository that our ViewModel will use.
        fakeRepository = new FakeEventDetailsRepository();

        // 2. Create the factory that knows how to create our ViewModel.
        GenericViewModelFactory viewModelFactory = new GenericViewModelFactory();
        viewModelFactory.put(EventDetailsViewModel.class, () -> new EventDetailsViewModel(fakeRepository));

        // 3. Create the factory that knows how to create our Fragment.
        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(EventDetailsFragment.class, () -> new EventDetailsFragment(viewModelFactory));

        // 4. Prepare arguments to pass to the fragment, simulating navigation.
        fragmentArgs = new Bundle();
        fragmentArgs.putString("eventId", "fake-event-id");
    }

    /**
     * Test Case 1: A user who is NOT an entrant sees the "Join Waiting List" button.
     */
    @Test
    public void notAnEntrant_seesJoinButton() {
        // Arrange: The default state of the fake repo is not being an entrant.

        // Act: Launch the fragment.
        FragmentScenario.launchInContainer(EventDetailsFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Assert:
        // 1. The main event details are displayed.
        onView(withText("Name: Default Test Event")).check(matches(isDisplayed()));
        // 2. The correct button is visible.
        onView(withId(R.id.btn_action_positive)).check(matches(withText("Join Waiting List")));
        // 3. The info text and other buttons are hidden.
        onView(withId(R.id.text_info_message)).check(matches(not(isDisplayed())));
        onView(withId(R.id.btn_action_negative)).check(matches(not(isDisplayed())));
    }

    /**
     * Test Case 2: A user who has been invited sees the "Accept" and "Decline" buttons.
     */
    @Test
    public void invitedEntrant_seesAcceptAndDeclineButtons() {
        // Arrange: Set the initial state of the fake repository to be "invited".
        Entrant invitedEntrant = new Entrant();
        invitedEntrant.setStatus("invited");
        fakeRepository.setInitialEntrant(invitedEntrant);

        // Act: Launch the fragment.
        FragmentScenario.launchInContainer(EventDetailsFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Assert:
        // 1. The two-button container is visible.
        onView(withId(R.id.button_actions_container)).check(matches(isDisplayed()));
        // 2. Both buttons have the correct text.
        onView(withId(R.id.btn_action_positive)).check(matches(withText("Accept Invitation")));
        onView(withId(R.id.btn_action_negative)).check(matches(withText("Decline Invitation")));
        // 3. The info text is hidden.
        onView(withId(R.id.text_info_message)).check(matches(not(isDisplayed())));
    }

    /**
     * Test Case 3: A user who has already accepted sees the "Accepted" info text.
     */
    @Test
    public void acceptedEntrant_seesInfoText() {
        // Arrange: Set the initial state of the fake repository to be "accepted".
        Entrant acceptedEntrant = new Entrant();
        acceptedEntrant.setStatus("accepted");
        fakeRepository.setInitialEntrant(acceptedEntrant);

        // Act: Launch the fragment.
        FragmentScenario.launchInContainer(EventDetailsFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Assert:
        // 1. The info text view is visible and has the correct message.
        onView(withId(R.id.text_info_message))
                .check(matches(withText("Invitation accepted! You are attending.")));
        // 2. The buttons are hidden.
        onView(withId(R.id.button_actions_container)).check(matches(not(isDisplayed())));
    }

    /**
     * Test Case 4: A user clicks the "Join Waiting List" button and the UI updates to "Leave Waiting List".
     */
    @Test
    public void clickJoinButton_uiUpdatesToLeaveButton() {
        // Arrange: Start with the default state (not an entrant).
        FragmentScenario.launchInContainer(EventDetailsFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Sanity check: ensure the initial state is correct.
        onView(withId(R.id.btn_action_positive)).check(matches(withText("Join Waiting List")));

        // Act: Simulate the user clicking the button.
        onView(withId(R.id.btn_action_positive)).perform(click());

        // Assert: The ViewModel should have updated the state, and the UI should have reacted.
        // The button text should now be "Leave Waiting List".
        onView(withId(R.id.btn_action_positive)).check(matches(withText("Leave Waiting List")));
    }

    /**
    * Test Case 5: A user who has previously declined the invitation sees the "Declined" info text.
    */
    @Test
    public void declinedEntrant_seesInfoText() {
        // Arrange: Set the initial state of the fake repository to be "declined".
        Entrant declinedEntrant = new Entrant();
        declinedEntrant.setStatus("declined");
        fakeRepository.setInitialEntrant(declinedEntrant);

        // Act: Launch the fragment.
        FragmentScenario.launchInContainer(EventDetailsFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Assert:
        // 1. The info text view is visible and has the correct message.
        onView(withId(R.id.text_info_message))
                .check(matches(withText("You have declined this invitation.")));
        // 2. The buttons are hidden.
        onView(withId(R.id.button_actions_container)).check(matches(not(isDisplayed())));
    }

    /**
     * Test Case 6: A user whose invitation was cancelled by the organizer sees the "Cancelled" info text.
     */
    @Test
    public void cancelledEntrant_seesInfoText() {
        // Arrange: Set the initial state of the fake repository to be "cancelled".
        Entrant cancelledEntrant = new Entrant();
        cancelledEntrant.setStatus("cancelled");
        fakeRepository.setInitialEntrant(cancelledEntrant);

        // Act: Launch the fragment.
        FragmentScenario.launchInContainer(EventDetailsFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Assert:
        // 1. The info text view is visible and has the correct message.
        onView(withId(R.id.text_info_message))
                .check(matches(withText("Your invitation was cancelled.")));
        // 2. The buttons are hidden.
        onView(withId(R.id.button_actions_container)).check(matches(not(isDisplayed())));
    }

    /**
     * Test Case 7: The event is finalized.
     * Verifies that the user sees an info message indicating the event is closed.
     */
    @Test
    public void finalizedEvent_seesInfoText() {
        // Arrange: Modify the in-memory event in the fake repository.
        fakeRepository.getInMemoryEvent().setStatus("finalized");

        // Act
        FragmentScenario.launchInContainer(EventDetailsFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Assert
        onView(withId(R.id.text_info_message))
                .check(matches(withText("This event is now closed.")));
        onView(withId(R.id.button_actions_container)).check(matches(not(isDisplayed())));
    }

    /**
     * Test Case 8: Registration for an event has not yet opened.
     * Verifies the user sees an appropriate info message.
     */
    @Test
    public void registrationNotYetOpen_seesInfoText() {
        // Arrange: Set the registration start date to be in the future.
        fakeRepository.getInMemoryEvent().setRegistrationStartDateTime(
                new Timestamp(new Date(System.currentTimeMillis() + 100000)) // 100 seconds in the future
        );

        // Act
        FragmentScenario.launchInContainer(EventDetailsFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Assert
        onView(withId(R.id.text_info_message))
                .check(matches(withText("Registration has not yet opened.")));
        onView(withId(R.id.button_actions_container)).check(matches(not(isDisplayed())));
    }

    /**
     * Test Case 9: Registration for an event is closed (past the end date).
     * Verifies the user sees the "registration closed" message.
     */
    @Test
    public void registrationIsClosed_seesInfoText() {
        // Arrange: Set both start and end dates to be in the past.
        fakeRepository.getInMemoryEvent().setRegistrationStartDateTime(
                new Timestamp(new Date(System.currentTimeMillis() - 200000))
        );
        fakeRepository.getInMemoryEvent().setRegistrationEndDateTime(
                new Timestamp(new Date(System.currentTimeMillis() - 100000))
        );

        // Act
        FragmentScenario.launchInContainer(EventDetailsFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Assert
        onView(withId(R.id.text_info_message))
                .check(matches(withText("Registration for this event is closed.")));
        onView(withId(R.id.button_actions_container)).check(matches(not(isDisplayed())));
    }

    /**
     * Test Case 10: The event and waiting list are both full.
     * Verifies the user sees the "full" message instead of a join button.
     */
    @Test
    public void eventAndWaitingListAreFull_seesInfoText() {
        // Arrange
        // 1. Set a capacity and waiting list limit on the event.
        fakeRepository.getInMemoryEvent().setCapacity(1);
        fakeRepository.getInMemoryEvent().setWaitingListLimit(1);

        // 2. Add entrants to fill both the attendee and waiting list spots.
        Entrant attendee = new Entrant();
        attendee.setUserId("attendee-1");
        attendee.setStatus("accepted");
        fakeRepository.addInitialEntrant(attendee); // Assumes you add a helper method

        Entrant waiter = new Entrant();
        waiter.setUserId("waiter-1");
        waiter.setStatus("waiting");
        fakeRepository.addInitialEntrant(waiter); // Assumes you add a helper method

        // Act
        FragmentScenario.launchInContainer(EventDetailsFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Assert
        onView(withId(R.id.text_info_message))
                .check(matches(withText("This event and its waiting list are full.")));
        onView(withId(R.id.button_actions_container)).check(matches(not(isDisplayed())));
    }

    /**
     * Test Case 11: Event requires geolocation.
     * Verifies that clicking "Join" for an event requiring geolocation will succeed
     * if the user has granted permission.
     */
    @Test
    public void joinEvent_locationRequired_waitsForPermission() {
        // 1. Arrange: Require Geolocation
        fakeRepository.getInMemoryEvent().setGeoLocationRequired(true);

        // 2. PRE-GRANT PERMISSION
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("pm grant " + androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName() + " android.permission.ACCESS_FINE_LOCATION");

        // 3. Act: Launch and Click
        FragmentScenario.launchInContainer(EventDetailsFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);
        onView(withId(R.id.btn_action_positive)).perform(click());

        // 4. Assert
        onView(withId(R.id.btn_action_positive)).check(matches(withText("Leave Waiting List")));
    }

    /**
     * Test Case 12: Event does NOT require geolocation.
     * Verifies that clicking "Join" joins the list immediately.
     */
    @Test
    public void joinEvent_locationNotRequired_joinsImmediately() {
        // Arrange
        fakeRepository.getInMemoryEvent().setGeoLocationRequired(false);

        // Act
        FragmentScenario.launchInContainer(EventDetailsFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);
        onView(withId(R.id.btn_action_positive)).perform(click());

        // Assert
        // Since no permission was needed, the join happened immediately.
        onView(withId(R.id.btn_action_positive)).check(matches(withText("Leave Waiting List")));
    }

    /**
     * Test Case 13: When an event has no poster image set, the details page
     * shows a placeholder image in the poster ImageView.
     */
    @Test
    public void eventWithoutPoster_showsPlaceholderImage() {
        // Arrange: ensure the in-memory event has no poster image.
        fakeRepository.resetToDefaultState();
        fakeRepository.getInMemoryEvent().setPosterImageUrl(null);

        // Act: launch the fragment.
        FragmentScenario<EventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(
                        EventDetailsFragment.class,
                        fragmentArgs,
                        R.style.Theme_LotteryEvent,
                        fragmentFactory
                );

        // Assert: the poster ImageView is present and has a drawable (placeholder).
        scenario.onFragment(fragment -> {
            ImageView imageView = fragment.requireView().findViewById(R.id.event_poster_image);
            assertNotNull("Poster ImageView should not be null", imageView);

            Drawable drawable = imageView.getDrawable();
            assertNotNull("Placeholder drawable should be set when no poster is available", drawable);
        });
    }

    /**
     * Test Case 14: When an event has a poster image set (Base64),
     * the details page displays it as a bitmap in the poster ImageView.
     */
    @Test
    public void eventWithPoster_showsPosterBitmap() {
        // Arrange: give the in-memory event a small valid Base64 PNG.
        fakeRepository.resetToDefaultState();
        // 1x1 transparent PNG (small, valid Base64)
        String base64Png =
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAA" +
                        "AAC0lEQVR4nGNgYAAAAAMAAWgmWQ0AAAAASUVORK5CYII=";

        fakeRepository.getInMemoryEvent().setPosterImageUrl(base64Png);

        // Act: launch the fragment.
        FragmentScenario<EventDetailsFragment> scenario =
                FragmentScenario.launchInContainer(
                        EventDetailsFragment.class,
                        fragmentArgs,
                        R.style.Theme_LotteryEvent,
                        fragmentFactory
                );

        // Assert: the poster ImageView now has a BitmapDrawable.
        scenario.onFragment(fragment -> {
            ImageView imageView = fragment.requireView().findViewById(R.id.event_poster_image);
            assertNotNull("Poster ImageView should not be null", imageView);

            Drawable drawable = imageView.getDrawable();
            assertNotNull("Poster drawable should be set for events with a poster", drawable);
            assertTrue(
                    "Poster drawable should be a BitmapDrawable when Base64 poster is provided",
                    drawable instanceof BitmapDrawable
            );
        });
    }

}