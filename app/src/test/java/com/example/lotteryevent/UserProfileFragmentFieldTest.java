package com.example.lotteryevent;

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
    /**
     * Checks if the given name is valid
     */
    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty();
    }

    /**
     * Checks if the given email has a valid format.
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    /**
     * Checks if the given phone number (optional) is valid.
     * Empty = valid (optional field)
     * Non-empty must contain exactly 10 digits.
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return true;
        String digits = phone.replaceAll("\\D", "");
        return digits.length() == 10;
    }

    @Test
    public void testValidName() {
        assertTrue(isValidName("John"));
    }

    @Test
    public void testEmptyName() {
        assertFalse(isValidName(""));
    }

    @Test
    public void testWhitespaceName() {
        assertFalse(isValidName("   "));
    }

    @Test
    public void testNullName() {
        assertFalse(isValidName(null));
    }

    @Test
    public void testValidEmail() {
        assertTrue(isValidEmail("user@example.com"));
    }

    @Test
    public void testMissingAtSymbol() {
        assertFalse(isValidEmail("userexample.com"));
    }

    @Test
    public void testMissingDomain() {
        assertFalse(isValidEmail("user@"));
    }

    @Test
    public void testEmptyEmail() {
        assertFalse(isValidEmail(""));
    }

    @Test
    public void testNullEmail() {
        assertFalse(isValidEmail(null));
    }

    @Test
    public void testEmptyPhone() {
        // optional field — empty is valid
        assertTrue(isValidPhone(""));
    }

    @Test
    public void testValidPhoneExactly10Digits() {
        assertTrue(isValidPhone("1234567890"));
    }

    @Test
    public void testPhoneWithFormatting() {
        assertTrue(isValidPhone("(123) 456-7890"));
    }

    @Test
    public void testTooShortPhone() {
        assertFalse(isValidPhone("12345"));
    }

    @Test
    public void testTooLongPhone() {
        assertFalse(isValidPhone("1234567890123"));
    }

    @Test
    public void testNullPhone() {
        assertTrue(isValidPhone(null)); // null = optional
    }
}
