package com.accesscontrol.controller;

import com.accesscontrol.dto.EventDTO;
import com.accesscontrol.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ws")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    // Event 객체 Status 값 변경 API
    @PatchMapping("/update-status")
    public void updateStatus(@RequestParam String status) {
        eventService.updateCustomStatus(status);
    }

    // Event 객체 MaxCount 값 변경 API
    @PatchMapping("/update-max")
    public void updateMaxCount(@RequestParam String max) {
        eventService.updateMaxCount(max);
    }

    // 초기 데이터 로드용 API
    @GetMapping("/init")
    public void getInitData() {
        eventService.getInitData();
    }

    // 현재 재실 인원 변경 API - 증가
    @PatchMapping("/increase-occupancy")
    public void increaseOccupancy(@RequestParam int num) {
        eventService.increaseOccupancy(num);
    }

    // 현재 재실 인원 변경 API - 감소
    @PatchMapping("/decrease-occupancy")
    public void decreaseOccupancy(@RequestParam int num) {
        eventService.decreaseOccupancy(num);
    }

    /* 운영 시간 조회, 변경 API */
    @GetMapping("/operation-time")
    public void getOperationTime() {
        eventService.getOperationTime();
    }

    // 운영 시작 시간 변경
    @PatchMapping("/open-time")
    public void updateOpenTime(@RequestParam String openTime) {
        eventService.setOpenTime(openTime);
    }

    // 운영 종료 시간 변경
    @PatchMapping("/close-time")
    public void updateCloseTime(@RequestParam String closeTime) {
        eventService.setCloseTime(closeTime);
    }

    // Relay URL 변경
    @PatchMapping("/relay")
    public void updateRelayUrl(@RequestParam String url) {
        eventService.setRelayUrl(url);
    }
}
