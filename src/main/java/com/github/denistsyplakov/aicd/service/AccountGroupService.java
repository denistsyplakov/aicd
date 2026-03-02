package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.AccountGroupRepository;
import com.github.denistsyplakov.aicd.repo.AccountGroupRepository.AccountGroupDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountGroupService {

    @Autowired
    private AccountGroupRepository accountGroupRepository;

    public AccountGroupDTO create(AccountGroupDTO accountGroupDTO) {
        try {
            return accountGroupRepository.save(new AccountGroupDTO(null, accountGroupDTO.name()));
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("idx_account_group_name_unique")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Account group name must be unique", e);
            }
            throw e;
        }
    }

    public AccountGroupDTO update(Integer id, AccountGroupDTO accountGroupDTO) {
        if (!accountGroupRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account group not found");
        }
        try {
            return accountGroupRepository.save(new AccountGroupDTO(id, accountGroupDTO.name()));
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("idx_account_group_name_unique")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Account group name must be unique", e);
            }
            throw e;
        }
    }

    public void delete(Integer id) {
        if (!accountGroupRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account group not found");
        }
        try {
            accountGroupRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account group is in use and cannot be deleted", e);
        }
    }
}
