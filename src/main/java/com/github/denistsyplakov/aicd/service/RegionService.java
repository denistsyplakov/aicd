package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.RegionRepository;
import com.github.denistsyplakov.aicd.repo.RegionRepository.RegionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegionService {

    @Autowired
    private RegionRepository regionRepository;

    public RegionDTO create(RegionDTO regionDTO) {
        try {
            return regionRepository.save(new RegionDTO(null, regionDTO.name()));
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("idx_region_name_unique")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Region name must be unique", e);
            }
            throw e;
        }
    }

    public RegionDTO update(Integer id, RegionDTO regionDTO) {
        if (!regionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found");
        }
        try {
            return regionRepository.save(new RegionDTO(id, regionDTO.name()));
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("idx_region_name_unique")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Region name must be unique", e);
            }
            throw e;
        }
    }

    public void delete(Integer id) {
        if (!regionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found");
        }
        try {
            regionRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Region is in use and cannot be deleted", e);
        }
    }
}
