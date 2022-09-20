package main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO-class required for containing search result by specific link.
 */
@Data
@AllArgsConstructor
public class SearchResult {
    private String uri;
    private String title;
    private String snippet;
    private String relevance;
    private int siteId;
}
