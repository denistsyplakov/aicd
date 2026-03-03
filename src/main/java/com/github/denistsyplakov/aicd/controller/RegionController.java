package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.AccountRepository;
import com.github.denistsyplakov.aicd.repo.RegionRepository;
import com.github.denistsyplakov.aicd.service.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class RegionController {

    @Autowired
    RegionRepository regionRepository;

    @Autowired
    RegionService regionService;

    @GetMapping("/api/region")
    public Iterable<RegionRepository.RegionDTO> getAll() {
        return regionRepository.findAll();
    }

    @GetMapping("/api/region/{id}")
    public RegionRepository.RegionDTO getById(@PathVariable Integer id) {
        return regionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/api/region")
    @ResponseStatus(HttpStatus.CREATED)
    public RegionRepository.RegionDTO create(@RequestBody RegionRepository.RegionDTO dto) {
        return regionService.create(dto.name());
    }

    @PutMapping("/api/region/{id}")
    public RegionRepository.RegionDTO update(@PathVariable Integer id,
                                             @RequestBody RegionRepository.RegionDTO dto) {
        return regionService.update(id, dto.name());
    }

    @DeleteMapping("/api/region/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        regionService.delete(id);
    }

    @GetMapping("/api/region/{id}/accounts")
    public List<AccountRepository.AccountDTO> getAccounts(@PathVariable Integer id) {
        regionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return regionRepository.findAccountsByRegionId(id);
    }
}
