package com.example.lotteryevent.repository;

import com.example.lotteryevent.data.Event;
import java.util.List;

public interface IAdminEventsRepository {

    void getAllEvents(EventsCallback callback);

    interface EventsCallback {
        void onSuccess(List<Event> events);
        void onFailure(Exception e);
    }
}
