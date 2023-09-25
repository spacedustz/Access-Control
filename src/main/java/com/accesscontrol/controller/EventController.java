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
    public ResponseEntity<String> updateStatus(@RequestParam String status) {
        return new ResponseEntity<>(eventService.updateCustomStatus(status), HttpStatus.OK);
    }

    // Event 객체 MaxCount 값 변경 API
    @PatchMapping("/update-max")
    public ResponseEntity<EventDTO.Response> updateMaxCount(@RequestParam String max) {
        return new ResponseEntity<>(EventDTO.Response.fromEntityForUpdateMaxCount(eventService.updateMaxCount(max)), HttpStatus.OK);
    }

    // 초기 데이터 로드용 API
    @GetMapping("/init")
    public ResponseEntity<EventDTO.Response> getInitData() {
        return new ResponseEntity<>(EventDTO.Response.fromEntityForViewer(eventService.getInitData()), HttpStatus.OK);
    }

    // 관리자 페이지용 Entity 조회
    @GetMapping("/stat")
    public ResponseEntity<EventDTO.Response> getDetail() {
        return new ResponseEntity<>(EventDTO.Response.fromEntityForAdmin(eventService.getInitData()), HttpStatus.OK);
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
}
