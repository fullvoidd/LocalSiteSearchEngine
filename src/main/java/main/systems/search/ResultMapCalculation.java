package main.systems.search;

import main.dto.DataForResultMapCalculation;
import main.dto.DataForSnippetCalculation;
import main.dto.SearchResult;
import main.model.Lemma;
import main.model.Page;
import main.systems.lemmatize.Lemmatizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a class used to collect calculation results in {@code Map}.
 * It implements @{link Runnable} for multiple calculation that increase speed of search system.
 */
public class ResultMapCalculation implements Runnable{

    private final HashMap<Page, float[]> pageMapForSearch;
    private final HashMap<SearchResult, Float> resultMap;
    private final Page page;
    private final List<Lemma> lemmaList;
    private final Lemmatizer lemmatizer;

    /**
     * @param data DTO-object, contains initial data for result calculation
     */
    public ResultMapCalculation(DataForResultMapCalculation data) {
        this.pageMapForSearch = data.getPageMapForSearch();
        this.resultMap = data.getResultMap();
        this.page = data.getPage();
        this.lemmaList = data.getLemmaList();
        this.lemmatizer = data.getLemmatizer();
    }

    @Override
    public void run() {
        String title = getTitle();
        String snippet = "";
        StringBuilder relevance = new StringBuilder();

        snippet = getSnippet(page.getContent(), snippet);
        if (snippet.isEmpty()) return;

        for (int i = 0; i < pageMapForSearch.get(page).length; i++) {
            relevance.append(pageMapForSearch.get(page)[i]).append(" ");
        }

        String uri = page.getPath();
        String relevanceForResult = relevance.toString().trim();
        SearchResult result = new SearchResult(uri, title, snippet, relevanceForResult, page.getSite().getId());

        Float relativeRelevance = pageMapForSearch.get(page)[pageMapForSearch.get(page).length - 1];
        resultMap.put(result, relativeRelevance);
    }


    /**
     * This method processes the page and finds the title from the content.
     *
     * @return {@code String} with title
     */
    private String getTitle() {
        String title = "";

        String content = page.getContent();
        boolean isTitleInSpan = false;
        String strForParsing = "";
        if (content.contains("<title>")) {
            strForParsing = content.substring(content.indexOf("<title>"),
                    content.indexOf("</title>") + 8);
        } else if (content.contains("title")) {
            Pattern pattern = Pattern.compile("<span.*</span>");
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String strForCheck = content.substring(matcher.start(), matcher.end());
                if (!strForCheck.contains("title=\"")) continue;
                strForParsing = strForCheck;
                isTitleInSpan = true;
                break;
            }
        }
        String queryForTitleSelect = !isTitleInSpan ? "title" : "span[title]";
        Document doc = Jsoup.parse(strForParsing, "", Parser.htmlParser());

        for (Element element : doc.select(queryForTitleSelect)) {
            title = !isTitleInSpan ? element.text() : element.attributes().get("title");
            break;
        }

        return title;
    }

    /**
     * This method start processes of snippet calculation.
     * It highlights in bold lemmas and his form from user request in page content
     * and collect them to the {@code List} of the snippets.
     *
     * @param content {@link String} content from page
     * @param snippet empty snippet for filling it in
     * @return result snippet with all coincidences
     */
    private String getSnippet(String content, String snippet) {
        TreeMap<String, List<String>> mapForCheck = getMapForCheck(lemmaList,
                lemmatizer.getLemmaSetFromText(content));

        for (String lem : mapForCheck.keySet()) {
            List<String> list = mapForCheck.get(lem);
            list.sort(Comparator.comparingInt(String::length).reversed());
            for (String word : list) content = content.replaceAll(word, "<b>" + word + "</b>");
            mapForCheck.put(lem, list);
        }

        Set<String> snippetList = getSnippetList(content, mapForCheck);

        if (!snippetList.isEmpty()) {
            StringBuilder snippetBuilder = new StringBuilder(snippet);
            for (String snip : snippetList) {
                snippetBuilder.append(snip).append("<br>");
            }
            snippet = snippetBuilder.toString();
        }

        return snippet;
    }

    /**
     * This method request {@code List} of lemmas from user request and
     * {@code Set} of {@code List} of lemmas and his forms from text.
     * It collects all of this in {@code Map} with lemma from request as key, and it forms as value.
     *
     * @param lemmaListFromRequest {@code List} of lemmas from request
     * @param lemmaListFromText {@code List} of lemmas from page text
     * @return {@code Map} with lemma from request as key, and it forms as value
     */
    private TreeMap<String, List<String>> getMapForCheck(List<Lemma> lemmaListFromRequest,
                                                         HashSet<List<String>> lemmaListFromText) {
        TreeMap<String, List<String>> mapForCheck = new TreeMap<>();
        for (List<String> list : lemmaListFromText) {
            for (Lemma lemma : lemmaListFromRequest) {
                if (!list.contains(lemma.getLemma())) continue;
                for (String word : list) {
                    if (!mapForCheck.containsKey(lemma.getLemma())) mapForCheck.put(lemma.getLemma(), list);
                    else if (!mapForCheck.get(lemma.getLemma()).contains(word)) {
                        List<String> l = mapForCheck.get(lemma.getLemma());
                        l.add(word);
                        mapForCheck.put(lemma.getLemma(), l);
                    }
                }
                break;
            }
        }
        return mapForCheck;
    }

    /**
     * This method calculates snippets and collects them into a {@code List}.
     *
     * @param stringDoc {@link Document} from page in {@code String} format
     * @param mapForCheck {@code Map} with lemmas and its forms for searching
     * @return {@code List} of snippets with each coincidence
     */
    private Set<String> getSnippetList(String stringDoc, TreeMap<String, List<String>> mapForCheck) {
        Set<String> snippetSet = new TreeSet<>();
        for (String lemma : mapForCheck.keySet()) {
            for (String word : mapForCheck.get(lemma)) {
                Pattern pattern = Pattern.compile(word);
                Matcher matcher = pattern.matcher(stringDoc);
                DataForSnippetCalculation data =
                        new DataForSnippetCalculation(snippetSet, matcher, stringDoc, mapForCheck);
                while (matcher.find()) new SnippetCalculation(data).run();
            }
        }
        return snippetSet;
    }
}
