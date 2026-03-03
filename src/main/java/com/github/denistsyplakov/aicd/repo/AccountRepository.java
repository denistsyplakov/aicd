package com.github.denistsyplakov.aicd.repo;

import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AccountRepository extends CrudRepository<AccountRepository.AccountDTO, Integer> {
    @Table("account")
    record AccountDTO(@Id Integer id, String name, @Column("account_group_id") Integer accountGroupId, @Column("region_id") Integer regionId) {}

    @Query("SELECT * FROM account WHERE region_id = :regionId")
    List<AccountDTO> findByRegionId(int regionId);

    @Query("SELECT * FROM account WHERE account_group_id = :groupId")
    List<AccountDTO> findByAccountGroupId(int groupId);
}
