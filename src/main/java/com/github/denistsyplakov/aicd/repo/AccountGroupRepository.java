package com.github.denistsyplakov.aicd.repo;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountGroupRepository extends CrudRepository<AccountGroupRepository.AccountGroupDTO, Integer> {
    @Table("account_group")
    record AccountGroupDTO(@Id Integer id, String name) {}
}
