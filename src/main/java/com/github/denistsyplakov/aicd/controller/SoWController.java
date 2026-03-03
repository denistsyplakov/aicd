package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.SoWRepository;
import com.github.denistsyplakov.aicd.service.SoWService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class SoWController {

    private final SoWRepository soWRepository;
    private final SoWService soWService;

    public SoWController(SoWRepository soWRepository, SoWService soWService) {
        this.soWRepository = soWRepository;
        this.soWService = soWService;
    }

    @PostMapping("/api/sows")
    @ResponseStatus(HttpStatus.CREATED)
    public SoWRepository.SoWDTO create(@RequestBody SoWRepository.SoWDTO dto) {
        return soWService.create(dto);
    }

    @GetMapping("/api/sows")
    public List<SoWRepository.SoWDTO> getAll() {
        return soWRepository.findAll();
    }

    @GetMapping("/api/sows/{id}")
    public SoWRepository.SoWDTO getById(@PathVariable Integer id) {
        return soWRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SoW not found"));
    }

    @PutMapping("/api/sows/{id}")
    public SoWRepository.SoWDTO update(@PathVariable Integer id, @RequestBody SoWRepository.SoWDTO dto) {
        return soWService.update(id, dto);
    }

    @DeleteMapping("/api/sows/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        soWService.delete(id);
    }

    @GetMapping("/api/sows/search")
    public List<SoWRepository.SoWDTO> search(@RequestParam("q") String queryText,
                                             @RequestParam("maxDoc") int maxDoc,
                                             @RequestParam("minRank") float minRank,
                                             @RequestParam("maxTextLength") int maxTextLength) {
        if (maxDoc <= 0 || minRank < 0 || maxTextLength < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid search parameters");
        }

        return soWRepository.search(queryText, maxDoc, minRank).stream()
                .map(dto -> new SoWRepository.SoWDTO(
                        dto.id(),
                        dto.accountId(),
                        dto.date(),
                        dto.title(),
                        dto.amount(),
                        dto.description(),
                        cropText(dto.text(), maxTextLength)
                ))
                .toList();
    }

    private String cropText(String text, int maxTextLength) {
        if (text == null || text.length() <= maxTextLength) {
            return text;
        }
        return text.substring(0, maxTextLength) + "... ";
    }
}
