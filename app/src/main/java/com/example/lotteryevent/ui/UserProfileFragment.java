package com.example.lotteryevent.ui;

import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.lotteryevent.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link Fragment} subclass that allows users to view and update their profile information.
 * This includes name, email, and phone number, all stored in Firestore.
 * The user can later delete their profile information and all associated information in collections.
 * The fragment uses Firebase Authentication to identify the user.
 *
 * <p>Includes validation for user input and provides a back button that navigates to the previous
 * fragment using Jetpack Navigation.</p>
 *
 * @author Sanaa Bhaidani
 * @version 1.0
 */
public class UserProfileFragment extends Fragment {

    private EditText nameField;
    private EditText emailField;
    private EditText phoneField;
    private Button updateInfo;
    private Button deleteProfile;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String deviceId;

    /**
     * Required empty public constructor.
     */
    public UserProfileFragment() {
    }

    /**
     * Inflates the layout for this fragment.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_profile, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has returned.
     * Initializes Firebase instances, sets up UI components, and loads the user's profile info.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();

        // make sure the user exists
        if (currentUser == null) {
            Toast.makeText(getContext(), "No user found.", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).popBackStack();
            return;
        }

        deviceId = currentUser.getUid(); // get the device ID

        // Find UI components
        nameField = view.findViewById(R.id.name_field);
        emailField = view.findViewById(R.id.email_field);
        phoneField = view.findViewById(R.id.phone_field);
        updateInfo = view.findViewById(R.id.update_button);
        deleteProfile = view.findViewById(R.id.delete_button);

        phoneField.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        // Load user data
        loadProfileInfo();

        // Save updated user data
        updateInfo.setOnClickListener(v -> setProfileInfo(view));

        // delete user data
        deleteProfile.setOnClickListener(c -> confirmProfileDeletion(view));

    }

    /**
     * Retrieves the user's profile information from Firestore.
     * Populates the input fields if data exists, otherwise logs an error.
     */
    private void loadProfileInfo() {
        DocumentReference docRef = db.collection("users").document(deviceId);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                String TAG = "UserProfileFragment";
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        nameField.setText(document.getString("name"));
                        emailField.setText(document.getString("email"));
                        phoneField.setText(document.getString("phone"));
                    } else {
                        Log.d(TAG, "Document does not exist.");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }

    /**
     * Validates user input fields and updates the user's profile information in Firestore.
     * <p>
     * This method, if validation for the input fields passes,
     * the user's profile document is updated in the "users" collection with the provided
     * values. Default values are also set for "admin" (false) and "optOutNotifications" (true).
     * A Toast message is displayed to indicate success or failure, and the view navigates back
     * upon completion.
     * </p>
     *
     * @param view the current view, used for navigation and context access
     */
    private void setProfileInfo(View view) {
        String name = nameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();

        String validationResult = validateProfileInfo(name, email, phone);

        // if validation does not pass, stay on the profile page
        if(validationResult != null){
            // validation failed
            Log.e("UserProfileFragment", "Profile validation failed for: " + validationResult);
            if(validationResult.equals(name)){
                Toast.makeText(getContext(), "Name is required.", Toast.LENGTH_SHORT).show();
            }
            if(validationResult.equals(email)){
                Toast.makeText(getContext(), "Enter a valid email.", Toast.LENGTH_SHORT).show();
            }
            if(validationResult.equals(phone) && !phone.isEmpty()){
                Toast.makeText(getContext(), "Enter a valid phone number.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // prepare Firestore update
        Map<String, Object> profileInfo = new HashMap<>();
        profileInfo.put("name", name);
        profileInfo.put("email", email);
        profileInfo.put("phone", phone.isEmpty() ? null : phone);
        profileInfo.put("admin", false);
        profileInfo.put("optOutNotifications", true);

        db.collection("users").document(deviceId).set(profileInfo);
        Toast.makeText(getContext(), "Profile updated successfully.", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(view).popBackStack();
    }

    /**
     * Clears the profile information for a specified user in Firestore.
     * <p>
     * This method sets the "name", "email", and "phone" fields in the user's
     * document to null, resets "admin" to false, and sets "optOutNotifications" to true.
     * This effectively anonymizes the user profile while maintaining the document reference.
     * </p>
     *
     * @param uid the unique identifier (UID) of the user whose profile information is to be cleared
     */
    private void clearProfileInfo(String uid) {
        Map<String, Object> clearFields = new HashMap<>();
        clearFields.put("name", null); // empty
        clearFields.put("email", null); // empty
        clearFields.put("phone", null); // empty
        clearFields.put("admin", false); // default false
        clearFields.put("optOutNotifications", true); // default true

        db.collection("users").document(uid)
                .update(clearFields)
                .addOnSuccessListener(aVoid -> Log.d("UserProfile", "User profile cleared"))
                .addOnFailureListener(e -> Log.e("UserProfile", "Failed to clear profile: " + e.getMessage()));
    }

    /**
     * Deletes the "eventsHistory" subcollection for a specified user in Firestore.
     * <p>
     * This method iterates through all documents in the "eventsHistory" subcollection
     * of the user's document and deletes them individually.
     * </p>
     *
     * @param uid the unique identifier (UID) of the user whose events history is to be deleted
     */
    private void deleteEventsHistory(String uid) {
        db.collection("users").document(uid).collection("eventsHistory").get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                })
                .addOnFailureListener(e -> Log.e("UserProfile", "Failed to delete subcollection eventsHistory: " + e.getMessage()));
    }

    /**
     * Deletes all notifications associated with a specific user.
     * <p>
     * This method queries the "notifications" collection for documents where
     * "recipientId" matches the user's UID and deletes each corresponding document.
     * </p>
     *
     * @param uid the unique identifier (UID) of the user whose notifications will be deleted
     */
    private void deleteNotifications(String uid){
        db.collection("notifications")
                .whereEqualTo("recipientId", uid).get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                })
                .addOnFailureListener(e -> Log.e("UserProfile", "Failed to delete notifications: " + e.getMessage()));
    }

    /**
     * Removes a user from all event-related subcollections in the "events" collection.
     * <p>
     * This method searches through all events and deletes the user's document (if present)
     * from the "entrants", "selected", and "waitlist" subcollections within each event.
     * </p>
     *
     * @param uid the unique identifier (UID) of the user to remove from event subcollections
     */
    private void deleteFromEventsCol(String uid) {
        String[] subcollections = {"entrants", "selected", "waitlist"};

        db.collection("events").get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String eventId = doc.getId();

                        for (String sub : subcollections) {
                            db.collection("events").document(eventId)
                                    .collection(sub).document(uid)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> Log.d("UserProfile", "Removed user from " + sub + " for event " + eventId))
                                    .addOnFailureListener(e -> Log.e("UserProfile", "Failed to remove user from " + sub + ": " + e.getMessage()));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("UserProfile", "Failed to query events: " + e.getMessage()));
    }

    /**
     * Deletes all events organized by a specific user.
     * <p>
     * This method finds all events in the "events" collection where "organizerId" matches
     * the user's UID, deletes their "entrants" subcollection documents, and then removes
     * the event documents themselves.
     * </p>
     *
     * @param uid the unique identifier (UID) of the user whose organized events are to be deleted
     */
    private void deleteEventsOrganized(String uid){
        db.collection("events")
                .whereEqualTo("organizerId", uid).get()
                .addOnSuccessListener(querySnapshot -> {
                    // if user didn't organize any events, bypass
                    if(querySnapshot.isEmpty()){
                        return;
                    }
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String eventId = doc.getId();

                        // delete entrants subcollection
                        db.collection("events").document(eventId).collection("entrants").get()
                                .addOnSuccessListener(entrantsSnapshot -> {
                                    for (DocumentSnapshot entrant : entrantsSnapshot.getDocuments()) {
                                        entrant.getReference().delete();
                                    }

                                    // delete event document
                                    db.collection("events").document(eventId).delete()
                                            .addOnSuccessListener(aVoid -> Log.d("UserProfile", "Deleted event " + eventId + " organized by user."))
                                            .addOnFailureListener(e -> Log.e("UserProfile", "Failed to delete event " + eventId + ": " + e.getMessage()));
                                })
                                .addOnFailureListener(e -> Log.e("UserProfile", "Failed to delete entrants for " + eventId + ": " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> Log.e("UserProfile", "Failed to query events organized by user: " + e.getMessage()));
    }

    /**
     * Deletes all user-related data from Firestore and navigates back to the previous screen.
     * <p>
     * This method clears the user's profile information, removes event history, notifications,
     * and event participation or organization data. A confirmation Toast message is displayed
     * upon successful deletion.
     * </p>
     *
     * @param uid  the unique identifier (UID) of the user whose data will be deleted
     * @param view the current view, used for navigation and context access
     */
    private void deleteUserData(String uid, View view) {
        clearProfileInfo(uid); // clears main document fields
        deleteEventsHistory(uid); // deletes events history
        deleteNotifications(uid); // deletes all notifications
        deleteFromEventsCol(uid); // delete user from current events
        deleteEventsOrganized(uid); // delete users events
        Toast.makeText(getContext(), "Profile succesfully deleted.", Toast.LENGTH_SHORT).show();
        Navigation.findNavController(view).popBackStack();
    }

    /**
     * Displays a confirmation dialog before deleting the user's profile and all associated data.
     * <p>
     * If confirmed, the currently authenticated user's data is deleted.
     * If canceled, no further action is taken.
     * </p>
     *
     * @param view the current view, used for dialog context and navigation
     */
    private void confirmProfileDeletion(View view){
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Data")
                .setMessage("This will clear your profile information and delete all associated data. Are you sure you want to delete your profile?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseUser currentUser = auth.getCurrentUser();
                    if (currentUser != null) {
                        deleteUserData(currentUser.getUid(), view);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * This method checks that the name and email fields are not empty, validates the email format,
     * and verifies that the phone number (if provided) contains exactly 10 digits.
     * @param name the name inputted by user
     * @param email the email inputted by user
     * @param phone the phone number inputte by user
     * @return the string that is invalid, null if successful validation of all fields
     */
    public String validateProfileInfo(String name, String email, String phone){
        // name and email mandatory (check)
        if (name.isEmpty()) {
            return name;
        }

        // check if valid email address
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.isEmpty()) {
            return email;
        }

        // check if valid phone number (if one has been provided)
        if (!phone.isEmpty()) {
            String phoneDigitsOnly = phone.replaceAll("\\D", "");
            if(phoneDigitsOnly.length() != 10) {
                return phone;
            }
        }
        return null; //validation successful
    }
}
