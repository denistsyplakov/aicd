package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.SoWRepository;
import com.github.denistsyplakov.aicd.repo.SoWRepository.SoWDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SoWService {

    @Autowired
    private SoWRepository sowRepository;

    @Transactional
    public SoWDTO create(SoWDTO sowDTO) {
        try {
            SoWDTO saved = sowRepository.save(new SoWDTO(null, sowDTO.accountId(), sowDTO.date(),
                    sowDTO.title(), sowDTO.amount(), sowDTO.description(), sowDTO.text()));
            if (saved.text() != null) {
                sowRepository.createTextIndex(saved.id(), saved.text());
            } else {
                sowRepository.createTextIndex(saved.id(), "");
            }
            return saved;
        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e);
            throw e;
        }
    }

    @Transactional
    public SoWDTO update(Integer id, SoWDTO sowDTO) {
        if (!sowRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SoW not found");
        }
        try {
            SoWDTO updated = sowRepository.save(new SoWDTO(id, sowDTO.accountId(), sowDTO.date(),
                    sowDTO.title(), sowDTO.amount(), sowDTO.description(), sowDTO.text()));
            if (updated.text() != null) {
                sowRepository.updateTextIndex(id, updated.text());
            } else {
                sowRepository.updateTextIndex(id, "");
            }
            return updated;
        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e);
            throw e;
        }
    }

    @Transactional
    public void delete(Integer id) {
        if (!sowRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SoW not found");
        }
        try {
            sowRepository.deleteTextIndex(id);
            sowRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SoW cannot be deleted", e);
        }
    }

    private void handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String message = e.getMessage();
        if (message != null && message.contains("sow_account_id_fkey")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account does not exist", e);
        }
    }
}
