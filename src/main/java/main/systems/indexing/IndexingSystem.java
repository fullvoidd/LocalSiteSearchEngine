package main.systems.indexing;

import main.model.Field;
import main.model.Lemma;
import main.model.Page;
import main.services.IndexingService;
import main.systems.lemmatize.Lemmatizer;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;

import java.util.*;

/**
 * This is a main indexing class that request page and processing it.
 * {@code IndexingSystem} class implements {@link Runnable} to increase the speed.
 */
public class IndexingSystem implements Runnable {
    private final Page page;
    private final IndexingService indexingService;
    private final Lemmatizer lemmatizer;

    /**
     * @param page specific {@link Page} which will be indexed
     * @param indexingService main service for communicating with DB
     * @param lemmatizer {@link Lemmatizer} object for finding lemmas
     */
    public IndexingSystem(Page page, IndexingService indexingService, Lemmatizer lemmatizer) {
        this.page = page;
        this.indexingService = indexingService;
        this.lemmatizer = lemmatizer;
    }

    @Override
    public void run() {
        String codeType = Integer.toString(page.getCode()).substring(0, 1);
        if (!codeType.equals("4") && !codeType.equals("5")) {
            HashMap<String, HashMap<Field, Integer>> wordBaseFormsMap =
                    getWordBaseFormsMap(indexingService.getFieldList(), page.getContent());
            HashMap<Lemma, HashMap<Field, Integer>> lemmaMap = new HashMap<>();
            for (String lemma : wordBaseFormsMap.keySet()) {
                Lemma nextLemma = new Lemma(lemma, page.getSite(), 1);
                lemmaMap.put(nextLemma, wordBaseFormsMap.get(lemma));
            }

            new LemmaAndIndexPersisting(lemmaMap, indexingService, page).run();
        }
    }

    /**
     * This method process page content and return {@code Map} with lemmas in {@code String} format
     * and their frequency on each selector field.
     *
     * @param fieldList {@code List}<{@link Field}> with fields for selector
     * @param content page content
     * @return {@code HashMap} that contain {@code String} lemma as key
     *          and {@code Map} with frequency on each field as value
     */
    private HashMap<String, HashMap<Field, Integer>> getWordBaseFormsMap(List<Field> fieldList, String content) {
        HashMap<String, HashMap<Field, Integer>> wordBaseFormsMap;
        wordBaseFormsMap = new HashMap<>();
        for (Field field : fieldList) {
            TreeMap<String, Integer> wordOnFieldMap = lemmatizer.getLemmasFromText(
                    Jsoup.parse(content, "", Parser.htmlParser()).getAllElements()
                            .select(field.getSelector()).text());
            for (String word : wordOnFieldMap.keySet()) {
                HashMap<Field, Integer> mapForField = new HashMap<>();
                if (wordBaseFormsMap.containsKey(word)) {
                    mapForField = wordBaseFormsMap.get(word);
                }
                mapForField.put(field, wordOnFieldMap.get(word));
                wordBaseFormsMap.put(word, mapForField);
            }
        }
        return wordBaseFormsMap;
    }

}
