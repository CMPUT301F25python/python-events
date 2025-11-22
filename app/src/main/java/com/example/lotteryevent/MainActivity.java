package com.example.lotteryevent;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;


/**
 * The main and only activity for the application, following a Single Activity Architecture.
 * <p>
 * This activity hosts the {@link NavHostFragment} which manages the display of all other
 * UI screens (fragments). It is responsible for setting up the global navigation components,
 * including the {@link Toolbar} and the {@link DrawerLayout} with its {@link NavigationView}.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Configuration for the app bar (Toolbar/Action Bar) to integrate with the NavController.
     * It defines top-level destinations and configures the navigation drawer icon.
     */
    private AppBarConfiguration appBarConfiguration;

    // device authentication
    private FirebaseAuth mAuth;
    private static final String TAG = "AnonymousAuth";
    private NotificationCustomManager notificationCustomManager;

    // flag to check user is initialized (only then should rest of app be set up)
    private boolean userInitialized = false;

    /**
     * Initializes the activity, sets up the main layout, logs user in based on device id, and configures navigation.
     * <p>
     * This method inflates the activity's UI, finds the NavController, and uses {@link NavigationUI}
     * to connect the Toolbar and NavigationView to the navigation graph. This enables automatic
     * title updates and handling of the navigation drawer icon (hamburger icon).
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *                           shut down, this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // user identification by device
        mAuth = FirebaseAuth.getInstance();
        notificationCustomManager = new NotificationCustomManager(getApplicationContext());

        FirebaseUser currentUser = mAuth.getCurrentUser();
        // anonymous sign-in if no user, else initialize immediately
        if(currentUser == null){
            signInAnonymously();
        } else {
            onUserInitialized(currentUser); // now we have a user, initialize the app
        }
    }

    /**
     * Called once we know we have a valid Firebase user.
     * Sets up Firestore user doc and notifications for this UID.
     * @param user this is the id of the device
     */
    public void initializeUser(FirebaseUser user){
        // if no user, this doesn't execute (extra safety)
        if(user == null){
            return;
        }
        String uid = user.getUid();
        updateUI(user); //write to Firestore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                getPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        notificationCustomManager.clearNotifications();
        notificationCustomManager.checkAndDisplayUnreadNotifications(uid);
        notificationCustomManager.listenForNotifications(uid);
    }

    /**
     * This function initializes the user. Once the auth state is set, app is fully set-up
     * @param user the user's device id
     */
    private void onUserInitialized(FirebaseUser user){
        // extra check: don't do anything if user isn't logged in
        if(user == null || userInitialized){
            return;
        }
        userInitialized = true; // set flag

        initializeUser(user); // initialize user data and notifications
        setUpNavigation(); // guaranteed auth state so set up nav and fragments
    }

    /**
     * This function sets up the navigation for the app use in onCreate().
     * We also setup the nav_graph after the user ID has been set
     */
    private void setUpNavigation(){
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        navController.setGraph(R.navigation.nav_graph); // set up navgraph after uid has been set

        appBarConfiguration = new AppBarConfiguration.Builder(R.id.homeFragment)
                .setOpenableLayout(drawerLayout)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }

    /**
     * Called when the activity becomes visible to the user.
     */
    @Override
    public void onStart() {
        super.onStart();
    }

    private final ActivityResultLauncher<String> getPermission =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean isGranted) {
                if (isGranted) {
                    Toast.makeText(getApplicationContext(), "Notification permission granted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Notification permission denied.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    /**
     * Signs the user in anonymously using Firebase Authentication.
     * <p>
     * This method assigns a unique UID to the user, allowing them to interact
     * with the Firestore database without creating an account. If the sign-in
     * is successful, updateUI(FirebaseUser) is called with the authenticated user.
     * If it fails, an error message is logged and displayed to the user.
     * </p>
     */
    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInAnonymously:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            onUserInitialized(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInAnonymously:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }
    /**
     * Updates or creates a Firestore document for the authenticated user.
     * <p>
     * This method ensures that the current user's unique device ID (Firebase UID)
     * is present in the {@code users} collection in Firestore. If the document
     * does not already exist, it is created. If any pre-existing data, they are merged.
     * </p>
     *
     * @param user The currently authenticated Firebase user whose profile entry
     *             should be verified or created in the Firestore database.
     */
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            String deviceId = user.getUid();
            Log.d("DeviceID", deviceId);

            // create entry in firestore collection (if it doesn't already exist)
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> userInfo = new HashMap<>();
            db.collection("users").document(deviceId)
                    .set(userInfo, SetOptions.merge()) // merge to avoid overwriting if document already exists
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User document ready"))
                    .addOnFailureListener(e -> Log.w(TAG, "Error creating user document", e));
        }
    }

    /**
     * Handles the Up button navigation action in the app bar.
     * <p>
     * This method is called when the user presses the "Up" arrow in the toolbar. It delegates
     * the navigation action to the {@link NavController}, which correctly handles navigating
     * back through the fragment back stack.
     *
     * @return {@code true} if navigation was handled by the NavController, {@code false} otherwise.
     */
    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}