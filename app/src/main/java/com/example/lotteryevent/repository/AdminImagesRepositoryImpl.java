package com.example.lotteryevent.repository;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminImagesRepositoryImpl implements IAdminImagesRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public void getAllImages(ImagesCallback callback) {
        db.collection("images")
                .get()
                .addOnSuccessListener(q -> {
                    List<String> urls = new ArrayList<>();
                    q.getDocuments().forEach(doc -> {
                        String url = doc.getString("url");
                        if (url != null) urls.add(url);
                    });
                    callback.onSuccess(urls);
                })
                .addOnFailureListener(callback::onFailure);
    }
}
