package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.AccountGroupRepository;
import com.github.denistsyplakov.aicd.repo.AccountRepository;
import com.github.denistsyplakov.aicd.service.AccountGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class AccountGroupController {

    private final AccountGroupRepository accountGroupRepository;
    private final AccountRepository accountRepository;
    private final AccountGroupService accountGroupService;

    public AccountGroupController(AccountGroupRepository accountGroupRepository,
                                  AccountRepository accountRepository,
                                  AccountGroupService accountGroupService) {
        this.accountGroupRepository = accountGroupRepository;
        this.accountRepository = accountRepository;
        this.accountGroupService = accountGroupService;
    }

    @PostMapping("/api/account-groups")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountGroupRepository.AccountGroupDTO create(@RequestBody AccountGroupRepository.AccountGroupDTO dto) {
        return accountGroupService.create(dto);
    }

    @GetMapping("/api/account-groups")
    public List<AccountGroupRepository.AccountGroupDTO> getAll() {
        return accountGroupRepository.findAll();
    }

    @GetMapping("/api/account-groups/{id}")
    public AccountGroupRepository.AccountGroupDTO getById(@PathVariable Integer id) {
        return accountGroupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account group not found"));
    }

    @PutMapping("/api/account-groups/{id}")
    public AccountGroupRepository.AccountGroupDTO update(@PathVariable Integer id,
                                                         @RequestBody AccountGroupRepository.AccountGroupDTO dto) {
        return accountGroupService.update(id, dto);
    }

    @DeleteMapping("/api/account-groups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        accountGroupService.delete(id);
    }

    @GetMapping("/api/account-groups/{id}/accounts")
    public List<AccountRepository.AccountDTO> getAccountsByGroup(@PathVariable Integer id) {
        if (!accountGroupRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account group not found");
        }
        return accountRepository.findAllByAccountGroupId(id);
    }
}
