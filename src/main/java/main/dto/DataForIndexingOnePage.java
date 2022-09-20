package main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import main.services.IndexingService;
import main.systems.lemmatize.Lemmatizer;

/**
 *  * DTO-class required for transmission parameters for indexing one page to the {@code LinkParser} class.
 */
@Data
@AllArgsConstructor
public class DataForIndexingOnePage {
    private String url;
    private IndexingService indexingService;
    private Lemmatizer lemmatizer;
    private boolean indexOnePage;
}
