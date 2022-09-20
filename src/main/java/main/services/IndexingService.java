package main.services;

import lombok.RequiredArgsConstructor;
import main.model.*;
import main.repositories.*;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * This service provides an opportunity to interact with each DB table.
 * It has methods for inserting records into the DB and requesting them from it,
 * and also various situational methods.
 */
@Service
@RequiredArgsConstructor
public class IndexingService {

    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final FieldRepository fieldRepository;
    private final IndexRepository indexRepository;

    public void saveSite(Site site) {
        siteRepository.save(site);
    }

    public void saveAllLemmas(Set<Lemma> lemmaSet) {
        lemmaRepository.saveAllAndFlush(lemmaSet);
    }

    public void saveAllIndexes(Set<Index> indexSet) {
        indexRepository.saveAllAndFlush(indexSet);
    }

    public void deletePageWhenReindexing(Page page) {
        List<Integer> lemmaIds = indexRepository.getLemmaIdsListByPageId(page.getId());
        lemmaRepository.decreaseLemmaByIds(lemmaIds);
        indexRepository.deleteIndexesByPageId(page.getId());
        pageRepository.delete(page);
    }

    public void savePage(Page page) {
        pageRepository.save(page);
    }

    public List<Field> getFieldList() {
        return fieldRepository.findAll();
    }

    public Lemma getLemmaByNameAndSiteId(String lemmaName, int siteId) {
        return lemmaRepository.getLemmaByLemmaAndSiteId(lemmaName, siteId);
    }

    public Page getPageByPathAndSiteId(String path, int siteId) {
        return pageRepository.getPageByPathAndSiteId(path, siteId);
    }

    public Site getSiteByUrl(String url) {
        return siteRepository.getSiteByUrl(url);
    }

    public List<Site> getAllSites() {
        return siteRepository.findAll();
    }

    public long getPagesCount() {
        return pageRepository.count();
    }

    /**
     * The method gets a {@code List} of {@link Lemma} by {@code siteId} and response {@code HashMap}
     * with {@code lemmaName} as key and {@link Lemma} as value.
     *
     * @param siteId id of a specific site
     * @return {@link HashMap} that contains {@link String} as key and {@link Lemma} as value.
     */
    public HashMap<String, Lemma> getLemmaMapBySiteId(int siteId) {
        HashMap<String, Lemma> mapForResponse = new HashMap<>();
        lemmaRepository.getLemmaListBySiteId(siteId).forEach(lemma -> mapForResponse.put(lemma.getLemma(), lemma));
        return mapForResponse;
    }


    /**
     * The method gets as parameters a {@code List} of {@link Lemma} and response {@code HashMap}
     * with {@link Lemma} as key and {@code List}<{@link Index}> as value.
     *
     * @param lemmaList lemmas whose indexes need to be found
     * @return {@code HashMap} with with {@link Lemma} as key and {@code List}<{@link Index}> as value
     */
    public HashMap<Lemma, List<Index>> getLemmaAndIndexMap(List<Lemma> lemmaList) {
        HashMap<Lemma, List<Index>> resultMap = new HashMap<>();
        List<Integer> ids = new ArrayList<>();
        lemmaList.forEach(lemma -> ids.add(lemma.getId()));
        List<Index> indexList = indexRepository.getIndexListByLemmaIds(ids);

        lemmaList.forEach(lemma -> {
            List<Index> indexListOfSpecificLemma = new ArrayList<>();
            indexList.forEach(index -> {
                if (index.getLemma().getId() == lemma.getId()) indexListOfSpecificLemma.add(index);
            });
            resultMap.put(lemma, indexListOfSpecificLemma);
        });

        return resultMap;
    }
}
