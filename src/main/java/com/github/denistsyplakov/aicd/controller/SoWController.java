package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.SoWRepository;
import com.github.denistsyplakov.aicd.repo.SoWRepository.SoWDTO;
import com.github.denistsyplakov.aicd.service.SoWService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class SoWController {

    @Autowired
    private SoWRepository sowRepository;

    @Autowired
    private SoWService sowService;

    @PostMapping("/api/sow")
    @ResponseStatus(HttpStatus.CREATED)
    public SoWDTO create(@RequestBody SoWDTO sowDTO) {
        return sowService.create(sowDTO);
    }

    @GetMapping("/api/sow")
    public Iterable<SoWDTO> getAll() {
        return sowRepository.findAll();
    }

    @GetMapping("/api/sow/{id}")
    public SoWDTO getById(@PathVariable Integer id) {
        return sowRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SoW not found"));
    }

    @PutMapping("/api/sow/{id}")
    public SoWDTO update(@PathVariable Integer id, @RequestBody SoWDTO sowDTO) {
        return sowService.update(id, sowDTO);
    }

    @DeleteMapping("/api/sow/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        sowService.delete(id);
    }

    @GetMapping("/api/sow/search")
    public List<SoWDTO> search(
            @RequestParam String query,
            @RequestParam(name = "max_doc", defaultValue = "10") int maxDoc,
            @RequestParam(name = "min_rank", defaultValue = "0.0") float minRank,
            @RequestParam(name = "max_text_length", defaultValue = "255") int maxTextLength
    ) {
        return sowRepository.search(query, maxDoc, minRank, maxTextLength);
    }
}
