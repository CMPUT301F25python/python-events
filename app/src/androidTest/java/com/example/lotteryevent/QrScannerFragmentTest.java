package com.example.lotteryevent;

import android.Manifest;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.example.lotteryevent.ui.QrScannerFragment;
import com.example.lotteryevent.viewmodels.QrScannerViewModel;
import org.junit.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented unit tests for the {@link QrScannerFragment}.
 * <p>
 * This test class is responsible for validating the user interface components of the QR scanner.
 * It specifically ensures that the fragment initializes correctly and displays the camera preview
 * stream when launched.
 * <p>
 * A {@link FakeQrScannerViewModel} is utilized to decouple the test from the underlying
 * business logic, ensuring a stable testing environment free from external dependencies.
 */
@RunWith(AndroidJUnit4.class)
public class QrScannerFragmentTest {

    /**
     * Grants the {@link Manifest.permission#CAMERA} permission automatically.
     * This prevents the test from blocking or failing due to the system runtime permission dialog.
     */
    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA);

    private FakeQrScannerViewModel fakeViewModel;
    private ReusableTestFragmentFactory fragmentFactory;
    private TestNavHostController navController;

    /**
     * Configures the test environment before each test execution.
     * <p>
     * This method initializes a {@link FakeQrScannerViewModel} and injects it into the
     * fragment via a custom {@link ViewModelProvider.Factory}. This setup isolates the
     * UI test from production business logic and external dependencies.
     */
    @Before
    public void setup() {
        fakeViewModel = new FakeQrScannerViewModel();

        ViewModelProvider.Factory fakeFactory = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                if (modelClass.isAssignableFrom(QrScannerViewModel.class)) {
                    return (T) fakeViewModel;
                }
                throw new IllegalArgumentException("Unknown ViewModel class");
            }
        };

        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(QrScannerFragment.class, () -> new QrScannerFragment(fakeFactory));
    }

    /**
     * Helper method to launch the fragment and attach the TestNavHostController.
     */
    private void launchFragment() {
        navController = new TestNavHostController(ApplicationProvider.getApplicationContext());

        FragmentScenario<QrScannerFragment> scenario = FragmentScenario.launchInContainer(
                QrScannerFragment.class, null, R.style.Theme_LotteryEvent, fragmentFactory
        );

        scenario.onFragment(fragment -> {
            navController.setGraph(R.navigation.nav_graph);
            navController.setCurrentDestination(R.id.qrScannerFragment);
            Navigation.setViewNavController(fragment.requireView(), navController);
        });
    }

    /**
     * Verifies that the camera preview component is visible upon fragment launch.
     * <p>
     * This test confirms that the {@link QrScannerFragment} successfully initializes
     * its views and handles the camera permission without error.
     */
    @Test
    public void scanner_displaysCameraPreview() {
        // Act: Launch Fragment
        FragmentScenario.launchInContainer(
                QrScannerFragment.class, null, R.style.Theme_LotteryEvent, fragmentFactory
        );

        // Assert: Camera Preview is visible
        Espresso.onView(ViewMatchers.withId(R.id.camera_preview)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
    }

    /**
     * Verifies that a successful scan triggers navigation to the Event Details screen.
     * <p>
     * This test simulates a scan result via the ViewModel and checks the Navigation Controller's
     * back stack to ensure the correct destination and arguments were pushed.
     */
    @Test
    public void validScanResult_navigatesToEventDetails() {
        // Arrange
        launchFragment();
        String testEventId = "event_999";

        // Ensure UI is settled
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Act: Trigger the scan on the main thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            fakeViewModel.simulateScan(testEventId);
        });

        // Wait for LiveData propagation and Navigation
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Assert: Check Back Stack for the destination
        Bundle arguments = null;
        try {
            arguments = navController.getBackStackEntry(R.id.eventDetailsFragment).getArguments();
        }
        catch (IllegalArgumentException e) {
            Assert.fail("Navigation to EventDetailsFragment did not occur.");
        }

        Assert.assertNotNull("Arguments should not be null", arguments);
        Assert.assertEquals(testEventId, arguments.getString("eventId"));
    }

    /**
     * A stub implementation of {@link QrScannerViewModel} used for testing purposes.
     * <p>
     * This class acts as a placeholder to satisfy the fragment's dependency requirements,
     * preventing the instantiation of real camera or ML Kit components during UI testing.
     */
    public static class FakeQrScannerViewModel extends QrScannerViewModel {
        private final MutableLiveData<String> testLiveData = new MutableLiveData<>();

        @Override
        public LiveData<String> getScannedEventId() {
            return testLiveData;
        }

        public void simulateScan(String eventId) {
            // Using postValue ensures safety across threads in test environments
            testLiveData.postValue(eventId);
        }
    }
}