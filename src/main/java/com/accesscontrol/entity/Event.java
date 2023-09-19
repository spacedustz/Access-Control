package com.accesscontrol.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime eventTime; // 이벤트 시간

    @Setter
    private int inCount = 0; // 입장 카운트

    @Setter
    private int outCount = 0; // 퇴장 카운트

    @Setter
    private int occupancy = 0; // 현재 Room 내 인원 수 : InCount - OutCount

    @Setter
    private int maxCount = 15; // 최대 수용 인원

    private String relayUrl = ""; // Relay URL

    @Setter
    private String status = "입장 가능합니다."; // Room 상태

    private Event(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    // 생성자 - 정적 팩토리 함수
    public static Event createOf(LocalDateTime eventTime) {
        return new Event(eventTime);
    }
}
