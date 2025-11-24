package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.data.AdminImageItem;
import com.example.lotteryevent.repository.IAdminImagesRepository;

import java.util.List;

public class AdminImagesViewModel extends ViewModel {

    private final IAdminImagesRepository repo;

    private final MutableLiveData<List<AdminImageItem>> images = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public AdminImagesViewModel(IAdminImagesRepository repo) {
        this.repo = repo;
    }

    public LiveData<List<AdminImageItem>> getImages() { return images; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<Boolean> isLoading() { return loading; }

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

    public void deleteImage(String eventId) {
        loading.postValue(true); // Show loading spinner

        repo.deleteImage(eventId, new IAdminImagesRepository.DeleteCallback() {
            @Override
            public void onSuccess() {
                loading.postValue(false);
                message.postValue("Image successfully deleted");
                fetchImages(); // Refresh the grid
            }

            @Override
            public void onFailure(Exception e) {
                loading.postValue(false);
                message.postValue("Failed to delete image: " + e.getMessage());
            }
        });
    }

}
