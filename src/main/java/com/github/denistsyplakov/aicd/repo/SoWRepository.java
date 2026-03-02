package com.github.denistsyplakov.aicd.repo;

import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Repository
public interface SoWRepository extends CrudRepository<SoWRepository.SoWDTO, Integer> {

    @Table("sow")
    record SoWDTO(
            @Id Integer id,
            @Column("account_id") Integer accountId,
            Date date,
            String title,
            BigDecimal amount,
            String description,
            String text
    ) {}

    @Modifying
    @Query("INSERT INTO sow_text_index (id, tsvector) VALUES (:id, to_tsvector('english', :text))")
    void createTextIndex(Integer id, String text);

    @Modifying
    @Query("UPDATE sow_text_index SET tsvector = to_tsvector('english', :text) WHERE id = :id")
    void updateTextIndex(Integer id, String text);

    @Modifying
    @Query("DELETE FROM sow_text_index WHERE id = :id")
    void deleteTextIndex(Integer id);

    @Query("""
            SELECT s.id, s.account_id, s.date, s.title, s.amount, s.description,
                   CASE WHEN length(s.text) > :maxTextLength THEN left(s.text, :maxTextLength) || '... ' ELSE s.text END as text
            FROM sow s
            JOIN sow_text_index sti ON s.id = sti.id
            WHERE sti.tsvector @@ plainto_tsquery('english', :searchQuery)
              AND ts_rank(sti.tsvector, plainto_tsquery('english', :searchQuery)) >= :minRank
            ORDER BY ts_rank(sti.tsvector, plainto_tsquery('english', :searchQuery)) DESC
            LIMIT :maxDoc
            """)
    List<SoWDTO> search(
            String searchQuery,
            int maxDoc,
            float minRank,
            int maxTextLength
    );
}
