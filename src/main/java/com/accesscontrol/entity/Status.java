package com.accesscontrol.entity;

import lombok.Getter;

// 현재 방안의 상태
public enum Status {
    LOW("입장 가능합니다."),
    MEDIUM("혼잡합니다."),
    HIGH("만십입니다."),
    NOT_OPERATING("운영시간이 아닙니다.");

    @Getter
    private final String desc;

    Status(String desc) {
        this.desc = desc;
    }
}
