package com.accesscontrol.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CountDTO {
    private Extra extra;
    private LocalDateTime eventTime;

    @Getter
    public static class Extra {
        private Long count;
    }
}
