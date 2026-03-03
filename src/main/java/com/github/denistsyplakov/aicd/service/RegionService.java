package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.RegionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegionService {

    @Autowired
    RegionRepository regionRepository;

    public RegionRepository.RegionDTO create(RegionRepository.RegionDTO region) {
        try {
            return regionRepository.save(region);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Region name already exists");
        }
    }

    public RegionRepository.RegionDTO update(RegionRepository.RegionDTO region) {
        if (!regionRepository.existsById(region.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found");
        }
        try {
            return regionRepository.save(region);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Region name already exists");
        }
    }

    public void delete(int id) {
        try {
            regionRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Region is in use", e);
        }
    }
}
