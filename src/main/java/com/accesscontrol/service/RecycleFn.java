package com.accesscontrol.service;

import com.accesscontrol.entity.Event;
import org.springframework.stereotype.Service;

@Service
public class RecycleFn {
    public void autoUpdateStatus(Event event) {
        if (event != null) {
            if (event.getOccupancy() <= 9) {
                event.setStatus("입장 가능합니다.");
            } else if (event.getOccupancy() <= 14) {
                event.setStatus("혼잡합니다.");
            } else if (event.getOccupancy() >= 15) {
                event.setStatus("만실입니다.");
            }
        }
    }
}
