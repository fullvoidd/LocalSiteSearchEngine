package main.services;

import main.config.ApplicationProperties;
import main.dto.SearchResult;
import main.dto.SearchServiceData;
import main.repositories.SiteRepository;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The service accepts immutable parameters and search system result as input,
 * then generates a search response in the JSONObject format.
 */
@Service
public class SearchService {
    private SiteRepository siteRepository;
    private HashMap<SearchResult, Float> searchResultMap;
    private List<ApplicationProperties.CfgSite> siteList;
    private int limit;
    private int offset;
    private int count = 0;

    public SearchService() {
    }

    /**
     * @param searchServiceData DTO-object, that contains data for result creating.
     */
    public SearchService(SearchServiceData searchServiceData) {
        this.siteRepository = searchServiceData.getSiteRepository();
        this.searchResultMap = searchServiceData.getSearchResult();
        this.siteList = searchServiceData.getSiteList();
        this.limit = searchServiceData.getLimit();
        this.offset = searchServiceData.getOffset();
    }

    private JSONArray getData() {
        JSONArray data = new JSONArray();

        if (searchResultMap.isEmpty()) {
            return data;
        }
        List<Map.Entry<SearchResult, Float>> toSort = new ArrayList<>(searchResultMap.entrySet());
        toSort.sort(Map.Entry.<SearchResult, Float>comparingByValue().reversed());
        for (int i = offset; count < limit; i++) {
            count++;
            Map.Entry<SearchResult, Float> entry = toSort.get(i);
            SearchResult result = entry.getKey();
            String urlForSearch = siteRepository.findById(result.getSiteId()).get().getUrl();
            siteList.forEach(site -> {
                if (!urlForSearch.equals(site.getUrl())) {
                    return;
                }
                JSONObject content = new JSONObject();
                content.put("site", urlForSearch);
                content.put("siteName", site.getName());
                content.put("uri", result.getUri());
                content.put("title", result.getTitle());
                content.put("snippet", result.getSnippet());
                content.put("relevance", entry.getValue());
                data.add(content);
            });
            if (toSort.size() == count + offset) {
                break;
            }
        }

        return data;
    }

    /**
     * The method generates a resultant response in the JSONObject format,
     * which has all the necessary keys for transmission to the front.
     *
     * @return {@link JSONObject} that have keys: result(boolean), count(int), data(JSONObject).
     */
    public JSONObject getResult() {
        JSONObject result = new JSONObject();

        result.put("result", true);
        result.put("count", searchResultMap.size());
        result.put("data", getData());

        return result;
    }
}
