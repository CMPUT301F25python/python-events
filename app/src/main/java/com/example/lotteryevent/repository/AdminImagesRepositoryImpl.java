package com.example.lotteryevent.repository;

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
                .addOnSuccessListener(q -> {
                    List<String> urls = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : q) {
                        String base64 = doc.getString("posterImageUrl");

                        if (base64 != null && !base64.trim().isEmpty()) {
                            urls.add(base64);
                        }
                    }
                    callback.onSuccess(urls);
                })
                .addOnFailureListener(callback::onFailure);
    }
}
