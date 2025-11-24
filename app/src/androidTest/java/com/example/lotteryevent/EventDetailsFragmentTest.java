package com.example.lotteryevent;

import android.os.Bundle;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import com.example.lotteryevent.data.Entrant;
import com.example.lotteryevent.repository.FakeEventDetailsRepository;
import com.example.lotteryevent.ui.EventDetailsFragment;
import com.example.lotteryevent.viewmodels.EventDetailsViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private UiDevice device;
    private TestNavHostController navController;

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

        // Initialize UI Automator - used due to difficulty in testing dialog buttons
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    /**
     * Helper method to launch the fragment with a TestNavHostController attached.
     * This is required for tests that trigger navigation actions (e.g., "navigateUp" after deletion).
     */
    private void launchFragment() {
        // Create a TestNavHostController
        navController = new TestNavHostController(ApplicationProvider.getApplicationContext());

        // Launch the fragment
        FragmentScenario<EventDetailsFragment> scenario = FragmentScenario.launchInContainer(
                EventDetailsFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory
        );

        // Attach the controller to the fragment's view
        scenario.onFragment(fragment -> {
            // Set the graph so the controller knows where it is (start destination)
            navController.setGraph(R.navigation.nav_graph);

            // Set the current destination to the fragment being tested
            navController.setCurrentDestination(R.id.eventDetailsFragment);

            // Attach the controller to the view
            Navigation.setViewNavController(fragment.requireView(), navController);
        });
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
     * Test Case 11: A regular (non-admin) user CANNOT see the delete button.
     * This is a critical security check.
     */
    @Test
    public void nonAdmin_cannotSeeDeleteButton() {
        // Arrange
        fakeRepository.setIsAdmin(false);

        // Act
        FragmentScenario.launchInContainer(EventDetailsFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Assert
        onView(withId(R.id.btn_remove_event)).check(matches(not(isDisplayed())));
    }

    /**
     * Test Case 12: An admin user CAN see the delete button.
     */
    @Test
    public void admin_seesDeleteButton() {
        // Arrange
        fakeRepository.setIsAdmin(true);

        // Act
        FragmentScenario.launchInContainer(EventDetailsFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        // Assert
        onView(withId(R.id.btn_remove_event)).check(matches(isDisplayed()));
    }

    /**
     * Test Case 13: Admin successfully deletes an event via the dialog (Using UIAutomator).
     */
    @Test
    public void admin_deleteFlow_happyPath() throws Exception {
        // Arrange
        fakeRepository.setIsAdmin(true);
        launchFragment();

        // 1. Click Delete
        onView(withId(R.id.btn_remove_event)).perform(click());

        // 2. Use UIAutomator to find the "Delete" button in the dialog
        UiObject deleteButton = device.findObject(new UiSelector()
                .text("Delete")
                .className("android.widget.Button"));

        if (deleteButton.exists() || deleteButton.waitForExists(2000)) {
            deleteButton.click();
        } else {
            throw new RuntimeException("Could not find Delete button in dialog");
        }

        // 3. Wait briefly for async callback & Verify Repository Update
        Thread.sleep(500);
        Boolean isDeleted = fakeRepository.getIsDeleted().getValue();
        // Use assert to check true (handling nulls safely)
        assert(isDeleted != null && isDeleted);
    }

    /**
     * Test Case 14: Admin cancels the deletion via the dialog (Using UIAutomator).
     */
    @Test
    public void admin_deleteFlow_cancelPath() throws Exception {
        // Arrange
        fakeRepository.setIsAdmin(true);
        launchFragment();

        // 1. Click Delete
        onView(withId(R.id.btn_remove_event)).perform(click());

        // 2. Use UIAutomator to find the "Cancel" button
        UiObject cancelButton = device.findObject(new UiSelector()
                .text("Cancel")
                .className("android.widget.Button"));

        if (cancelButton.exists() || cancelButton.waitForExists(2000)) {
            cancelButton.click();
        } else {
            throw new RuntimeException("Could not find Cancel button in dialog");
        }

        // 3. Verify Repository was NOT updated
        Thread.sleep(500);
        Boolean isDeleted = fakeRepository.getIsDeleted().getValue();
        assert(isDeleted == null || !isDeleted);
    }

}