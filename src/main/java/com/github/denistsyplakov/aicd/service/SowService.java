package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.SowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SowService {

    @Autowired
    SowRepository sowRepository;

    @Transactional
    public SowRepository.SowDTO create(SowRepository.SowDTO dto) {
        try {
            SowRepository.SowDTO saved = sowRepository.save(
                    new SowRepository.SowDTO(null, dto.accountId(), dto.date(),
                            dto.title(), dto.amount(), dto.description(), dto.text()));
            sowRepository.upsertTextIndex(saved.id(), saved.text() != null ? saved.text() : "");
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid account_id");
        }
    }

    @Transactional
    public SowRepository.SowDTO update(Integer id, SowRepository.SowDTO dto) {
        sowRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            SowRepository.SowDTO saved = sowRepository.save(
                    new SowRepository.SowDTO(id, dto.accountId(), dto.date(),
                            dto.title(), dto.amount(), dto.description(), dto.text()));
            sowRepository.upsertTextIndex(id, saved.text() != null ? saved.text() : "");
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid account_id");
        }
    }

    @Transactional
    public void delete(Integer id) {
        sowRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        sowRepository.deleteTextIndex(id);
        sowRepository.deleteById(id);
    }
}
