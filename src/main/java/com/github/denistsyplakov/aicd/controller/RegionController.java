package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.AccountRepository;
import com.github.denistsyplakov.aicd.repo.RegionRepository;
import com.github.denistsyplakov.aicd.service.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RegionController {

    @Autowired
    RegionRepository regionRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    RegionService regionService;

    @PostMapping("/api/region")
    public RegionRepository.RegionDTO create(@RequestBody RegionRepository.RegionDTO region) {
        return regionService.create(region);
    }

    @GetMapping("/api/region")
    public Iterable<RegionRepository.RegionDTO> getAll() {
        return regionRepository.findAll();
    }

    @GetMapping("/api/region/{id}")
    public RegionRepository.RegionDTO getById(@PathVariable int id) {
        return regionRepository.findById(id).orElseThrow();
    }

    @PutMapping("/api/region")
    public RegionRepository.RegionDTO update(@RequestBody RegionRepository.RegionDTO region) {
        return regionService.update(region);
    }

    @DeleteMapping("/api/region/{id}")
    public void delete(@PathVariable int id) {
        regionService.delete(id);
    }

    @GetMapping("/api/region/{id}/accounts")
    public List<AccountRepository.AccountDTO> getAccountsForRegion(@PathVariable int id) {
        return accountRepository.findByRegionId(id);
    }
}
