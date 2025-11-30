package com.example.lotteryevent.repository;

import com.example.lotteryevent.data.AdminImageItem;
import java.util.ArrayList;
import java.util.List;


/**
 * A fake implementation of {@link IAdminImagesRepository} for testing purposes.
 * <p>
 * This class simulates the behavior of a real repository (like Firebase) but runs
 * synchronously and in-memory. It allows tests to:
 * <ul>
 *     <li>Pre-load data to return via {@code itemsToReturn}.</li>
 *     <li>Inspect if methods were called via {@code deleteWasCalled}.</li>
 *     <li>Simulate failure scenarios via the {@code shouldFail} flag.</li>
 * </ul>
 * </p>
 */
public class FakeAdminImagesRepository implements IAdminImagesRepository {

    /**
     * A list of items that this repository will return when {@link #getAllImages(ImagesCallback)} is called.
     * Populate this list in your test setup to simulate fetching data.
     */
    public List<AdminImageItem> itemsToReturn = new ArrayList<>();

    /**
     * Flag indicating whether the {@link #deleteImage(String, DeleteCallback)} method was successfully called.
     */
    public boolean deleteWasCalled = false;

    /**
     * Stores the ID of the event that was passed to the last {@link #deleteImage(String, DeleteCallback)} call.
     */
    public String deletedEventId = null;

    /**
     * If set to true, repository methods will trigger their failure callbacks instead of success.
     * Useful for testing error handling logic.
     */
    public boolean shouldFail = false;

    /**
     * Simulates fetching all images.
     * Immediately returns the contents of {@link #itemsToReturn} via the success callback.
     *
     * @param callback The callback to receive the data.
     */
    @Override
    public void getAllImages(ImagesCallback callback) {
        /**
         * Simulate success immediately
         */
        callback.onSuccess(itemsToReturn);
    }

    /**
     * Simulates deleting an image.
     * <p>
     * If {@link #shouldFail} is true, it triggers {@code callback.onFailure}.
     * Otherwise, it records the call in {@link #deleteWasCalled} and {@link #deletedEventId},
     * then triggers {@code callback.onSuccess}.
     * </p>
     *
     * @param eventId  The ID of the event image to delete.
     * @param callback The callback to report success or failure.
     */
    @Override
    public void deleteImage(String eventId, DeleteCallback callback) {
        if (shouldFail) {
            /**
             * Throws exception on failure
             */
            callback.onFailure(new Exception("Network Error"));
        } else {
            deleteWasCalled = true;
            deletedEventId = eventId;
            callback.onSuccess();
        }
    }
}
