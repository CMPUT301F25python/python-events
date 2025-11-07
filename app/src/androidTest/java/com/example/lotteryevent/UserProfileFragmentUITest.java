package com.example.lotteryevent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for the validation logic within the {@link UserProfileFragment}.
 * <p>
 * This class tests the {@code validateProfileInfo} method to ensure all business rules
 * for providing profile info are correctly enforced. It covers scenarios such as empty fields
 * for name and email and invalid emails and phone numbers.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserProfileFragmentUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> scenario =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void navigateToUserProfileFragment() {
        // Navigate from HomeFragment to UserProfileFragment
        // assuming you have a profile icon or menu item
        onView(withId(R.id.profile_icon)).perform(click());
        // Wait until the UserProfileFragment's update button is visible to start tests (otherwise testInvalidNameField fails)
        onView(withId(R.id.update_button)).check(matches(isDisplayed()));
    }

    /**
     * if all valid fields (including phone number) navigate to HomeFragment
     */
    @Test
    public void testValidFieldsIncludingPhoneNumber() {
        // Type valid user information
        onView(withId(R.id.name_field)).perform(clearText(), typeText("John Doe"), closeSoftKeyboard());
        onView(withId(R.id.email_field)).perform(clearText(), typeText("john@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.phone_field)).perform(clearText(), typeText("1234567890"), closeSoftKeyboard());

        // Click update button
        onView(withId(R.id.update_button)).perform(click());

        // Verify HomeFragment is displayed (example: by checking a known element)
        onView(withId(R.id.fab_add_event)).check(matches(isDisplayed()));
    }

    /**
     * if all valid fields (not including phone number), navigate to HomeFragment
     */
    @Test
    public void testValidFieldsNotIncludingPhoneNumber() {
        // Type valid user information
        onView(withId(R.id.name_field)).perform(clearText(), typeText("John Doe"), closeSoftKeyboard());
        onView(withId(R.id.email_field)).perform(clearText(), typeText("johndoe@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.phone_field)).perform(clearText(), closeSoftKeyboard());

        // Click update button
        onView(withId(R.id.update_button)).perform(click());

        // Verify HomeFragment is displayed (example: by checking a known element)
        onView(withId(R.id.fab_add_event)).check(matches(isDisplayed()));
    }

    /**
     * if invalid email field, then remain on UserProfileFragment
     */
    @Test
    public void testInvalidEmailField() {
        // Leave email invalid/empty
        onView(withId(R.id.name_field)).perform(clearText(), typeText("John Doe"), closeSoftKeyboard());
        onView(withId(R.id.email_field)).perform(clearText(), closeSoftKeyboard());
        onView(withId(R.id.phone_field)).perform(clearText(), typeText("9876543210"), closeSoftKeyboard());

        // Click update button
        onView(withId(R.id.update_button)).perform(click());

        // Verify still on UserProfileFragment (e.g., name field still visible)
        onView(withId(R.id.name_field)).check(matches(isDisplayed()));
    }

    /**
     * if invalid phone number field (ex. too long) then remain on UserProfileFragment
     */
    @Test
    public void testInvalidPhoneField_tooLong() {
        // Leave email invalid/empty
        onView(withId(R.id.name_field)).perform(clearText(), typeText("John Doe"), closeSoftKeyboard());
        onView(withId(R.id.email_field)).perform(clearText(), typeText("johndoe@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.phone_field)).perform(clearText(), typeText("11223344557799"), closeSoftKeyboard());

        // Click update button
        onView(withId(R.id.update_button)).perform(click());

        // Verify still on UserProfileFragment (e.g., name field still visible)
        onView(withId(R.id.name_field)).check(matches(isDisplayed()));
    }

    /**
     * if invalid phone number field (ex. too short) then remain on UserProfileFragment
     */
    @Test
    public void testInvalidPhoneField_tooShort() {
        // Leave email invalid/empty
        onView(withId(R.id.name_field)).perform(clearText(), typeText("John Doe"), closeSoftKeyboard());
        onView(withId(R.id.email_field)).perform(clearText(), typeText("johndoe@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.phone_field)).perform(clearText(), typeText("125"), closeSoftKeyboard());

        // Click update button
        onView(withId(R.id.update_button)).perform(click());

        // Verify still on UserProfileFragment (e.g., name field still visible)
        onView(withId(R.id.name_field)).check(matches(isDisplayed()));
    }
}
