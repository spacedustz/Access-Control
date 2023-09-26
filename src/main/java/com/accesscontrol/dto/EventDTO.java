package com.accesscontrol.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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

    // Event 객체 Response 용도
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Response {
        private Long id; // ID
        private int occupancy; // 현재 Room 내 인원 수 : InCount - OutCount
        private int maxCount; // 최대 수용 인원
        private String customStatus; // Room 상태
        private String relayUrl; // Relay URL
        private String status; // status
        private String openTime; // 운영시작 시간
        private String closeTime; // 운영 종료 시간

        private Response(int maxCount) {
            this.maxCount = maxCount;
        }

        public static EventDTO.Response fromEntity(com.accesscontrol.entity.Event entity) {
            return new EventDTO.Response(
                    entity.getId(),
                    entity.getOccupancy(),
                    entity.getMaxCount(),
                    entity.getCustomStatus(),
                    entity.getRelayUrl(),
                    entity.getStatus().getDesc(),
                    entity.getOpenTime(),
                    entity.getCloseTime()
            );
        }

        // maxCount 업데이트 용
        public static EventDTO.Response fromEntityForUpdateMaxCount(com.accesscontrol.entity.Event entity) {
            return new EventDTO.Response(
                    entity.getMaxCount()
            );
        }
    }
}