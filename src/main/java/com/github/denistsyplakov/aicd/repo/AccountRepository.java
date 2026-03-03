package com.github.denistsyplakov.aicd.repo;

import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends CrudRepository<AccountRepository.AccountDTO, Integer> {

    @Table("account")
    record AccountDTO(@Id Integer id, String name, Integer accountGroupId, Integer regionId) {}

    @Query("""
            select id, name, account_group_id, region_id
            from account
            where region_id = :regionId
            order by id
            """)
    List<AccountDTO> findAllByRegionId(Integer regionId);

    @Query("""
            select id, name, account_group_id, region_id
            from account
            where account_group_id = :accountGroupId
            order by id
            """)
    List<AccountDTO> findAllByAccountGroupId(Integer accountGroupId);

    @Override
    List<AccountDTO> findAll();
}
