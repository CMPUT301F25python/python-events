package com.example.lotteryevent.repository;

import com.example.lotteryevent.data.AdminImageItem;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminImagesRepositoryImpl implements IAdminImagesRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public void getAllImages(ImagesCallback callback) {
        db.collection("events")
                .get()
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
                .addOnFailureListener(callback::onFailure);
    }

    @Override
    public void deleteImage(String eventId, DeleteCallback callback) {
        db.collection("events").document(eventId)
                .update("posterImageUrl", null)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }
}
