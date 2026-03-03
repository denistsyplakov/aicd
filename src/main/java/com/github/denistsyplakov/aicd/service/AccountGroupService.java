package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.AccountGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountGroupService {

    @Autowired
    AccountGroupRepository accountGroupRepository;

    public AccountGroupRepository.AccountGroupDTO create(AccountGroupRepository.AccountGroupDTO group) {
        return accountGroupRepository.save(group);
    }

    public AccountGroupRepository.AccountGroupDTO update(AccountGroupRepository.AccountGroupDTO group) {
        if (!accountGroupRepository.existsById(group.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account Group not found");
        }
        return accountGroupRepository.save(group);
    }

    public void delete(int id) {
        try {
            accountGroupRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account Group is in use", e);
        }
    }
}
