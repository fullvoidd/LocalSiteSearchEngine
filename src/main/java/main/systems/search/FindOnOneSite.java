package main.systems.search;

import main.dto.DataForResultMapCalculation;
import main.dto.DataForSearchSystem;
import main.dto.SearchResult;
import main.model.Index;
import main.model.Lemma;
import main.model.Page;
import main.services.IndexingService;
import main.systems.lemmatize.Lemmatizer;

import java.util.*;

/**
 * This is a class used to find coincidences on one site.
 * It implements Runnable to be able to search on few sites at the same time.
 */
public class FindOnOneSite implements Runnable{

    private final String userRequest;
    private final IndexingService indexingService;
    private final HashMap<SearchResult, Float> resultMap;
    private final int siteId;
    private List<Lemma> lemmaList;
    private float maxRank;
    private final DataForSearchSystem data;
    private HashMap<Page, float[]> pageMapForSearch = new HashMap<>();
    private final Lemmatizer lemmatizer;

    /**
     * @param data DTO-object, that contains initial data for searching
     * @param siteId id of the specific site
     */
    public FindOnOneSite(DataForSearchSystem data, int siteId) {
        this.userRequest = data.getUserRequest();
        this.indexingService = data.getIndexingService();
        this.resultMap = data.getResultMap();
        this.siteId = siteId;
        this.maxRank = data.getMaxRank();
        this.data = data;
        this.lemmatizer = data.getLemmatizer();
    }

    @Override
    public void run() {
        try {
            List<Lemma> lemmaList = getSortedLemmaList(userRequest, indexingService);
            if (lemmaList == null) return;
            this.lemmaList = lemmaList;
            pageMapForSearch = getPageMapForSearch(indexingService);
            for (Page page : pageMapForSearch.keySet()) {
                DataForResultMapCalculation data = new DataForResultMapCalculation(
                        pageMapForSearch, lemmaList, resultMap, page, lemmatizer);
                new ResultMapCalculation(data).run();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This method lemmatize user request, sort it and convert {@code String} lemma to object {@link Lemma}.
     *
     * @param userRequest user request from search bar
     * @param indexingService main service for communicating with DB
     * @return sorted {@code List} of {@link Lemma}
     */
    private List<Lemma> getSortedLemmaList(String userRequest, IndexingService indexingService) {
        TreeMap<String, Integer> userLemmaMap = lemmatizer.getLemmasFromText(userRequest);

        List<Lemma> lemmaList = new ArrayList<>();
        for (String lemma : userLemmaMap.keySet()) {
            Lemma nextLemma = indexingService.getLemmaByNameAndSiteId(lemma, siteId);
            if (nextLemma != null) lemmaList.add(nextLemma);
        }
        if (userLemmaMap.size() != lemmaList.size()) return null;
        lemmaList.sort(Comparator.comparing(Lemma::getFrequency));

        return lemmaList;
    }

    /**
     * This method collect {@code Map} of page for further searching.
     * It calculates rank of specific page.
     *
     * @param indexingService main service for communicating with DB
     * @return {@code Map} with page as key and its rank as value
     */
    private HashMap<Page, float[]> getPageMapForSearch(IndexingService indexingService) {
        long pagesCount = indexingService.getPagesCount();
        HashMap<Lemma, List<Index>> lemmaAndIndexMap = indexingService.getLemmaAndIndexMap(lemmaList);
        for (Lemma lemma : lemmaAndIndexMap.keySet()) {
            List<Index> indexList = lemmaAndIndexMap.get(lemma);
            if (!((double) (indexList.size() / pagesCount) < 0.4)) continue;
            if (pageMapForSearch.isEmpty()) {
                indexList.forEach(index -> {
                    float[] rank = new float[1];
                    rank[0] = index.getRank();
                    pageMapForSearch.put(index.getPage(), rank);
                });
            } else {
                HashMap<Page, float[]> pageMapWithRank =
                        rankCalculation(lemmaAndIndexMap.get(lemma));
                pageMapForSearch.clear();
                pageMapForSearch.putAll(pageMapWithRank);
            }
        }
        calculateRelativeRank();

        return pageMapForSearch;
    }

    /**
     * This method request {@code List} of indexes of specific lemma and calculate rank for each page
     *
     * @param indexList {@code List} with indexes of specific lemma
     * @return copy of {@code Map} for search with ranks
     */
    private HashMap<Page, float[]> rankCalculation(List<Index> indexList) {
        HashMap<Page, float[]> copyPageMapForSearch = new HashMap<>();
        for (Index index : indexList) {
            for (Page page : pageMapForSearch.keySet()) {
                if (page.getId() != index.getPage().getId()) continue;
                int length = pageMapForSearch.get(page).length;
                copyPageMapForSearch.put(page, rankMassiveCalculation(index, page, length));
                break;
            }
        }
        return copyPageMapForSearch;
    }

    /**
     * This method calculate rank array adding new ranks of new lemma.
     *
     * @param index index of specific lemma
     * @param page page with this index
     * @param length rank array length
     * @return rank array with new rank
     */
    private float[] rankMassiveCalculation(Index index, Page page, int length) {
        float[] rankMassive;
        if ((length + 1) == lemmaList.size()) {
            rankMassive = new float[length + 2];
            for (int i = 0; i < length; i++) {
                rankMassive[i] = pageMapForSearch.get(page)[i];
            }
            rankMassive[rankMassive.length - 2] = index.getRank();
            float sum = (float) 0;
            for (int i = 0; i < rankMassive.length - 1; i++) {
                sum += rankMassive[i];
            }
            rankMassive[rankMassive.length - 1] = sum;
        } else {
            rankMassive = new float[length + 1];
            for (int i = 0; i < length; i++) {
                rankMassive[i] = pageMapForSearch.get(page)[i];
            }
            rankMassive[rankMassive.length - 1] = index.getRank();
        }
        return rankMassive;
    }

    /**
     * This method calculate relative rank and put it on the last place of rank massive
     * for each page from {@code Map}.
     */
    private void calculateRelativeRank() {
        findMaxRank();
        for (Page page : pageMapForSearch.keySet()) {
            float[] rankMassive = new float[pageMapForSearch.get(page).length + 1];
            for (int i = 0; i < rankMassive.length; i++) {
                if (i != rankMassive.length - 1) rankMassive[i] = pageMapForSearch.get(page)[i];
                else rankMassive[i] = rankMassive[i - 1] / maxRank;
            }
            pageMapForSearch.put(page, rankMassive);
        }
    }

    /**
     * This method finds max rank in main {@code Map} for search.
     *
     * @return max rank
     */
    private float findMaxRank() {
        for (Page page : pageMapForSearch.keySet()) {
            for (int i = 0; i < pageMapForSearch.get(page).length; i++) {
                if (pageMapForSearch.get(page)[i] < maxRank) continue;
                maxRank = pageMapForSearch.get(page)[i];
                data.setMaxRank(maxRank);
            }
        }
        return maxRank;
    }

}
