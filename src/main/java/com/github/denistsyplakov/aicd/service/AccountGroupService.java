package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.AccountGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountGroupService {

    @Autowired
    AccountGroupRepository accountGroupRepository;

    public AccountGroupRepository.AccountGroupDTO create(String name) {
        try {
            return accountGroupRepository.save(new AccountGroupRepository.AccountGroupDTO(null, name));
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account group name already exists");
        }
    }

    public AccountGroupRepository.AccountGroupDTO update(Integer id, String name) {
        accountGroupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            return accountGroupRepository.save(new AccountGroupRepository.AccountGroupDTO(id, name));
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account group name already exists");
        }
    }

    public void delete(Integer id) {
        accountGroupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            accountGroupRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account group is in use");
        }
    }
}
