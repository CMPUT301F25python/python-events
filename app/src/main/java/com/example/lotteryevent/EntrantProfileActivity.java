package com.example.lotteryevent;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.material.appbar.MaterialToolbar;

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

public class EntrantProfileActivity extends AppCompatActivity {

    private EditText nameField;
    private EditText emailField;
    private EditText phoneField;
    private Button updateInfo;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String deviceId;
    private MaterialToolbar profileHeader;

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

        nameField = findViewById(R.id.nameField);
        emailField = findViewById(R.id.emailField);
        phoneField = findViewById(R.id.phoneField);
        updateInfo = findViewById(R.id.updateButton);

        // going back to main page
        profileHeader.setNavigationOnClickListener(v->{
            Intent intent = new Intent(EntrantProfileActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        loadProfileInfo(); // loading existing profile data, if any
        updateInfo.setOnClickListener(v -> setProfileInfo()); // save user data
    }

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

    private void setProfileInfo(){
        String name = nameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();

        // name and email mandatory (check)
        if(name.isEmpty() || email.isEmpty()){
            Toast.makeText(this, "Name and email required.", Toast.LENGTH_LONG).show();
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