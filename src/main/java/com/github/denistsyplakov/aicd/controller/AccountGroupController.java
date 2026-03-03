package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.AccountRepository;
import com.github.denistsyplakov.aicd.repo.AccountGroupRepository;
import com.github.denistsyplakov.aicd.service.AccountGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class AccountGroupController {

    @Autowired
    AccountGroupRepository accountGroupRepository;

    @Autowired
    AccountGroupService accountGroupService;

    @GetMapping("/api/account-group")
    public Iterable<AccountGroupRepository.AccountGroupDTO> getAll() {
        return accountGroupRepository.findAll();
    }

    @GetMapping("/api/account-group/{id}")
    public AccountGroupRepository.AccountGroupDTO getById(@PathVariable Integer id) {
        return accountGroupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/api/account-group")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountGroupRepository.AccountGroupDTO create(@RequestBody AccountGroupRepository.AccountGroupDTO dto) {
        return accountGroupService.create(dto.name());
    }

    @PutMapping("/api/account-group/{id}")
    public AccountGroupRepository.AccountGroupDTO update(@PathVariable Integer id,
                                                        @RequestBody AccountGroupRepository.AccountGroupDTO dto) {
        return accountGroupService.update(id, dto.name());
    }

    @DeleteMapping("/api/account-group/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        accountGroupService.delete(id);
    }

    @GetMapping("/api/account-group/{id}/accounts")
    public List<AccountRepository.AccountDTO> getAccounts(@PathVariable Integer id) {
        accountGroupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return accountGroupRepository.findAccountsByAccountGroupId(id);
    }
}
