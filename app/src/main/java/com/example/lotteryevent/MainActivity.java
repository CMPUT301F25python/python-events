package com.example.lotteryevent;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

    /**
     * Initializes the activity, sets up the main layout, and configures navigation.
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

        // user identification by device
        mAuth = FirebaseAuth.getInstance();
        signInAnonymously();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        appBarConfiguration = new AppBarConfiguration.Builder(R.id.homeFragment)
                .setOpenableLayout(drawerLayout)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInAnonymously:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
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

    private void updateUI(FirebaseUser user) {
        if(user != null){
            String deviceId = user.getUid();
            Log.d("DeviceID", deviceId);
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