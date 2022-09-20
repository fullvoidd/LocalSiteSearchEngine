package main.systems.search;

import main.dto.DataForSearchSystem;
import main.dto.SearchResult;
import main.services.IndexingService;
import main.systems.lemmatize.Lemmatizer;

import java.io.IOException;
import java.util.*;

/**
 * This is a main class of search system.
 * It starts a search on each site.
 * This class request user request and {@code String} site if searching is provided on specific site.
 */
public class SearchSystem {
    private final Lemmatizer lemmatizer = new Lemmatizer();

    public SearchSystem() throws IOException {
    }

    /**
     * This method starts find coincidences on each site or on a specific site
     *
     * @param userRequest user request from search bar
     * @param indexingService main service for communicating with DB
     * @param site specific site(eq. null if searching on all sites)
     * @return {@code Map} with results of searching
     */
    public HashMap<SearchResult, Float> find(String userRequest, IndexingService indexingService, String site) {
        HashMap<SearchResult, Float> resultMap = new HashMap<>();
        float maxRank = 0;
        DataForSearchSystem data = new DataForSearchSystem(
                userRequest, indexingService, resultMap, maxRank, lemmatizer);
        if (site == null) {
            indexingService.getAllSites().forEach(siteFromDB ->
                    new FindOnOneSite(data, siteFromDB.getId()).run());
        } else {
            int siteId = indexingService.getSiteByUrl(site).getId();
            new FindOnOneSite(data, siteId).run();
        }
        return resultMap;
    }
}
