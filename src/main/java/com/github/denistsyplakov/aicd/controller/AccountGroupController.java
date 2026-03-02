package com.github.denistsyplakov.aicd.controller;

import com.github.denistsyplakov.aicd.repo.AccountGroupRepository;
import com.github.denistsyplakov.aicd.repo.AccountGroupRepository.AccountGroupDTO;
import com.github.denistsyplakov.aicd.repo.AccountRepository;
import com.github.denistsyplakov.aicd.repo.AccountRepository.AccountDTO;
import com.github.denistsyplakov.aicd.service.AccountGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/account-group")
public class AccountGroupController {

    @Autowired
    private AccountGroupRepository accountGroupRepository;

    @Autowired
    private AccountGroupService accountGroupService;

    @Autowired
    private AccountRepository accountRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountGroupDTO create(@RequestBody AccountGroupDTO accountGroupDTO) {
        return accountGroupService.create(accountGroupDTO);
    }

    @GetMapping
    public Iterable<AccountGroupDTO> getAll() {
        return accountGroupRepository.findAll();
    }

    @GetMapping("/{id}")
    public AccountGroupDTO getById(@PathVariable Integer id) {
        return accountGroupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account group not found"));
    }

    @PutMapping("/{id}")
    public AccountGroupDTO update(@PathVariable Integer id, @RequestBody AccountGroupDTO accountGroupDTO) {
        return accountGroupService.update(id, accountGroupDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        accountGroupService.delete(id);
    }

    @GetMapping("/{id}/account")
    public Iterable<AccountDTO> getAccounts(@PathVariable Integer id) {
        if (!accountGroupRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account group not found");
        }
        return accountRepository.findAllByAccountGroupId(id);
    }
}
