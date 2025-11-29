package com.example.lotteryevent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;

import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.example.lotteryevent.data.Notification;
import com.example.lotteryevent.repository.FakeEventDetailsRepository;
import com.example.lotteryevent.ui.EventDetailsFragment;
import com.example.lotteryevent.ui.admin.AdminUserProfileFragment;
import com.example.lotteryevent.viewmodels.AdminProfilesViewModel;
import com.example.lotteryevent.viewmodels.EventDetailsViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Instrumented tests for the {@link AdminUserProfileFragment}.
 * <p>
 * This class uses {@link FragmentScenario} to launch the fragment in isolation and {@link androidx.test.espresso.Espresso}
 * to verify the UI components. It tests the visibility of key elements like the delete button and
 * section headers, as well as the functionality of the deletion confirmation dialog.
 * </p>
 */
@RunWith(AndroidJUnit4.class)
public class AdminUserProfileFragmentTest {

    private FakeEventDetailsRepository fakeRepository;
    private ReusableTestFragmentFactory fragmentFactory;

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
        fragmentFactory.put(AdminUserProfileFragment.class, () -> new AdminUserProfileFragment(viewModelFactory));
    }

    /**
     * Verifies that the fragment launches correctly with a provided user ID, displaying
     * the "Delete User" button and the relevant event history headers.
     */
    @Test
    public void testPageLaunchesAndShowsNotifyDeleteButton() {
        // 1. Prepare arguments
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putString("userId", "test_user_123");

        // 2. Launch the fragment with arguments
        FragmentScenario.launchInContainer(AdminUserProfileFragment.class, fragmentArgs);

        // 3. Verify the Notify and Delete buttons are visible
        onView(withId(R.id.btn_delete_user))
                .check(matches(isDisplayed()));
        onView(withId(R.id.btn_notify))
                .check(matches(isDisplayed()));

        // 4. Verify the headers are visible (Events Organized/Attended)
        onView(withText("Events Organized")).check(matches(isDisplayed()));
        onView(withText("Events Attended")).check(matches(isDisplayed()));
    }


    /**
     * Verifies that clicking the "Delete User" button triggers an {@link android.app.AlertDialog}
     * with the correct confirmation message and action buttons.
     */
    @Test
    public void testDeleteButton_ShowsConfirmationDialog() {
        // 1. Prepare arguments
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putString("userId", "test_user_123");

        // 2. Launch fragment
        FragmentScenario.launchInContainer(AdminUserProfileFragment.class, fragmentArgs);

        // 3. Click the Delete button
        onView(withId(R.id.btn_delete_user)).perform(click());

        // 4. Verify the Dialog appears
        // Check for the title
        onView(withText("Delete User?")).check(matches(isDisplayed()));

        // Check for part of the message content
        onView(withText(containsString("Are you sure you want to delete")))
                .check(matches(isDisplayed()));

        // Check for Positive/Negative buttons
        onView(withText("Delete")).check(matches(isDisplayed()));
        onView(withText("Cancel")).check(matches(isDisplayed()));
    }

    /**
     * Verifies that clicking "Cancel" inside the deletion dialog dismisses the dialog
     * and returns the user to the fragment view without performing any action.
     */
    @Test
    public void testCancelButton_DismissesDialog() {
        // 1. Launch and open dialog
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putString("userId", "test_user_123");
        FragmentScenario.launchInContainer(AdminUserProfileFragment.class, fragmentArgs);

        onView(withId(R.id.btn_delete_user)).perform(click());

        // 2. Click "Cancel"
        onView(withText("Cancel")).perform(click());

        // 3. Verify the Delete button is visible again (back on the fragment)
        onView(withId(R.id.btn_delete_user)).check(matches(isDisplayed()));
    }

    /**
     * Verifies that clicking the "Send Notification" button triggers an {@link android.app.AlertDialog}
     * with the correct title and action buttons.
     */
    @Test
    public void testNotifyButton_ShowsConfirmationDialog() {
        // 1. Prepare arguments
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putString("userId", "test_user_123");

        // 2. Launch fragment
        FragmentScenario.launchInContainer(AdminUserProfileFragment.class, fragmentArgs);

        // 3. Click the Delete button
        onView(withId(R.id.btn_notify)).perform(click());

        // 4. Verify the Dialog appears
        // Check for the title
        onView(withText("Notification Message")).check(matches(isDisplayed()));

        // Check for Positive/Negative buttons
        onView(withText("Notify")).check(matches(isDisplayed()));
        onView(withText("Cancel")).check(matches(isDisplayed()));
    }

    /**
     * Verifies that clicking "Cancel" inside the send notification dialog dismisses the dialog
     * and returns the user to the fragment view without performing any action.
     */
    @Test
    public void testNotifyButton_DismissesDialog() {
        // 1. Launch and open dialog
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putString("userId", "test_user_123");
        FragmentScenario.launchInContainer(AdminUserProfileFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        onView(withId(R.id.btn_notify)).perform(click());

        // 2. Click "Cancel"
        onView(withText("Cancel")).perform(click());

        // 3. Verify the Send Notification button is visible again (back on the fragment)
        onView(withId(R.id.btn_notify)).check(matches(isDisplayed()));

        List<Notification> calls = fakeRepository.getNotificationCalls();
        assertEquals(calls.size(), 0);
    }

    /**
     * Verifies that clicking "Notify" inside the send notification dialog dismisses the dialog
     * and returns the user to the fragment view and sends notification (call).
     */
    @Test
    public void testNotifyButton_SendsNotif() {
        // 1. Launch and open dialog
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putString("userId", "test_user_123");
        FragmentScenario.launchInContainer(AdminUserProfileFragment.class, fragmentArgs, R.style.Theme_LotteryEvent, fragmentFactory);

        onView(withId(R.id.btn_notify)).perform(click());

        // Dialog is shown
        onView(withText("Notification Message")).check(matches(isDisplayed()));

        // Type into the EditText with hint "Enter message..."
        String message = "Test admin message";
        /**
         * Callback executed when typing text into the dialog's EditText field.
         * Ensures the message input is correctly inserted and that the soft keyboard
         * is closed afterward to allow subsequent interactions.
         * @param message the message being typed into the EditText
         */
        onView(withHint("Enter message..."))
                .perform(typeText(message), closeSoftKeyboard());

        /**
         * Callback triggered when the "Notify All" button inside the dialog is clicked.
         * Commits the organizer message and causes the ViewModel to dispatch simulated
         * notification calls through the fake repository.
         * @param view the button view that was clicked
         */
        onView(withText("Notify")).perform(click());

        // 2. Verify the Send Notification button is visible again (back on the fragment)
        onView(withId(R.id.btn_notify)).check(matches(isDisplayed()));

        List<Notification> calls = fakeRepository.getNotificationCalls();
        assertEquals(1, calls.size());
        assertEquals("test_user_123", calls.get(0).getRecipientId());
        assertEquals(message, calls.get(0).getMessage());
    }
}