package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.AccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountRepository.AccountDTO create(AccountRepository.AccountDTO dto) {
        try {
            return accountRepository.save(new AccountRepository.AccountDTO(null, dto.name(), dto.accountGroupId(), dto.regionId()));
        } catch (DataIntegrityViolationException exception) {
            SqlStateErrorMapper.throwForDataIntegrity(exception, "Unable to create account");
            throw exception;
        }
    }

    public AccountRepository.AccountDTO update(Integer id, AccountRepository.AccountDTO dto) {
        accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        try {
            return accountRepository.save(new AccountRepository.AccountDTO(id, dto.name(), dto.accountGroupId(), dto.regionId()));
        } catch (DataIntegrityViolationException exception) {
            SqlStateErrorMapper.throwForDataIntegrity(exception, "Unable to update account");
            throw exception;
        }
    }

    public void delete(Integer id) {
        if (!accountRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
        try {
            accountRepository.deleteById(id);
        } catch (DataIntegrityViolationException exception) {
            SqlStateErrorMapper.throwForDataIntegrity(exception, "Account is in use and cannot be deleted");
        }
    }
}
