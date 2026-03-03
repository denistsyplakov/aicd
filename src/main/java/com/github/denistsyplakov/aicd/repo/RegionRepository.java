package com.github.denistsyplakov.aicd.repo;

import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionRepository extends CrudRepository<RegionRepository.RegionDTO, Integer> {

    @Table("region")
    record RegionDTO(@Id Integer id, String name) {}

    @Query("SELECT id, name, account_group_id, region_id FROM account WHERE region_id = :regionId")
    List<AccountRepository.AccountDTO> findAccountsByRegionId(Integer regionId);
}
