package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.AccountGroupRepository;
import com.github.denistsyplakov.aicd.repo.AccountRepository;
import com.github.denistsyplakov.aicd.repo.RegionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    RegionRepository regionRepository;

    @Autowired
    AccountGroupRepository accountGroupRepository;

    private void validateReferences(AccountRepository.AccountDTO account) {
        if (account.regionId() != null && !regionRepository.existsById(account.regionId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found");
        }
        if (account.accountGroupId() != null && !accountGroupRepository.existsById(account.accountGroupId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account Group not found");
        }
    }

    public AccountRepository.AccountDTO create(AccountRepository.AccountDTO account) {
        validateReferences(account);
        return accountRepository.save(account);
    }

    public AccountRepository.AccountDTO update(AccountRepository.AccountDTO account) {
        if (!accountRepository.existsById(account.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
        validateReferences(account);
        return accountRepository.save(account);
    }

    public void delete(int id) {
        try {
            accountRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is in use", e);
        }
    }
}
