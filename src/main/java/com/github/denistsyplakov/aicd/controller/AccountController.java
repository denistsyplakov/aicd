package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.AccountRepository;
import com.github.denistsyplakov.aicd.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AccountController {

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AccountService accountService;

    @GetMapping("/api/account")
    public Iterable<AccountRepository.AccountDTO> getAll() {
        return accountRepository.findAll();
    }

    @GetMapping("/api/account/{id}")
    public AccountRepository.AccountDTO getById(@PathVariable Integer id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/api/account")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountRepository.AccountDTO create(@RequestBody AccountRepository.AccountDTO dto) {
        return accountService.create(dto);
    }

    @PutMapping("/api/account/{id}")
    public AccountRepository.AccountDTO update(@PathVariable Integer id,
                                               @RequestBody AccountRepository.AccountDTO dto) {
        return accountService.update(id, dto);
    }

    @DeleteMapping("/api/account/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        accountService.delete(id);
    }
}
