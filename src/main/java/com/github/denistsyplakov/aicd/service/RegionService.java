package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.RegionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegionService {

    private final RegionRepository regionRepository;

    public RegionService(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    public RegionRepository.RegionDTO create(RegionRepository.RegionDTO dto) {
        if (regionRepository.existsByName(dto.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Region with this name already exists");
        }
        try {
            return regionRepository.save(new RegionRepository.RegionDTO(null, dto.name()));
        } catch (DataIntegrityViolationException exception) {
            SqlStateErrorMapper.throwForDataIntegrity(exception, "Unable to create region");
            throw exception;
        }
    }

    public RegionRepository.RegionDTO update(Integer id, RegionRepository.RegionDTO dto) {
        regionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found"));
        if (regionRepository.existsByNameAndIdNot(dto.name(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Region with this name already exists");
        }
        try {
            return regionRepository.save(new RegionRepository.RegionDTO(id, dto.name()));
        } catch (DataIntegrityViolationException exception) {
            SqlStateErrorMapper.throwForDataIntegrity(exception, "Unable to update region");
            throw exception;
        }
    }

    public void delete(Integer id) {
        if (!regionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found");
        }
        try {
            regionRepository.deleteById(id);
        } catch (DataIntegrityViolationException exception) {
            SqlStateErrorMapper.throwForDataIntegrity(exception, "Region is in use and cannot be deleted");
        }
    }
}
