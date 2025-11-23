package com.example.lotteryevent.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.lotteryevent.data.Event;
import com.example.lotteryevent.repository.IAdminEventsRepository;

import java.util.List;

public class AdminEventsViewModel extends ViewModel {
    private final IAdminEventsRepository repo;

    private final MutableLiveData<List<Event>> events = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public AdminEventsViewModel(IAdminEventsRepository repo) {
        this.repo = repo;
    }

    public LiveData<List<Event>> getEvents() { return events; }

    public LiveData<String> getMessage() { return message; }

    public void fetchAllEvents() {
        repo.getAllEvents(new IAdminEventsRepository.EventsCallback() {
            @Override
            public void onSuccess(List<Event> list) {
                events.postValue(list);
            }

            @Override
            public void onFailure(Exception e) {
                message.postValue("Failure to load events: " + e.getMessage());
            }
        });
    }

}
