package main.repositories;

import main.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    @Query(value = "SELECT count(*) FROM search_engine.page WHERE site_id = :site_id", nativeQuery = true)
    long getCountOfCertainSite(@Param("site_id") long siteId);

    Page getPageByPathAndSiteId(String path, int siteId);

    @Query("SELECT lp FROM Page lp WHERE lp.id in ?1")
    List<Page> getPageListByIds(List<Integer> ids);
}
