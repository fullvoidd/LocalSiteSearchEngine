package main.services;

import lombok.RequiredArgsConstructor;
import main.model.Site;
import main.repositories.LemmaRepository;
import main.repositories.PageRepository;
import main.repositories.SiteRepository;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The service that returns indexing statistics in the {@code JSONObject} format
 * for further transmission to the front.
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;

    /**
     * The method generates indexing statistics in the {@code JSONObject} format
     * using data from DB and boolean parameter.
     *
     * @param isIndexing flag that describe current indexing status
     * @return {@link JSONObject} contains keys result(boolean) and statistics(JSONObject)
     */
    public JSONObject getStatisticsResult(boolean isIndexing) {
        JSONObject result = new JSONObject();
        JSONObject statistics = new JSONObject();

        statistics.put("total", getTotal(isIndexing));
        statistics.put("detailed", getDetailed());

        result.put("result", true);
        result.put("statistics", statistics);

        return result;
    }

    private JSONArray getDetailed() {
        List<Site> siteList = siteRepository.findAll();

        JSONArray detailed = new JSONArray();

        for (Site site : siteList) {
            long[] list = getPageAndLemmaCount(site);
            JSONObject content = new JSONObject();
            content.put("url", site.getUrl());
            content.put("name", site.getName());
            content.put("status", site.getStatus());
            content.put("statusTime", site.getStatusTime());
            if (site.getLastError() != null) {
                content.put("error", site.getLastError());
            }
            content.put("pages", list[0]);
            content.put("lemmas", list[1]);
            detailed.add(content);
        }

        return detailed;
    }

    private JSONObject getTotal(boolean isIndexing) {
        JSONObject total = new JSONObject();

        total.put("sites", siteRepository.count());
        total.put("pages", pageRepository.count());
        total.put("lemmas", lemmaRepository.count());
        total.put("isIndexing", isIndexing);

        return total;
    }

    /**
     * This method request site, then calculate pages and lemmas count using data from DB.
     *
     * @param site specific site, which contains pages and lemmas
     * @return a {@code long} array of size 2 with lemma and page count on specific site
     */
    public long[] getPageAndLemmaCount(Site site) {
        long[] result = new long[2];

        result[0] = pageRepository.getCountOfCertainSite(site.getId());
        result[1] = lemmaRepository.getCountOfCertainSite(site.getId());

        return result;
    }
}
