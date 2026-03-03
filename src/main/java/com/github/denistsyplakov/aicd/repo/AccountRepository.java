package com.github.denistsyplakov.aicd.repo;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends CrudRepository<AccountRepository.AccountDTO, Integer> {

    @Table("account")
    record AccountDTO(@Id Integer id, String name, Integer accountGroupId, Integer regionId) {}
}
