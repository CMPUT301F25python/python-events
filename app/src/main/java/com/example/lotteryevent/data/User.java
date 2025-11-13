package com.example.lotteryevent.data;

import com.google.firebase.firestore.DocumentId;

/**
 * Represents a single document in the user collection
 * This POJO is a direct mapping of the '/user/{userId}' schema.
 */
public class User {

    @DocumentId
    private String id;
    private String name;
    private String email;
    private String phone;
    private Boolean admin;
    private Boolean optOutNotifications;

    /**
     * A public no-argument constructor required by Firestore.
     */
    public User() {}

    // --- Getters and Setters ---
    // Firestore uses these to map data to and from the object

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Boolean getAdmin() { return admin; }
    public void setAdmin(Boolean admin) { this.admin = admin; }

    public Boolean getOptOutNotifications() { return optOutNotifications; }
    public void setOptOutNotifications(Boolean optOutNotifications) {
        this.optOutNotifications = optOutNotifications;
    }

}
