package com.example.lotteryevent;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.example.lotteryevent.data.AdminImageItem;
import com.example.lotteryevent.repository.IAdminImagesRepository;
import com.example.lotteryevent.viewmodels.AdminImagesViewModel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the {@link AdminImagesViewModel}.
 * <p>
 * This class tests the business logic of the ViewModel in isolation using Mockito.
 * It verifies that:
 * <ul>
 *     <li>Fetching images updates the images LiveData correctly.</li>
 *     <li>Deleting an image triggers the repository and refreshes the list.</li>
 * </ul>
 */
public class AdminImagesViewModelTest {

    /**
     * Rule to force LiveData to execute immediately on the main thread.
     * This is required for testing LiveData synchronously.
     */
    @Rule
    public InstantTaskExecutorRule instantExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private IAdminImagesRepository repository;

    @Mock
    private Observer<List<AdminImageItem>> imagesObserver;

    @Mock
    private Observer<String> messageObserver;

    private AdminImagesViewModel viewModel;

    /**
     * Sets up the test environment.
     * Initializes mocks and attaches observers to the ViewModel's LiveData
     * to verify updates.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new AdminImagesViewModel(repository);
        viewModel.getImages().observeForever(imagesObserver);
        viewModel.getMessage().observeForever(messageObserver);
    }


    /**
     * Verifies that when {@code fetchImages()} is called, the ViewModel:
     * 1. Calls the repository's {@code getAllImages} method.
     * 2. Updates the {@code images} LiveData when the repository returns success.
     */
    @Test
    public void fetchImages_success_updatesLiveData() {
        // Arrange
        List<AdminImageItem> mockList = new ArrayList<>();
        mockList.add(new AdminImageItem("event1", "base64string"));

        // Act
        viewModel.fetchImages();

        // Capture the callback passed to the repository
        ArgumentCaptor<IAdminImagesRepository.ImagesCallback> captor = ArgumentCaptor.forClass(IAdminImagesRepository.ImagesCallback.class);
        verify(repository).getAllImages(captor.capture());

        // Simulate Repository Success
        captor.getValue().onSuccess(mockList);

        // Assert
        verify(imagesObserver).onChanged(mockList);
    }

    /**
     * Verifies that when {@code deleteImage()} is called, the ViewModel:
     * 1. Calls the repository's {@code deleteImage} method.
     * 2. Updates the status message upon success.
     * 3. Automatically refreshes the image list (calls {@code getAllImages} again).
     */
    @Test
    public void deleteImage_success_updatesMessageAndRefreshes() {
        // Act
        viewModel.deleteImage("event123");

        // Capture callback
        ArgumentCaptor<IAdminImagesRepository.DeleteCallback> captor = ArgumentCaptor.forClass(IAdminImagesRepository.DeleteCallback.class);
        verify(repository).deleteImage(any(String.class), captor.capture());

        // Simulate Repository Success
        captor.getValue().onSuccess();

        // Assert
        verify(messageObserver).onChanged("Image successfully deleted");
        // Try to fetch images again
        verify(repository).getAllImages(any());
    }
}
