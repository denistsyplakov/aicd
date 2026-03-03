package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.AccountRepository;
import com.github.denistsyplakov.aicd.repo.RegionRepository;
import com.github.denistsyplakov.aicd.service.RegionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class RegionController {

    private final RegionRepository regionRepository;
    private final AccountRepository accountRepository;
    private final RegionService regionService;

    public RegionController(RegionRepository regionRepository,
                            AccountRepository accountRepository,
                            RegionService regionService) {
        this.regionRepository = regionRepository;
        this.accountRepository = accountRepository;
        this.regionService = regionService;
    }

    @PostMapping("/api/regions")
    @ResponseStatus(HttpStatus.CREATED)
    public RegionRepository.RegionDTO create(@RequestBody RegionRepository.RegionDTO dto) {
        return regionService.create(dto);
    }

    @GetMapping("/api/regions")
    public List<RegionRepository.RegionDTO> getAll() {
        return regionRepository.findAll();
    }

    @GetMapping("/api/regions/{id}")
    public RegionRepository.RegionDTO getById(@PathVariable Integer id) {
        return regionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found"));
    }

    @PutMapping("/api/regions/{id}")
    public RegionRepository.RegionDTO update(@PathVariable Integer id, @RequestBody RegionRepository.RegionDTO dto) {
        return regionService.update(id, dto);
    }

    @DeleteMapping("/api/regions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        regionService.delete(id);
    }

    @GetMapping("/api/regions/{id}/accounts")
    public List<AccountRepository.AccountDTO> getAccountsByRegion(@PathVariable Integer id) {
        if (!regionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found");
        }
        return accountRepository.findAllByRegionId(id);
    }
}
