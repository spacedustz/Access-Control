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
public class Count {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long count;
    private LocalDateTime eventTime;

    private Count(Long count, LocalDateTime eventTime) {
        this.count = count;
        this.eventTime = eventTime;
    }

    public static Count createOf(Long count, LocalDateTime eventTime) {
        return new Count(count, eventTime);
    }
}
