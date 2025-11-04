package com.example.lotteryevent;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
 * This class manages the entrant's profile screen,
 * allowing users to view and update their personal information such as name,
 * email, and phone number. It integrates with Firebase Authentication and
 * Firestore to store and retrieve user data.
 *
 * <p>This activity also provides a back button for navigation and includes
 * input validation before saving user details to Firestore.</p>
 *
 * @author Sanaa Bhaidani
 * @version 1.0
 */

public class EntrantProfileActivity extends AppCompatActivity {

    private EditText nameField;
    private EditText emailField;
    private EditText phoneField;
    private Button updateInfo;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String deviceId;
    private ImageButton backArrow;

    /**
     * Called when the activity is first created. Initializes Firebase Authentication
     * and Firestore instances, retrieves the current user, and sets up the UI components
     * including input fields, update button, and back arrow. Loads the user's profile
     * information from Firestore and sets up listeners for user actions.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *                           shut down, this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_profile);

        // initialize firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();

        // making sure user exists
        if(currentUser == null){
            finish();
            return;
        }

        deviceId = currentUser.getUid(); // get the device ID

        nameField = findViewById(R.id.name_field);
        emailField = findViewById(R.id.email_field);
        phoneField = findViewById(R.id.phone_field);
        updateInfo = findViewById(R.id.update_button);

        loadProfileInfo(); // loading existing profile data, if any
        updateInfo.setOnClickListener(v -> setProfileInfo()); // save user data

        // going back to main page
        backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v->finish());
    }

    /**
     * Retrieves the entrant's profile information from Firestore based on their unique
     * device ID (Firebase user UID). If the document exists, it populates the input
     * fields with the stored name, email, and phone values. Logs errors if retrieval
     * fails or if the document does not exist.
     */

    private void loadProfileInfo(){
        DocumentReference docRef = db.collection("users").document(deviceId);
        docRef.get().addOnCompleteListener((new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                String TAG = "EntrantProfileActivity";
                if(task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()){
                        nameField.setText(document.getString("name"));
                        emailField.setText(document.getString("email"));
                        phoneField.setText(document.getString("phone"));
                    } else {
                        Log.d(TAG, "Document does not exist.");
                    }
                } else {
                    Log.d(TAG,"get failed with ", task.getException());
                }
            }
        }));
    }

    /**
     * Validates user input fields (name, email, and phone number) and updates the
     * entrant's profile information in Firestore. Displays appropriate error messages
     * for invalid input or missing mandatory fields. On successful update, a toast
     * notification confirms the update; otherwise, an error message is displayed.
     */
    private void setProfileInfo(){
        String name = nameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();

        // name and email mandatory (check)
        if(name.isEmpty() || email.isEmpty()){
            Toast.makeText(this, "Name and email required.", Toast.LENGTH_SHORT).show();
            return;
        }

        // checking if valid email address
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this, "Enter a valid email.", Toast.LENGTH_SHORT).show();
            return;
        }

        // checking if valid phone number (if one has been provided)
        if(!phone.isEmpty() && phone.length() != 10){
            Toast.makeText(this, "Enter a valid phone number.", Toast.LENGTH_SHORT).show();
            return;
        }

        // adding user input to Firebase
        Map<String, Object> profileInfo = new HashMap<>();
        profileInfo.put("name", name);
        profileInfo.put("email", email);
        if(phone.isEmpty()){
            profileInfo.put("phone", null);
        } else {
            profileInfo.put("phone", phone);
        }

        db.collection("users").document(deviceId)
                .set(profileInfo)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Profile updated successfully.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(aVoid ->
                        Toast.makeText(this, "Error updating profile.", Toast.LENGTH_SHORT).show());

    }
}