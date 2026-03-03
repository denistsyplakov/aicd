package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.PingRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    private final PingRepository pingRepository;

    public PingController(PingRepository pingRepository) {
        this.pingRepository = pingRepository;
    }

    @GetMapping("/api/ping")
    public int ping() {
        return pingRepository.ping().reply();
    }

}
