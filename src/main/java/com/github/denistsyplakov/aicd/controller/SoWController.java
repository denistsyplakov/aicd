package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.SoWRepository;
import com.github.denistsyplakov.aicd.service.SoWService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SoWController {

    @Autowired
    SoWRepository sowRepository;

    @Autowired
    SoWService sowService;

    @PostMapping("/api/sow")
    public SoWRepository.SoWDTO create(@RequestBody SoWRepository.SoWDTO sow) {
        return sowService.create(sow);
    }

    @GetMapping("/api/sow")
    public Iterable<SoWRepository.SoWDTO> getAll() {
        return sowRepository.findAll();
    }

    @GetMapping("/api/sow/{id}")
    public SoWRepository.SoWDTO getById(@PathVariable int id) {
        return sowRepository.findById(id).orElseThrow();
    }

    @PutMapping("/api/sow")
    public SoWRepository.SoWDTO update(@RequestBody SoWRepository.SoWDTO sow) {
        return sowService.update(sow);
    }

    @DeleteMapping("/api/sow/{id}")
    public void delete(@PathVariable int id) {
        sowService.delete(id);
    }

    @GetMapping("/api/sow/search")
    public List<SoWRepository.SoWDTO> search(
            @RequestParam String query,
            @RequestParam int maxDoc,
            @RequestParam double minRank,
            @RequestParam int maxTextLength) {
        List<SoWRepository.SoWDTO> results = sowRepository.search(query, maxDoc, minRank);
        return results.stream()
                .map(sow -> {
                    String text = sow.text();
                    if (text != null && text.length() > maxTextLength) {
                        text = text.substring(0, maxTextLength) + "... ";
                    }
                    return new SoWRepository.SoWDTO(
                            sow.id(),
                            sow.accountId(),
                            sow.date(),
                            sow.title(),
                            sow.amount(),
                            sow.description(),
                            text
                    );
                })
                .collect(Collectors.toList());
    }
}
