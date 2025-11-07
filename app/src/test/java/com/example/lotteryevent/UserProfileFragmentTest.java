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
public class UserProfileFragmentTest {

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

    // all valid inputs, null
    @Test
    public void testAllValid() {
        String result = fragment.validateProfileInfo(validName, validEmail, validPhone);
        assertNull(result);
    }

    // no name provided, invalid
    @Test
    public void testEmptyName() {
        String result = fragment.validateProfileInfo("", validEmail, validPhone);
        assertEquals("", result);
    }
    // email not provided
    @Test
    public void testInvalidNoEmail() {
        String result = fragment.validateProfileInfo(validName, "", validPhone);
        assertEquals("", result);
    }

    // invalid email format
    @Test
    public void testInvalidEmailFormat() {
        String result = fragment.validateProfileInfo(validName, "invalidemail.com", validPhone);
        assertEquals("invalidemail.com", result);
    }

    // valid email format
    @Test
    public void testValidEmail() {
        String result = fragment.validateProfileInfo(validName, "user@example.com", validPhone);
        assertNull(result);
    }

    // no phone number, valid, since empty
    @Test
    public void testEmptyPhone() {
        // phone optional — empty is valid
        String result = fragment.validateProfileInfo(validName, validEmail, "");
        assertNull(result);
    }

    // user formatted phone number - (XXX) XXX-XXXX is valid
    @Test
    public void testValidFormattedPhone() {
        String result = fragment.validateProfileInfo(validName, validEmail, "(123) 456-7890");
        assertNull(result);
    }

    // number <10 is short
    @Test
    public void testInvalidShortNumber() {
        String result = fragment.validateProfileInfo(validName, validEmail, "12345");
        assertEquals("12345", result);
    }

    // number >10 is too long
    @Test
    public void testInvalidLongNumber() {
        String result = fragment.validateProfileInfo(validName, validEmail, "1234567890123");
        assertEquals("1234567890123", result);
    }

    // exactly 10 digits
    @Test
    public void testValidPhone() {
        String result = fragment.validateProfileInfo(validName, validEmail, "1234567890");
        assertNull(result);
    }
}
