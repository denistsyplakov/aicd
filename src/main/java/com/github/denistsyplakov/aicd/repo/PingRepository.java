package com.github.denistsyplakov.aicd.repo;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PingRepository extends CrudRepository<PingRepository.PingDTO, String> {

    record PingDTO(int reply){}

    @Query("SELECT 1+1 as reply")
    PingDTO ping();
}
