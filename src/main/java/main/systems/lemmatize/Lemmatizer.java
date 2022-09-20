package main.systems.lemmatize;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a one of main classes for parsing and indexing.
 * It contains methods for text lemmatizing.
 */
public class Lemmatizer {
    private final LuceneMorphology luceneMorph;

    public Lemmatizer() throws IOException {
        luceneMorph = new RussianLuceneMorphology();
    }

    /**
     * This method lemmatize text and get lemmas of russian words and their count.
     *
     * @param text text on page
     * @return {@code Map} that contains {@code String} lemma as key and count of this lemma as value
     */
    public TreeMap<String, Integer> getLemmasFromText(String text) {
        TreeMap<String, Integer> wordBaseFormsMap = new TreeMap<>();

        String[] wordList = text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
        for(String word : wordList) {
            if (word.isBlank()) continue;
            if (hasParticles(luceneMorph, word)) continue;

            List<String> wordBF = luceneMorph.getNormalForms(word).stream().toList();
            if (wordBF.isEmpty()) continue;

            wordBF.forEach(normalWord -> {
                if (wordBaseFormsMap.containsKey(normalWord)) {
                    int count = wordBaseFormsMap.get(normalWord);
                    wordBaseFormsMap.replace(normalWord, count + 1);
                } else wordBaseFormsMap.put(normalWord, 1);
            });
        }
        return wordBaseFormsMap;
    }

    /**
     * This method checks the word for presence of particles.
     *
     * @param luceneMorph Object of {@link LuceneMorphology} for finding morph info
     * @param word specific word
     * @return {@code true} or {@code false} if word has particles
     */
    private boolean hasParticles(LuceneMorphology luceneMorph, String word) {
        List<String> wordBaseFormsInfo = luceneMorph.getMorphInfo(word);
        for (String info : wordBaseFormsInfo) {
            String specialSymbol = info.substring(info.indexOf("|") + 1, info.indexOf("|") + 2);
            if (specialSymbol.equals("o") || specialSymbol.equals("n") ||
                    specialSymbol.equals("l") || specialSymbol.equals("p")) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method lemmatize text and get lemmas of russian words and their count.
     *
     * @param text text on page
     * @return {@code Set} of {@code List} of {@code String} which contains lemma and word initial form
     */
    public HashSet<List<String>> getLemmaSetFromText(String text) {
        HashSet<List<String>> resultSet = new HashSet<>();

        Pattern pattern = Pattern.compile("[а-яА-Я]+");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String word = text.substring(matcher.start(), matcher.end());
            String expression = word.toLowerCase();
            if (expression.isEmpty()) continue;
            List<String> wordInfo = luceneMorph.getMorphInfo(expression);
            for (String info : wordInfo) {
                String specialSymbol = info.substring(info.indexOf("|") + 1, info.indexOf("|") + 2);
                if (specialSymbol.equals("o") || specialSymbol.equals("n") ||
                        specialSymbol.equals("l") || specialSymbol.equals("p")) continue;
                List<String> wordBF = luceneMorph.getNormalForms(expression).stream().toList();
                resultSet.add(addWordList(wordBF, info, word));
            }
        }
        return resultSet;
    }

    /**
     * This method adds word to {@code List} of words.
     *
     * @param wordBF {@code List} of base forms of words
     * @param info morph info about specific word
     * @param word specific word
     * @return this {@code List}
     */
    private List<String> addWordList(List<String> wordBF, String info, String word) {
        List<String> list = new ArrayList<>();
        wordBF.forEach(form -> {
            if (info.contains(form) && !list.contains(form)) {
                if (!word.equals(form)) list.add(word);
                list.add(form);
            }
        });
        return list;
    }
}
