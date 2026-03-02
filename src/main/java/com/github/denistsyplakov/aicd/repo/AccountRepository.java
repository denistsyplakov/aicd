package com.github.denistsyplakov.aicd.repo;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends CrudRepository<AccountRepository.AccountDTO, Integer> {

    @Table("account")
    record AccountDTO(
            @Id Integer id,
            String name,
            @Column("account_group_id") Integer accountGroupId,
            @Column("region_id") Integer regionId
    ) {}

    Optional<AccountDTO> findByName(String name);
    Iterable<AccountDTO> findAllByRegionId(Integer regionId);
    Iterable<AccountDTO> findAllByAccountGroupId(Integer accountGroupId);

}
