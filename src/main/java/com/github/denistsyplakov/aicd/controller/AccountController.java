package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.AccountRepository;
import com.github.denistsyplakov.aicd.repo.AccountRepository.AccountDTO;
import com.github.denistsyplakov.aicd.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AccountController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @PostMapping("/api/account")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDTO create(@RequestBody AccountDTO accountDTO) {
        return accountService.create(accountDTO);
    }

    @GetMapping("/api/account")
    public Iterable<AccountDTO> getAll() {
        return accountRepository.findAll();
    }

    @GetMapping("/api/account/{id}")
    public AccountDTO getById(@PathVariable Integer id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    @PutMapping("/api/account/{id}")
    public AccountDTO update(@PathVariable Integer id, @RequestBody AccountDTO accountDTO) {
        return accountService.update(id, accountDTO);
    }

    @DeleteMapping("/api/account/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        accountService.delete(id);
    }
}
