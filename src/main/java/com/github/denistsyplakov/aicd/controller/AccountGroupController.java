package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.AccountGroupRepository;
import com.github.denistsyplakov.aicd.repo.AccountRepository;
import com.github.denistsyplakov.aicd.service.AccountGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AccountGroupController {

    @Autowired
    AccountGroupRepository accountGroupRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AccountGroupService accountGroupService;

    @PostMapping("/api/account-group")
    public AccountGroupRepository.AccountGroupDTO create(@RequestBody AccountGroupRepository.AccountGroupDTO group) {
        return accountGroupService.create(group);
    }

    @GetMapping("/api/account-group")
    public Iterable<AccountGroupRepository.AccountGroupDTO> getAll() {
        return accountGroupRepository.findAll();
    }

    @GetMapping("/api/account-group/{id}")
    public AccountGroupRepository.AccountGroupDTO getById(@PathVariable int id) {
        return accountGroupRepository.findById(id).orElseThrow();
    }

    @PutMapping("/api/account-group")
    public AccountGroupRepository.AccountGroupDTO update(@RequestBody AccountGroupRepository.AccountGroupDTO group) {
        return accountGroupService.update(group);
    }

    @DeleteMapping("/api/account-group/{id}")
    public void delete(@PathVariable int id) {
        accountGroupService.delete(id);
    }

    @GetMapping("/api/account-group/{id}/accounts")
    public List<AccountRepository.AccountDTO> getAccountsForGroup(@PathVariable int id) {
        return accountRepository.findByAccountGroupId(id);
    }
}
