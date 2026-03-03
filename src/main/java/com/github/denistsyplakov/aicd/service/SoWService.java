package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.AccountRepository;
import com.github.denistsyplakov.aicd.repo.SoWRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SoWService {

    @Autowired
    SoWRepository sowRepository;

    @Autowired
    AccountRepository accountRepository;

    private void validateReferences(SoWRepository.SoWDTO sow) {
        if (sow.accountId() != null && !accountRepository.existsById(sow.accountId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
    }

    @Transactional
    public SoWRepository.SoWDTO create(SoWRepository.SoWDTO sow) {
        validateReferences(sow);
        SoWRepository.SoWDTO saved = sowRepository.save(sow);
        sowRepository.updateTextIndex(saved.id(), saved.text());
        return saved;
    }

    @Transactional
    public SoWRepository.SoWDTO update(SoWRepository.SoWDTO sow) {
        if (!sowRepository.existsById(sow.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SoW not found");
        }
        validateReferences(sow);
        SoWRepository.SoWDTO saved = sowRepository.save(sow);
        sowRepository.updateTextIndex(saved.id(), saved.text());
        return saved;
    }

    @Transactional
    public void delete(int id) {
        sowRepository.deleteTextIndex(id);
        sowRepository.deleteById(id);
    }
}
