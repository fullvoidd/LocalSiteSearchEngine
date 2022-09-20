package main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import main.model.Site;
import main.services.IndexingService;
import main.systems.lemmatize.Lemmatizer;
import org.hibernate.Session;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DTO-class required for secondary transmission initial parameters to the {@code LinkParser} class.
 */
@Data
@AllArgsConstructor
public class DataForSecondaryParsing {
    private String url;
    private String mySite;
    private HashSet<String> uniqueUrls;
    private Site site;
    private IndexingService indexingService;
    private AtomicBoolean isIndexing;
    private Session session;
    private Lemmatizer lemmatizer;
}
