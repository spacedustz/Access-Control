package com.accesscontrol.service;

import com.accesscontrol.repository.EventRepository;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WebSocketEntityListener {

    private final EventRepository eventRepository;
    private final SimpleMessaging

    @PostPersist
    @PostUpdate
    public void detectEntityUpdate() {

    }
}
