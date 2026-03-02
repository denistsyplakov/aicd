package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.AccountRepository;
import com.github.denistsyplakov.aicd.repo.AccountRepository.AccountDTO;
import com.github.denistsyplakov.aicd.repo.RegionRepository;
import com.github.denistsyplakov.aicd.repo.RegionRepository.RegionDTO;
import com.github.denistsyplakov.aicd.service.RegionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/region")
public class RegionController {

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private RegionService regionService;

    @Autowired
    private AccountRepository accountRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegionDTO create(@RequestBody RegionDTO regionDTO) {
        return regionService.create(regionDTO);
    }

    @GetMapping
    public Iterable<RegionDTO> getAll() {
        return regionRepository.findAll();
    }

    @GetMapping("/{id}")
    public RegionDTO getById(@PathVariable Integer id) {
        return regionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found"));
    }

    @PutMapping("/{id}")
    public RegionDTO update(@PathVariable Integer id, @RequestBody RegionDTO regionDTO) {
        return regionService.update(id, regionDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        regionService.delete(id);
    }

    @GetMapping("/{id}/account")
    public Iterable<AccountDTO> getAccounts(@PathVariable Integer id) {
        if (!regionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found");
        }
        return accountRepository.findAllByRegionId(id);
    }
}
