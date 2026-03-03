package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.SowRepository;
import com.github.denistsyplakov.aicd.service.SowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class SowController {

    record SearchRequest(String query, int maxDoc, double minRank, int maxTextLength) {}

    @Autowired
    SowRepository sowRepository;

    @Autowired
    SowService sowService;

    @GetMapping("/api/sow")
    public Iterable<SowRepository.SowDTO> getAll() {
        return sowRepository.findAll();
    }

    @GetMapping("/api/sow/{id}")
    public SowRepository.SowDTO getById(@PathVariable Integer id) {
        return sowRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/api/sow")
    @ResponseStatus(HttpStatus.CREATED)
    public SowRepository.SowDTO create(@RequestBody SowRepository.SowDTO dto) {
        return sowService.create(dto);
    }

    @PutMapping("/api/sow/{id}")
    public SowRepository.SowDTO update(@PathVariable Integer id,
                                       @RequestBody SowRepository.SowDTO dto) {
        return sowService.update(id, dto);
    }

    @DeleteMapping("/api/sow/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        sowService.delete(id);
    }

    @PostMapping("/api/sow/search")
    public List<SowRepository.SowDTO> search(@RequestBody SearchRequest req) {
        List<SowRepository.SearchResultDTO> results =
                sowRepository.search(req.query(), req.minRank(), req.maxDoc());
        return results.stream()
                .map(r -> {
                    String text = r.text();
                    if (text != null && text.length() > req.maxTextLength()) {
                        text = text.substring(0, req.maxTextLength()) + "... ";
                    }
                    return new SowRepository.SowDTO(r.id(), r.accountId(), r.date(),
                            r.title(), r.amount(), r.description(), text);
                })
                .toList();
    }
}
