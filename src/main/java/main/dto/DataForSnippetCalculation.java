package main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;

/**
 * DTO-class required for transmission parameters to the {@code SnippetCalculation} class.
 */
@Data
@AllArgsConstructor
public class DataForSnippetCalculation {
    private Set<String> snippetSet;
    private Matcher matcher;
    private String stringDoc;
    private TreeMap<String, List<String>> mapForCheck;
}
