package main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import main.services.IndexingService;
import main.systems.lemmatize.Lemmatizer;

import java.util.HashMap;

/**
 * DTO-class required for transmission parameters to the {@code SearchSystem} class.
 */
@Data
@AllArgsConstructor
public class DataForSearchSystem {
    private String userRequest;
    private IndexingService indexingService;
    private HashMap<SearchResult, Float> resultMap;
    private float maxRank;
    private Lemmatizer lemmatizer;
}
