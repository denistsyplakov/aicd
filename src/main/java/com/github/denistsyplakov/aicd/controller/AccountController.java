package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.AccountRepository;
import com.github.denistsyplakov.aicd.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class AccountController {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AccountService accountService;

    @PostMapping("/api/account")
    public AccountRepository.AccountDTO create(@RequestBody AccountRepository.AccountDTO account) {
        return accountService.create(account);
    }

    @GetMapping("/api/account")
    public Iterable<AccountRepository.AccountDTO> getAll() {
        return accountRepository.findAll();
    }

    @GetMapping("/api/account/{id}")
    public AccountRepository.AccountDTO getById(@PathVariable int id) {
        return accountRepository.findById(id).orElseThrow();
    }

    @PutMapping("/api/account")
    public AccountRepository.AccountDTO update(@RequestBody AccountRepository.AccountDTO account) {
        return accountService.update(account);
    }

    @DeleteMapping("/api/account/{id}")
    public void delete(@PathVariable int id) {
        accountService.delete(id);
    }
}
