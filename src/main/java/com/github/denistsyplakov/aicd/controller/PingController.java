package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.PingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @Autowired
    PingRepository pingRepository;

    @GetMapping("/api/ping")
    public int ping() {
        return pingRepository.ping().reply();
    }

}
