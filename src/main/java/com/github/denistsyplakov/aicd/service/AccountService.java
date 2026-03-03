package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    @Autowired
    AccountRepository accountRepository;

    public AccountRepository.AccountDTO create(AccountRepository.AccountDTO dto) {
        try {
            return accountRepository.save(new AccountRepository.AccountDTO(null, dto.name(), dto.accountGroupId(), dto.regionId()));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid account_group_id or region_id");
        }
    }

    public AccountRepository.AccountDTO update(Integer id, AccountRepository.AccountDTO dto) {
        accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            return accountRepository.save(new AccountRepository.AccountDTO(id, dto.name(), dto.accountGroupId(), dto.regionId()));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid account_group_id or region_id");
        }
    }

    public void delete(Integer id) {
        accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            accountRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is in use");
        }
    }
}
