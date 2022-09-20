package main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import main.model.Lemma;
import main.model.Page;
import main.systems.lemmatize.Lemmatizer;

import java.util.HashMap;
import java.util.List;

/**
 * DTO-class required for transmission parameters to the {@code ResultMapCalculation} class.
 */
@Data
@AllArgsConstructor
public class DataForResultMapCalculation {
    private HashMap<main.model.Page, float[]> pageMapForSearch;
    private List<Lemma> lemmaList;
    private HashMap<SearchResult, Float> resultMap;
    private Page page;
    private Lemmatizer lemmatizer;
}
