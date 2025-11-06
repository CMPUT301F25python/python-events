package com.example.lotteryevent;

import android.os.Bundle;
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
 * The fragment uses Firebase Authentication to identify the user.
 *
 * <p>Includes validation for user input and provides a back button that navigates to the previous
 * fragment using Jetpack Navigation.</p>
 *
 * @author
 * @version 1.0
 */
public class UserProfileFragment extends Fragment {

    private EditText nameField;
    private EditText emailField;
    private EditText phoneField;
    private Button updateInfo;
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

        // Load user data
        loadProfileInfo();

        // Save updated user data
        updateInfo.setOnClickListener(v -> setProfileInfo());

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
    private void setProfileInfo() {
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
        if (!phone.isEmpty() && phone.length() != 10) {
            Toast.makeText(getContext(), "Enter a valid phone number.", Toast.LENGTH_SHORT).show();
            return;
        }

        // prepare Firestore update
        Map<String, Object> profileInfo = new HashMap<>();
        profileInfo.put("name", name);
        profileInfo.put("email", email);
        profileInfo.put("phone", phone.isEmpty() ? null : phone);

        db.collection("users").document(deviceId)
                .set(profileInfo)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), "Profile updated successfully.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error updating profile.", Toast.LENGTH_SHORT).show());
    }
}
