package com.github.denistsyplakov.aicd.repo;

import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SoWRepository extends CrudRepository<SoWRepository.SoWDTO, Integer> {

    @Table("sow")
    record SoWDTO(@Id Integer id,
                  Integer accountId,
                  LocalDate date,
                  String title,
                  BigDecimal amount,
                  String description,
                  String text) {}

    record SoWSearchDTO(Integer id,
                        Integer accountId,
                        LocalDate date,
                        String title,
                        BigDecimal amount,
                        String description,
                        String text,
                        Float rank) {}

    @Override
    List<SoWDTO> findAll();

    @Modifying
    @Query("""
            insert into sow_text_index(id, tsvector)
            values (:sowId, to_tsvector('simple', coalesce(:text, '')))
            on conflict (id) do update set tsvector = excluded.tsvector
            """)
    void upsertTextIndex(Integer sowId, String text);

    @Modifying
    @Query("delete from sow_text_index where id = :sowId")
    void deleteTextIndex(Integer sowId);

    @Query("""
            select s.id,
                   s.account_id,
                   s.date,
                   s.title,
                   s.amount,
                   s.description,
                   s.text,
                   ts_rank_cd(sti.tsvector, plainto_tsquery('simple', :queryText)) as rank
            from sow s
            join sow_text_index sti on sti.id = s.id
            where sti.tsvector @@ plainto_tsquery('simple', :queryText)
              and ts_rank_cd(sti.tsvector, plainto_tsquery('simple', :queryText)) >= :minRank
            order by rank desc, s.id
            limit :maxDoc
            """)
    List<SoWSearchDTO> search(String queryText, int maxDoc, float minRank);
}
