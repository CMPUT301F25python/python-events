package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.data.AdminImageItem;
import com.example.lotteryevent.repository.IAdminImagesRepository;

import java.util.List;

/**
 * ViewModel for the Admin Images Dashboard.
 * <p>
 * This class manages the UI-related data for {@link com.example.lotteryevent.ui.admin.AdminImagesFragment}.
 * It acts as a bridge between the View and the {@link IAdminImagesRepository}, handling:
 * <ul>
 *     <li>Fetching the list of all event images.</li>
 *     <li>Deleting specific images.</li>
 *     <li>Managing loading states and status messages.</li>
 * </ul>
 * </p>
 */
public class AdminImagesViewModel extends ViewModel {

    private final IAdminImagesRepository repo;

    private final MutableLiveData<List<AdminImageItem>> images = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);


    /**
     * Constructs the ViewModel with a specific repository implementation.
     *
     * @param repo The repository used to fetch and delete image data.
     */
    public AdminImagesViewModel(IAdminImagesRepository repo) {
        this.repo = repo;
    }

    /**
     * Observes the list of images to be displayed in the grid.
     * @return LiveData containing the list of {@link AdminImageItem}.
     */
    public LiveData<List<AdminImageItem>> getImages() {
        return images;
    }

    /**
     * Observes status messages (success or error) to display to the user (e.g., via Toast).
     * @return LiveData containing the message string.
     */
    public LiveData<String> getMessage() {
        return message;
    }

    /**
     * Observes the loading state of asynchronous operations.
     * @return LiveData containing {@code true} if an operation is in progress, {@code false} otherwise.
     */
    public LiveData<Boolean> isLoading() {
        return loading;
    }


    /**
     * Initiates a request to fetch all event images from the repository.
     * <p>
     * Sets the loading state to true before the request and updates the {@code images} LiveData
     * upon success, or the {@code message} LiveData upon failure.
     * </p>
     */
    public void fetchImages() {
        loading.postValue(true);
        repo.getAllImages(new IAdminImagesRepository.ImagesCallback() {
            @Override
            public void onSuccess(List<AdminImageItem> list) {
                loading.postValue(false);
                images.postValue(list);
            }

            @Override
            public void onFailure(Exception e) {
                loading.postValue(false);
                message.postValue("Failed to load images");
            }
        });
    }

    /**
     * Initiates a request to delete a specific image associated with an event ID.
     * <p>
     * Upon successful deletion, it updates the status message and automatically
     * triggers {@link #fetchImages()} to refresh the list.
     * </p>
     *
     * @param eventId The unique identifier of the event whose image should be deleted.
     */
    public void deleteImage(String eventId) {
        loading.postValue(true); // Show loading spinner

        repo.deleteImage(eventId, new IAdminImagesRepository.DeleteCallback() {
            /**
             * Posts successful deletion and fetches image
             */
            @Override
            public void onSuccess() {
                loading.postValue(false);
                message.postValue("Image successfully deleted");
                fetchImages(); // Refresh the grid
            }

            /**
             * Posts exception to message
             * @param e The exception describing the failure.
             */
            @Override
            public void onFailure(Exception e) {
                loading.postValue(false);
                message.postValue("Failed to delete image: " + e.getMessage());
            }
        });
    }

}
