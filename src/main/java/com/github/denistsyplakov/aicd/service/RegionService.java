package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.RegionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegionService {

    @Autowired
    RegionRepository regionRepository;

    public RegionRepository.RegionDTO create(String name) {
        try {
            return regionRepository.save(new RegionRepository.RegionDTO(null, name));
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Region name already exists");
        }
    }

    public RegionRepository.RegionDTO update(Integer id, String name) {
        regionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            return regionRepository.save(new RegionRepository.RegionDTO(id, name));
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Region name already exists");
        }
    }

    public void delete(Integer id) {
        regionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            regionRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Region is in use");
        }
    }
}
