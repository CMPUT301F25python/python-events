package com.example.lotteryevent.repository;

import com.example.lotteryevent.data.AdminImageItem;

import java.util.List;

public interface IAdminImagesRepository {

    interface ImagesCallback {
        void onSuccess(List<AdminImageItem> images);
        void onFailure(Exception e);
    }

    interface DeleteCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    void getAllImages(ImagesCallback callback);

    void deleteImage(String eventId, DeleteCallback callback);
}
