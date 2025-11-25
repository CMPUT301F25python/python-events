package com.example.lotteryevent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.lotteryevent.data.AdminImageItem;
import com.example.lotteryevent.repository.FakeAdminImagesRepository;
import com.example.lotteryevent.ui.admin.AdminImagesFragment;
import com.example.lotteryevent.viewmodels.AdminImagesViewModel;
import com.example.lotteryevent.viewmodels.GenericViewModelFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test class for {@link AdminImagesFragment} and {@link AdminImagesViewModel}.
 * <p>
 * This class verifies the functionality of the Admin Image Dashboard, including:
 * <ul>
 *     <li>Browsing images (empty and populated states).</li>
 *     <li>Deleting images (success and failure scenarios).</li>
 * </ul>
 * It uses a {@link FakeAdminImagesRepository} to simulate Firestore operations, ensuring tests
 * are fast, reliable, and isolated from the network.
 */
@RunWith(AndroidJUnit4.class)
public class AdminImagesFragmentTest {

    private FakeAdminImagesRepository fakeRepository;
    private ReusableTestFragmentFactory fragmentFactory;
    private AdminImagesViewModel viewModel;

    /**
     * Sets up the test environment before each test method.
     * Initializes the FakeRepository, ViewModel, and configures the FragmentFactory
     * to inject the ViewModel into the Fragment.
     */
    @Before
    public void setup() {
        fakeRepository = new FakeAdminImagesRepository();

        viewModel = new AdminImagesViewModel(fakeRepository);

        GenericViewModelFactory viewModelFactory = new GenericViewModelFactory();
        viewModelFactory.put(AdminImagesViewModel.class, () -> viewModel);

        fragmentFactory = new ReusableTestFragmentFactory();
        fragmentFactory.put(
                AdminImagesFragment.class,
                () -> new AdminImagesFragment(viewModelFactory)
        );
    }

    /**
     * Verifies that when the repository is empty, the RecyclerView in the Fragment
     * correctly displays zero items.
     */
    @Test
    public void browseImages_emptyRepo_showsNoImages() {
        FragmentScenario.launchInContainer(
                AdminImagesFragment.class,
                null,
                R.style.Theme_LotteryEvent,
                fragmentFactory
        );

        onView(withId(R.id.admin_images_recycler))
                .check((view, noViewFoundException) -> {
                    if (noViewFoundException != null) throw noViewFoundException;
                    RecyclerView recycler = (RecyclerView) view;
                    assertEquals("Recycler view should be empty", 0, recycler.getAdapter().getItemCount());
                });
    }


    /**
     * Verifies that the ViewModel correctly fetches data from the repository and updates
     * the {@code images} LiveData.
     * <p>
     * This test checks the data flow directly on the ViewModel to avoid UI synchronization flakiness.
     */
    @Test
    public void verifyViewModel_receivesDataFromRepository() throws InterruptedException {
        // Arrange
        String fakeBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";
        fakeRepository.itemsToReturn.add(new AdminImageItem("event1", fakeBase64));
        fakeRepository.itemsToReturn.add(new AdminImageItem("event2", fakeBase64));

        // Act
        FragmentScenario.launchInContainer(AdminImagesFragment.class, null, R.style.Theme_LotteryEvent, fragmentFactory)
                .onFragment(fragment -> {
                    // Trigger fetch
                    viewModel.fetchImages();
                });

        // Assert
        // Use helper to wait for get
        java.util.List<AdminImageItem> images = getOrAwaitValue(viewModel.getImages());

        assertEquals(2, images.size());
        assertEquals("event1", images.get(0).getEventId());
    }

    /**
     * Verifies that calling {@code deleteImage} on the ViewModel successfully triggers
     * the repository's delete method and updates the success message.
     */
    @Test
    public void verifyDeleteLogic_callsRepository() throws InterruptedException {
        // Arrange
        String fakeBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";
        fakeRepository.itemsToReturn.add(new AdminImageItem("event123", fakeBase64));

        // Act
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            viewModel.deleteImage("event123");
        });

        // Assert
        assertTrue("Repository delete method should have been triggered", fakeRepository.deleteWasCalled);
        assertEquals("event123", fakeRepository.deletedEventId);

        // Use helper to wait for message
        String message = getOrAwaitValue(viewModel.getMessage());
        assertEquals("Image successfully deleted", message);
    }

    /**
     * Verifies that the ViewModel handles repository failures (e.g., network errors)
     * gracefully by updating the {@code message} LiveData with an error description.
     */
    @Test
    public void verifyDeleteFailure_showsErrorMessage() throws InterruptedException {
        // Arrange
        fakeRepository.shouldFail = true; // Force the repo to fail
        String fakeBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=";
        fakeRepository.itemsToReturn.add(new AdminImageItem("event123", fakeBase64));

        // Act
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            viewModel.deleteImage("event123");
        });

        // Assert
        String message = getOrAwaitValue(viewModel.getMessage());

        // Verify we get the error message
        assertTrue(message.contains("Failed"));
    }

    /**
     * A helper method to synchronously retrieve the value from a {@link androidx.lifecycle.LiveData} object.
     * <p>
     * This is necessary in tests because LiveData updates happen asynchronously on the Main Thread.
     * It waits up to 2 seconds for the value to be emitted.
     *
     * @param liveData The LiveData to observe.
     * @return The value emitted by the LiveData.
     * @throws InterruptedException If the thread is interrupted while waiting.
     * @throws RuntimeException If the value is never set within the timeout.
     */
    public static <T> T getOrAwaitValue(final androidx.lifecycle.LiveData<T> liveData) throws InterruptedException {
        final Object[] data = new Object[1];
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        androidx.lifecycle.Observer<T> observer = new androidx.lifecycle.Observer<T>() {
            @Override
            public void onChanged(@androidx.annotation.Nullable T o) {
                data[0] = o;
                latch.countDown();
                liveData.removeObserver(this);
            }
        };

        // Observers must be attached on the main thread
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            liveData.observeForever(observer);
        });

        // Wait for the value
        if (!latch.await(2, java.util.concurrent.TimeUnit.SECONDS)) {
            throw new RuntimeException("LiveData value was never set.");
        }

        return (T) data[0];
    }
}
