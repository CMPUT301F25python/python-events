package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.lotteryevent.data.Notification;
import com.example.lotteryevent.repository.INotificationRepository;
import java.util.List;
import java.util.Objects;

/**
 * ViewModel for the NotificationsFragment.
 * This class is responsible for managing the data and business logic related to notifications.
 * It communicates with the INotificationRepository to fetch and update data, and it exposes
 * this data to the UI via LiveData. It also handles the lifecycle of the real-time data listener.
 */
public class NotificationsViewModel extends ViewModel {

    private final INotificationRepository notificationRepository;

    private LiveData<List<Notification>> notificationsForEvent;

    // This LiveData will be used to signal navigation events to the Fragment.
    private final MutableLiveData<String> _navigateToEventDetails = new MutableLiveData<>();

    /**
     * Constructs the ViewModel with a dependency on the notification repository.
     * @param repository An implementation of INotificationRepository.
     */
    public NotificationsViewModel(INotificationRepository repository) {
        this.notificationRepository = repository;
        this.notificationsForEvent = repository.getNotificationsForEvent();
    }


    public LiveData<List<Notification>> getNotifications() {
        return notificationRepository.getNotifications();
    }

    public LiveData<List<Notification>> getNotificationsForEvent() {
        return notificationsForEvent;
    }

    public LiveData<Boolean> isLoading() {
        return notificationRepository.isLoading();
    }

    public LiveData<String> getMessage() {
        return notificationRepository.getMessage();
    }

    /**
     * Exposes the navigation event as a LiveData object. The Fragment will observe this
     * to know when to navigate to the EventDetailsFragment.
     */
    public LiveData<String> getNavigateToEventDetails() {
        return _navigateToEventDetails;
    }

    public void loadNotificationsForEvent(String eventId) {
        notificationRepository.fetchNotificationsForEvent(eventId);
    }

    // --- Business Logic ---

    /**
     * This method contains the logic from the original fragment's `redirectOnNotificationClick`.
     * It marks the notification as seen and triggers navigation if necessary.
     *
     * @param notification The notification that the user clicked on in the RecyclerView.
     */
    public void onNotificationClicked(Notification notification) {
        // If the notification hasn't been seen, tell the repository to update it.
        if (notification.getSeen() != Boolean.TRUE) {
            notificationRepository.markNotificationAsSeen(notification.getNotificationId());
        }

        // If the notification is a "lottery_win", post the event ID to the navigation LiveData.
        if (Objects.equals(notification.getType(), "lottery_win")) {
            _navigateToEventDetails.setValue(notification.getEventId());
        }
    }

    /**
     * Handles the initial state of the fragment, specifically if it was opened
     * by a tap on a system notification from the status bar.
     *
     * @param notificationId The ID of the notification, passed via fragment arguments.
     */
    public void processInitialNotification(String notificationId) {
        if (notificationId != null) {
            notificationRepository.markNotificationAsSeen(notificationId);
        }
    }

    /**
     * The Fragment must call this method after it has successfully navigated.
     * This prevents the navigation from being re-triggered on screen rotation.
     */
    public void onNavigationComplete() {
        _navigateToEventDetails.setValue(null);
    }


    // --- Lifecycle Management ---

    /**
     * This method is called by the Android framework when the ViewModel is about to be destroyed.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        notificationRepository.detachListener();
    }
}