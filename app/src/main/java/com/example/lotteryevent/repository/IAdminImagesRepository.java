package com.example.lotteryevent.repository;

import com.example.lotteryevent.data.AdminImageItem;

import java.util.List;


/**
 * Interface defining the contract for the repository responsible for managing Admin Image data.
 * <p>
 * This interface abstracts the data source (e.g., Firebase Firestore), allowing for
 * easier testing and cleaner architecture by decoupling the ViewModel from the specific
 * data implementation.
 * </p>
 */
public interface IAdminImagesRepository {

    /**
     * Callback interface to handle the asynchronous results of fetching images.
     */
    interface ImagesCallback {

        /**
         * Called when the operation successfully retrieves a list of images.
         * @param images The list of {@link AdminImageItem} retrieved.
         */
        void onSuccess(List<AdminImageItem> images);

        /**
         * Called when the operation fails.
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }


    /**
     * Callback interface to handle the asynchronous results of a delete operation.
     */
    interface DeleteCallback {

        /**
         * Called when the delete operation completes successfully.
         */
        void onSuccess();

        /**
         * Called when the delete operation fails.
         * @param e The exception describing the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Retrieves all images available for administrative review.
     *
     * @param callback The {@link ImagesCallback} to handle the result.
     */
    void getAllImages(ImagesCallback callback);

    /**
     * Deletes the image associated with a specific event ID.
     *
     * @param eventId  The unique identifier of the event whose image should be deleted.
     * @param callback The {@link DeleteCallback} to handle the result.
     */
    void deleteImage(String eventId, DeleteCallback callback);
}
