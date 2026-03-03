package com.github.denistsyplakov.aicd.service;

import com.github.denistsyplakov.aicd.repo.AccountGroupRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountGroupService {

    private final AccountGroupRepository accountGroupRepository;

    public AccountGroupService(AccountGroupRepository accountGroupRepository) {
        this.accountGroupRepository = accountGroupRepository;
    }

    public AccountGroupRepository.AccountGroupDTO create(AccountGroupRepository.AccountGroupDTO dto) {
        try {
            return accountGroupRepository.save(new AccountGroupRepository.AccountGroupDTO(null, dto.name()));
        } catch (DataIntegrityViolationException exception) {
            SqlStateErrorMapper.throwForDataIntegrity(exception, "Unable to create account group");
            throw exception;
        }
    }

    public AccountGroupRepository.AccountGroupDTO update(Integer id, AccountGroupRepository.AccountGroupDTO dto) {
        accountGroupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account group not found"));
        try {
            return accountGroupRepository.save(new AccountGroupRepository.AccountGroupDTO(id, dto.name()));
        } catch (DataIntegrityViolationException exception) {
            SqlStateErrorMapper.throwForDataIntegrity(exception, "Unable to update account group");
            throw exception;
        }
    }

    public void delete(Integer id) {
        if (!accountGroupRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account group not found");
        }
        try {
            accountGroupRepository.deleteById(id);
        } catch (DataIntegrityViolationException exception) {
            SqlStateErrorMapper.throwForDataIntegrity(exception, "Account group is in use and cannot be deleted");
        }
    }
}
