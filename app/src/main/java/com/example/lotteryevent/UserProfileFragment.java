package com.example.lotteryevent;

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
 * The user can later delete their profile information and all associate documents in collections.
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
     * Validates input fields and updates the user's profile in Firestore.
     * Displays a toast message indicating success or failure.
     */
    private void setProfileInfo(View view) {
        String name = nameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();

        // name and email mandatory (check)
        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(getContext(), "Name and email required.", Toast.LENGTH_SHORT).show();
            return;
        }

        // check if valid email address
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Enter a valid email.", Toast.LENGTH_SHORT).show();
            return;
        }

        // check if valid phone number (if one has been provided)
        if (!phone.isEmpty()) {
            String phoneDigitsOnly = phone.replaceAll("\\D", "");
            if(phoneDigitsOnly.length() != 10) {
                Toast.makeText(getContext(), "Enter a valid phone number.", Toast.LENGTH_SHORT).show();
                return;
            }
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
     * document to null, effectively removing all personal data fields.
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
     * Deletes all specified subcollections for a given user in Firestore.
     * <p>
     * Each document in the specified subcollections will be deleted.
     * </p>
     *
     * @param uid the unique identifier (UID) of the user whose subcollections are to be deleted
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
     * Deletes all user data including profile information and subcollections.
     * <p>
     * This method first clears the user's main document fields and then deletes
     * all related subcollections. A confirmation Toast message is displayed, and
     * go back to home.
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
     * Displays a confirmation dialog before deleting a user's profile and associated data.
     * <p>
     * If the user confirms, the method retrieves the currently authenticated user
     * and proceeds to delete their data from Firestore. If the user cancels, no action is taken.
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

}
