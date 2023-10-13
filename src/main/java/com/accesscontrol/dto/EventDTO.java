package com.accesscontrol.dto;

import lombok.Getter;

import java.util.List;

// RabbitMQ에서 들어오는 데이터를 받을 용도
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
        private String crossing_direction;
    }
}