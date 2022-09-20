package main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import main.config.ApplicationProperties;
import main.repositories.SiteRepository;

import java.util.HashMap;
import java.util.List;

/**
 * DTO-class required for transmission search results to the {@code SearchService}
 * class for further processing and output.
 */
@Data
@AllArgsConstructor
public class SearchServiceData {
    private SiteRepository siteRepository;
    private HashMap<SearchResult, Float> searchResult;
    private List<ApplicationProperties.CfgSite> siteList;
    private int limit;
    private int offset;
}
