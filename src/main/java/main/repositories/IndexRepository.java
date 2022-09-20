package main.repositories;

import main.model.Index;
import main.model.Lemma;
import main.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {

    @Query("SELECT il FROM Index il WHERE il.lemma.id IN ?1")
    List<Index> getIndexListByLemmaIds(List<Integer> ids);

    @Query("select i.lemma.id from Index i where i.page.id = ?1")
    List<Integer> getLemmaIdsListByPageId(Integer pageId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM search_engine.index WHERE page_id IN (SELECT :page_id FROM search_engine.page)",
            nativeQuery = true)
    void deleteIndexesByPageId(@Param("page_id") Integer pageId);
}
