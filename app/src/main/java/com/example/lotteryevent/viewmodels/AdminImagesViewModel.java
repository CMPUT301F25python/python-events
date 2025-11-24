package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.repository.IAdminImagesRepository;

import java.util.List;

public class AdminImagesViewModel extends ViewModel {

    private final IAdminImagesRepository repo;

    private final MutableLiveData<List<String>> images = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public AdminImagesViewModel(IAdminImagesRepository repo) {
        this.repo = repo;
    }

    public LiveData<List<String>> getImages() { return images; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<Boolean> isLoading() { return loading; }

    public void fetchImages() {
        loading.postValue(true);
        repo.getAllImages(new IAdminImagesRepository.ImagesCallback() {
            @Override
            public void onSuccess(List<String> list) {
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
}
