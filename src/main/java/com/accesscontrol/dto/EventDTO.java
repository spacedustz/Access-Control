package com.accesscontrol.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class EventDTO {
    private String system_date;
    private List<Event> events;

    @Getter
    public static class Event {
        private Extra extra;
    }

    @Getter
    public static class Extra {
        private int count;
    }
}