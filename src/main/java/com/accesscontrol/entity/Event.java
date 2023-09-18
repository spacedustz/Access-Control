package com.accesscontrol.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    @Setter
    private int inCount = 0;

    @Setter
    private int outCount = 0;

    private LocalDateTime eventTime;

    private Event(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    // 생성자 - 정적 팩토리 함수
    public static Event createOf(LocalDateTime eventTime) {
        return new Event(eventTime);
    }

    // 하루가 지날때 마다 count 수 초기화
    public void initializeCount() {
        this.inCount = 0;
        this.outCount = 0;
    }
}
