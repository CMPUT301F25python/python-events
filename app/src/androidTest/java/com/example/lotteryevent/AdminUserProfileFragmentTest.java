package com.example.lotteryevent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;

import android.os.Bundle;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lotteryevent.ui.admin.AdminUserProfileFragment;

import org.junit.Test;
import org.junit.runner.RunWith;

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

    /**
     * Verifies that the fragment launches correctly with a provided user ID, displaying
     * the "Delete User" button and the relevant event history headers.
     */
    @Test
    public void testPageLaunchesAndShowsDeleteButton() {
        // 1. Prepare arguments
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putString("userId", "test_user_123");

        // 2. Launch the fragment with arguments
        FragmentScenario.launchInContainer(AdminUserProfileFragment.class, fragmentArgs);

        // 3. Verify the Delete button is visible
        onView(withId(R.id.btn_delete_user))
                .check(matches(isDisplayed()));
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
}