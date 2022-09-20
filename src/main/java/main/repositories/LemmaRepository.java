package main.repositories;

import main.model.Lemma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Query(value = "SELECT count(*) FROM search_engine.lemma WHERE site_id = :site_id", nativeQuery = true)
    long getCountOfCertainSite(@Param("site_id") int siteId);

    Lemma getLemmaByLemmaAndSiteId(String lemmaName, int siteId);

    @Transactional
    @Modifying
    @Query("UPDATE Lemma l SET l.frequency = l.frequency - 1 WHERE id IN ?1")
    void decreaseLemmaByIds(@Param("ids") List<Integer> lemmaIds);

    List<Lemma> getLemmaListBySiteId(int siteId);
}
