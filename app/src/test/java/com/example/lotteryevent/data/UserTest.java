package com.example.lotteryevent.data;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the {@link User} data model.
 * <p>
 * This class verifies the behavior of the User POJO, ensuring that:
 * <ul>
 *     <li>The no-argument constructor initializes the object correctly.</li>
 *     <li>Getters and Setters correctly store and retrieve values.</li>
 *     <li>Boolean flags for admin status and notification preferences work as expected.</li>
 * </ul>
 */
public class UserTest {

    /**
     * Verifies that the no-argument constructor creates a valid instance
     * and that fields are initialized to null by default (standard Java behavior).
     */
    @Test
    public void testNoArgumentConstructor() {
        User user = new User();

        assertNotNull("User object should be created", user);
        assertNull("ID should be null initially", user.getId());
        assertNull("Name should be null initially", user.getName());
        assertNull("Email should be null initially", user.getEmail());
        assertNull("Phone should be null initially", user.getPhone());
        assertNull("Admin status should be null initially", user.getAdmin());
        assertNull("OptOutNotifications should be null initially", user.getOptOutNotifications());
    }

    /**
     * Verifies the getter and setter for the User ID (Firestore Document ID).
     */
    @Test
    public void testSetAndGetId() {
        User user = new User();
        String id = "user_12345";

        user.setId(id);

        assertEquals("User ID should match the set value", id, user.getId());
    }

    /**
     * Verifies the getter and setter for the User Name.
     */
    @Test
    public void testSetAndGetName() {
        User user = new User();
        String name = "John Doe";

        user.setName(name);

        assertEquals("Name should match the set value", name, user.getName());
    }

    /**
     * Verifies the getter and setter for the User Email.
     */
    @Test
    public void testSetAndGetEmail() {
        User user = new User();
        String email = "test@example.com";

        user.setEmail(email);

        assertEquals("Email should match the set value", email, user.getEmail());
    }

    /**
     * Verifies the getter and setter for the User Phone number.
     */
    @Test
    public void testSetAndGetPhone() {
        User user = new User();
        String phone = "555-0199";

        user.setPhone(phone);

        assertEquals("Phone should match the set value", phone, user.getPhone());
    }

    /**
     * Verifies the getter and setter for the Admin boolean flag.
     * Checks both true and false states.
     */
    @Test
    public void testSetAndGetAdmin() {
        User user = new User();

        // Test True
        user.setAdmin(true);
        assertTrue("Admin status should be true", user.getAdmin());

        // Test False
        user.setAdmin(false);
        assertFalse("Admin status should be false", user.getAdmin());
    }

    /**
     * Verifies the getter and setter for the Notification Opt-Out flag.
     * Checks both true and false states.
     */
    @Test
    public void testSetAndGetOptOutNotifications() {
        User user = new User();

        // Test True (User wants to opt out)
        user.setOptOutNotifications(true);
        assertTrue("OptOut status should be true", user.getOptOutNotifications());

        // Test False (User wants notifications)
        user.setOptOutNotifications(false);
        assertFalse("OptOut status should be false", user.getOptOutNotifications());
    }

    /**
     * Verifies that setters accept null values, which is a valid state
     * for Firestore documents (e.g., a user might not have a phone number).
     */
    @Test
    public void testNullValues() {
        User user = new User();
        user.setName("Temp Name");
        user.setPhone("123");

        // Set back to null
        user.setName(null);
        user.setPhone(null);

        assertNull("Name should handle null assignment", user.getName());
        assertNull("Phone should handle null assignment", user.getPhone());
    }
}