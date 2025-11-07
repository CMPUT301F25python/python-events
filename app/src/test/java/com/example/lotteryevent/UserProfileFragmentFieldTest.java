package com.example.lotteryevent;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Contains both the validation logic and its unit tests.
 * <p>
 * This test class validates the three profile fields used in setProfileInfo():
 * - Name: must not be empty or null
 * - Email: must follow a valid email format
 * - Phone: optional, but if provided must have exactly 10 digits
 * </p>
 */
public class UserProfileFragmentFieldTest {

    private UserProfileFragment fragment;

    @Before
    public void setUp() {
        // Create a new instance of the fragment before each test
        fragment = new UserProfileFragment();
    }

    // helper phone numbers and emails for testing
    private String validName = "John Doe";
    private String validEmail = "jdoe@gmail.com";

    private String validPhone = "1234567890";

    // all valid inputs returns null
    @Test
    public void testValidInputs() {
        String result = fragment.validateProfileInfo(validName, validEmail, validPhone);
        assertNull(result);
    }

    // no name provided, invalid
    @Test
    public void testEmptyName() {
        String result = fragment.validateProfileInfo("", validEmail, validPhone);
        assertEquals("", result);
    }

    @Test
    public void testEmptyEmail_ReturnsName() {
        String result = fragment.validateProfileInfo(validName, "", validPhone);
        assertEquals(validName, result);
    }

    @Test
    public void testInvalidEmailFormat_ReturnsEmail() {
        String result = fragment.validateProfileInfo(validName, "invalidemail.com", validPhone);
        assertEquals("invalidemail.com", result);
    }

    @Test
    public void testValidEmail_ReturnsNull() {
        String result = fragment.validateProfileInfo(validName, "user@example.com", validPhone);
        assertNull(result);
    }

    @Test
    public void testEmptyPhone_ReturnsNull() {
        // phone optional — empty is valid
        String result = fragment.validateProfileInfo(validName, validEmail, "");
        assertNull(result);
    }

    @Test
    public void testValidPhoneWithFormatting_ReturnsNull() {
        // (123) 456-7890 should still be valid
        String result = fragment.validateProfileInfo(validName, validEmail, "(123) 456-7890");
        assertNull(result);
    }

    @Test
    public void testTooShortPhone_ReturnsPhone() {
        String result = fragment.validateProfileInfo(validName, validEmail, "12345");
        assertEquals("12345", result);
    }

    @Test
    public void testTooLongPhone_ReturnsPhone() {
        String result = fragment.validateProfileInfo(validName, validEmail, "1234567890123");
        assertEquals("1234567890123", result);
    }

    @Test
    public void testValidPhoneExactly10Digits_ReturnsNull() {
        String result = fragment.validateProfileInfo(validName, validEmail, "1234567890");
        assertNull(result);
    }
}
