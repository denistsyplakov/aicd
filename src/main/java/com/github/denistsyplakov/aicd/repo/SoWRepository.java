package com.github.denistsyplakov.aicd.repo;

import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SoWRepository extends CrudRepository<SoWRepository.SoWDTO, Integer> {
    @Table("sow")
    record SoWDTO(@Id Integer id, @Column("account_id") Integer accountId, LocalDate date, String title, BigDecimal amount, String description, String text) {}

    @Modifying
    @Query("INSERT INTO sow_text_index (id, tsvector) VALUES (:id, to_tsvector('english', :text)) ON CONFLICT (id) DO UPDATE SET tsvector = to_tsvector('english', :text)")
    void updateTextIndex(int id, String text);

    @Modifying
    @Query("DELETE FROM sow_text_index WHERE id = :id")
    void deleteTextIndex(int id);

    @Query("SELECT s.*, ts_rank_cd(sti.tsvector, query) AS rank " +
            "FROM sow s " +
            "JOIN sow_text_index sti ON s.id = sti.id, " +
            "websearch_to_tsquery('english', :searchQuery) query " +
            "WHERE sti.tsvector @@ query AND ts_rank_cd(sti.tsvector, query) >= :minRank " +
            "ORDER BY rank DESC " +
            "LIMIT :maxDoc")
    List<SoWDTO> search(String searchQuery, int maxDoc, double minRank);
}
