package main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import main.model.Site;
import main.services.IndexingService;
import main.systems.lemmatize.Lemmatizer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DTO-class required for primary transmission parameters to the {@code LinkParser} class.
 */
@Data
@AllArgsConstructor
public class DataForPrimaryParsing {
    private Site site;
    private AtomicBoolean isIndexing;
    private IndexingService indexingService;
    private int number;
    private Lemmatizer lemmatizer;
}
