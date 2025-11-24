package com.example.lotteryevent.repository;

import java.util.List;

public interface IAdminImagesRepository {

    interface ImagesCallback {
        void onSuccess(List<String> images);
        void onFailure(Exception e);
    }

    void getAllImages(ImagesCallback callback);
}
