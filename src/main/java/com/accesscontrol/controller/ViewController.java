package com.accesscontrol.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

// 어드민 페이지 분리를 위한 컨트롤러
@Controller
public class ViewController {
    @RequestMapping("/admin")
    public String adminPage() {
        return "forward:/admin.html";
    }
}
