package main.systems.indexing;

import main.model.Field;
import main.model.Index;
import main.model.Lemma;
import main.model.Page;
import main.services.IndexingService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * This class used to increase speed of indexing and saving lemmas.
 * It implements Runnable for possibility to indexing few pages at the same time.
 */
public class LemmaAndIndexPersisting implements Runnable{

    private final HashMap<Lemma, HashMap<Field, Integer>> lemmaMap;
    private final IndexingService indexingService;
    private final Page page;

    /**
     * @param lemmaMap {@code Map} that contains {@link Lemma} as key and frequency on each field as value
     * @param indexingService main service for communicating with DB
     * @param page specific page that will be processed
     */
    public LemmaAndIndexPersisting(HashMap<Lemma, HashMap<Field, Integer>> lemmaMap,
                                   IndexingService indexingService, Page page) {
        this.lemmaMap = lemmaMap;
        this.indexingService = indexingService;
        this.page = page;
    }

    @Override
    public void run() {
        HashMap<Lemma, Float> lemmaMapForSave = new HashMap<>();
        HashMap<String, HashMap<Lemma, Float>> lemmaMapWithRank = preparingMapsForCalcAndSaving(lemmaMapForSave);

        synchronized (LemmaAndIndexPersisting.class) {
            HashMap<String, Lemma> resultLemmaMap = indexingService.getLemmaMapBySiteId(page.getSite().getId());
            for (String lemmaStr : lemmaMapWithRank.keySet()) {
                if (!resultLemmaMap.containsKey(lemmaStr)) {
                    lemmaMapForSave.putAll(lemmaMapWithRank.get(lemmaStr));
                } else {
                    Lemma resultLemma = resultLemmaMap.get(lemmaStr);
                    resultLemma.setFrequency(resultLemma.getFrequency() + 1);
                    HashMap<Lemma, Float> newMap = new HashMap<>();

                    lemmaMapWithRank.get(lemmaStr).forEach((key, value) -> newMap.put(resultLemma, value));
                    lemmaMapForSave.putAll(newMap);
                }
            }
            indexingService.saveAllLemmas(lemmaMapForSave.keySet());
        }

        Set<Index> indexSetForSave = new HashSet<>();
        lemmaMapForSave.forEach((lemma, rank) -> indexSetForSave.add(new Index(page, lemma, rank)));

        indexingService.saveAllIndexes(indexSetForSave);
    }

    /**
     * This method preparing 2 {@code Maps} for further calculating and multiple saving.
     *
     * @param lemmaMapForSave {@code Map} that contains lemmas for save
     * @return {@code Map} with ranks of each lemma
     */
    private HashMap<String, HashMap<Lemma, Float>> preparingMapsForCalcAndSaving(
            HashMap<Lemma, Float> lemmaMapForSave) {
        lemmaMap.keySet().forEach(lemma -> {
            float rank = 0;
            for (Field field : lemmaMap.get(lemma).keySet()) {
                rank += field.getWeight() * lemmaMap.get(lemma).get(field);
            }
            lemmaMapForSave.put(lemma, rank);
        });

        HashMap<String, HashMap<Lemma, Float>> lemmaMapWithRank = new HashMap<>();

        lemmaMapForSave.keySet().forEach(lemma -> {
            HashMap<Lemma, Float> newMap = new HashMap<>();
            newMap.put(lemma, lemmaMapForSave.get(lemma));
            lemmaMapWithRank.put(lemma.getLemma(), newMap);
        });
        lemmaMapForSave.clear();

        return lemmaMapWithRank;
    }
}
