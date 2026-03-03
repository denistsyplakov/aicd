package com.github.denistsyplakov.aicd.repo;

import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionRepository extends CrudRepository<RegionRepository.RegionDTO, Integer> {

    @Table("region")
    record RegionDTO(@Id Integer id, String name) {}

    @Query("""
            select case when count(*) > 0 then true else false end
            from region
            where name = :name
            """)
    boolean existsByName(String name);

    @Query("""
            select case when count(*) > 0 then true else false end
            from region
            where name = :name and id <> :id
            """)
    boolean existsByNameAndIdNot(String name, Integer id);

    @Override
    List<RegionDTO> findAll();
}
