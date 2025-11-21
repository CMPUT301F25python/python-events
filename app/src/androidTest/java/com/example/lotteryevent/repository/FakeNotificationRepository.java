package com.example.lotteryevent.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.lotteryevent.data.Notification;
import java.util.ArrayList;
import java.util.List;

public class FakeNotificationRepository implements INotificationRepository {

    private final MutableLiveData<List<Notification>> notificationsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> messageLiveData = new MutableLiveData<>();

    // Helper list to modify data easily during tests
    private final List<Notification> inMemoryList = new ArrayList<>();

    @Override
    public LiveData<List<Notification>> getNotifications() {
        return notificationsLiveData;
    }

    @Override
    public LiveData<Boolean> isLoading() {
        return isLoadingLiveData;
    }

    @Override
    public LiveData<String> getMessage() {
        return messageLiveData;
    }

    @Override
    public void markNotificationAsSeen(String notificationId) {
        // Iterate through our in-memory list to find the item
        for (Notification n : inMemoryList) {
            if (n.getNotificationId().equals(notificationId)) {
                n.setSeen(true);
                break;
            }
        }
        // Refresh the LiveData to trigger UI updates
        notificationsLiveData.postValue(new ArrayList<>(inMemoryList));
    }

    @Override
    public void detachListener() {
        // No-op for fake
    }

    // --- Test Helper Methods ---

    /**
     * Helper to set the initial state of the repository for testing.
     */
    public void setNotifications(List<Notification> notifications) {
        inMemoryList.clear();
        inMemoryList.addAll(notifications);
        notificationsLiveData.postValue(new ArrayList<>(inMemoryList));
    }

    public List<Notification> getInMemoryList() {
        return inMemoryList;
    }

    public void setMessage(String msg) {
        messageLiveData.postValue(msg);
    }
}