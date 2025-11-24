package com.example.lotteryevent.repository;

import com.example.lotteryevent.data.Event;
import java.util.List;

/**
 * Interface to allow the fetching of all events from firestore and
 * returning them through callback
 */
public interface IAdminEventsRepository {

    void getAllEvents(EventsCallback callback);

    interface EventsCallback {
        void onSuccess(List<Event> events);
        void onFailure(Exception e);
    }
}
