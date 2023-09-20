package com.accesscontrol.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ViewController {

    @RequestMapping("/admin")
    public String adminPage() {
        return "forward:/admin.html";
    }
}
