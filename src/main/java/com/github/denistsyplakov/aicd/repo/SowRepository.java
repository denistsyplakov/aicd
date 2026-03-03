package com.github.denistsyplakov.aicd.repo;

import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SowRepository extends CrudRepository<SowRepository.SowDTO, Integer> {

    @Table("sow")
    record SowDTO(
            @Id Integer id,
            Integer accountId,
            LocalDate date,
            String title,
            BigDecimal amount,
            String description,
            String text
    ) {}

    record SearchResultDTO(
            Integer id,
            Integer accountId,
            LocalDate date,
            String title,
            BigDecimal amount,
            String description,
            String text,
            double rank
    ) {}

    @Modifying
    @Query("""
            INSERT INTO sow_text_index (id, tsvector)
            VALUES (:id, to_tsvector('simple', :text))
            ON CONFLICT (id) DO UPDATE SET tsvector = to_tsvector('simple', :text)
            """)
    void upsertTextIndex(Integer id, String text);

    @Modifying
    @Query("DELETE FROM sow_text_index WHERE id = :id")
    void deleteTextIndex(Integer id);

    @Query("""
            SELECT s.id, s.account_id, s.date, s.title, s.amount, s.description, s.text,
                   ts_rank(i.tsvector, plainto_tsquery('simple', :query)) AS rank
            FROM sow s JOIN sow_text_index i ON s.id = i.id
            WHERE i.tsvector @@ plainto_tsquery('simple', :query)
              AND ts_rank(i.tsvector, plainto_tsquery('simple', :query)) >= :minRank
            ORDER BY rank DESC
            LIMIT :maxDoc
            """)
    List<SearchResultDTO> search(String query, double minRank, int maxDoc);
}
