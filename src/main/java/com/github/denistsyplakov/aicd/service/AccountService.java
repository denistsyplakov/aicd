package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.AccountRepository;
import com.github.denistsyplakov.aicd.repo.AccountRepository.AccountDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public AccountDTO create(AccountDTO accountDTO) {
        try {
            return accountRepository.save(new AccountDTO(null, accountDTO.name(), accountDTO.accountGroupId(), accountDTO.regionId()));
        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e);
            throw e;
        }
    }

    public AccountDTO update(Integer id, AccountDTO accountDTO) {
        if (!accountRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
        try {
            return accountRepository.save(new AccountDTO(id, accountDTO.name(), accountDTO.accountGroupId(), accountDTO.regionId()));
        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e);
            throw e;
        }
    }

    public void delete(Integer id) {
        if (!accountRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
        try {
            accountRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is in use and cannot be deleted", e);
        }
    }

    private void handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("idx_account_name_unique")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Account name must be unique", e);
            }
            if (message.contains("account_account_group_id_fkey")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account group does not exist", e);
            }
            if (message.contains("account_region_id_fkey")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Region does not exist", e);
            }
        }
    }
}
