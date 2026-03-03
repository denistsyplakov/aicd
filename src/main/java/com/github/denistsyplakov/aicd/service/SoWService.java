package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.SoWRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SoWService {

    private final SoWRepository soWRepository;

    public SoWService(SoWRepository soWRepository) {
        this.soWRepository = soWRepository;
    }

    public SoWRepository.SoWDTO create(SoWRepository.SoWDTO dto) {
        try {
            SoWRepository.SoWDTO saved = soWRepository.save(new SoWRepository.SoWDTO(
                    null,
                    dto.accountId(),
                    dto.date(),
                    dto.title(),
                    dto.amount(),
                    dto.description(),
                    dto.text()
            ));
            soWRepository.upsertTextIndex(saved.id(), saved.text());
            return saved;
        } catch (DataIntegrityViolationException exception) {
            SqlStateErrorMapper.throwForDataIntegrity(exception, "Unable to create SoW");
            throw exception;
        }
    }

    public SoWRepository.SoWDTO update(Integer id, SoWRepository.SoWDTO dto) {
        soWRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SoW not found"));
        try {
            SoWRepository.SoWDTO saved = soWRepository.save(new SoWRepository.SoWDTO(
                    id,
                    dto.accountId(),
                    dto.date(),
                    dto.title(),
                    dto.amount(),
                    dto.description(),
                    dto.text()
            ));
            soWRepository.upsertTextIndex(saved.id(), saved.text());
            return saved;
        } catch (DataIntegrityViolationException exception) {
            SqlStateErrorMapper.throwForDataIntegrity(exception, "Unable to update SoW");
            throw exception;
        }
    }

    public void delete(Integer id) {
        if (!soWRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SoW not found");
        }
        try {
            soWRepository.deleteTextIndex(id);
            soWRepository.deleteById(id);
        } catch (DataIntegrityViolationException exception) {
            SqlStateErrorMapper.throwForDataIntegrity(exception, "Unable to delete SoW");
        }
    }
}
