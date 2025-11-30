package com.example.lotteryevent.repository;

import com.example.lotteryevent.data.AdminImageItem;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the {@link IAdminImagesRepository} interface.
 * <p>
 * This class is responsible for interacting with Firebase Firestore to perform data operations
 * related to event images for the administrative dashboard. It handles fetching all event images
 * and deleting specific event images.
 * </p>
 */
public class AdminImagesRepositoryImpl implements IAdminImagesRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();


    /**
     * Retrieves a list of all images associated with events from the "events" collection in Firestore.
     * <p>
     * This method queries the "events" collection and filters for documents that contain
     * a non-null and non-empty "posterImageUrl". It maps the results to {@link AdminImageItem} objects.
     * </p>
     *
     * @param callback The callback interface to handle the success (returning the list of items)
     *                 or failure of the Firestore query.
     */
    @Override
    public void getAllImages(ImagesCallback callback) {
        db.collection("events")
                .get()
                /**
                 * Handles the successful retrieval of event documents from Firestore.
                 * This listener iterates over the query results, extracts each event's
                 * poster image and ID, and converts them into {@link AdminImageItem} objects.
                 * Once processed, the resulting list is passed back through the callback.
                 * @param querySnapshot snapshot containing all documents returned by the Firestore query
                 */
                .addOnSuccessListener(querySnapshot -> {
                    List<AdminImageItem> imageItems = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String base64Image = doc.getString("posterImageUrl");
                        String eventId = doc.getId(); // Get the Document ID

                        if (base64Image != null && !base64Image.isEmpty()) {
                            // Store both ID and Image
                            imageItems.add(new AdminImageItem(eventId, base64Image));
                        }
                    }
                    callback.onSuccess(imageItems);
                })
                /**
                 * Handles a Firestore query failure by passing the error back through
                 * the provided callback's onFailure method.
                 */
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Deletes (removes) an image associated with a specific event.
     * <p>
     * Instead of deleting the entire event document, this method updates the
     * "posterImageUrl" field to null for the specified event ID.
     * </p>
     *
     * @param eventId  The unique ID of the event document to update.
     * @param callback The callback interface to handle the success or failure of the update operation.
     */
    @Override
    public void deleteImage(String eventId, DeleteCallback callback) {
        db.collection("events").document(eventId)
                .update("posterImageUrl", null)
                /**
                 * Handles the successful update of the event document in Firestore.
                 * Once the "posterImageUrl" field is set to null, the success callback
                 * is triggered (if provided) to notify the caller.
                 * @param aVoid placeholder, no usable data
                 */
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                /**
                 * Handles a failure that occurs while attempting to update the event
                 * document in Firestore. If a callback is provided, the error is passed
                 * to the caller through the onFailure method.
                 * @param e exception thrown
                 */
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }
}
