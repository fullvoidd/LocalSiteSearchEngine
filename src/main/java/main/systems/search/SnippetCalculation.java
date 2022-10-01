package main.systems.search;

import main.dto.DataForSnippetCalculation;

import java.util.*;
import java.util.regex.Matcher;

/**
 * This is a class used to calculation each snippet.
 * It implements {@link Runnable} for multiple calculation of few snippets at the same time.
 */
public class SnippetCalculation implements Runnable{

    private final Set<String> snippetSet;
    private final Matcher matcher;
    private final String stringDoc;
    private final TreeMap<String, List<String>> mapForCheck;

    /**
     * @param data DTO-object, contains initial data for snippet calculation
     */
    public SnippetCalculation(DataForSnippetCalculation data) {
        this.snippetSet = data.getSnippetSet();
        this.matcher = data.getMatcher();
        this.stringDoc = data.getStringDoc();
        this.mapForCheck = data.getMapForCheck();
    }

    @Override
    public void run() {
        int[] fragmentsIds = getFragmentStartAndEnd(matcher.start(), matcher.end());
        String fragment = stringDoc.substring(fragmentsIds[0], fragmentsIds[1]);
        String rawFragment = fragment.replaceAll("[^а-яА-Я ]", "");
        List<String> rawFragmentList = Arrays.stream(rawFragment.split(" ")).toList();
        List<Boolean> matchList = new ArrayList<>();
        for (String lem : mapForCheck.keySet()) {
            matchList.add(mapForCheck.get(lem).stream().anyMatch(rawFragmentList::contains));
        }

        if (matchList.stream().allMatch(t -> t)) {
            snippetSet.add(fragment);
        }
    }

    /**
     * This method requests start and end ids of word which was found, then
     * processes them and returns ids of a fragment containing this word.
     *
     * @param newStart word start id
     * @param newEnd word end id
     * @return array containing fragment ids
     */
    private int[] getFragmentStartAndEnd(int newStart, int newEnd) {
        int fragmentStartId = calculateStartId(newStart);
        int fragmentEndId = calculateEndId(newEnd);
        int[] fragmentIds = new int[2];
        fragmentIds[0] = fragmentStartId;
        fragmentIds[1] = fragmentEndId;
        return fragmentIds;
    }

    /**
     * This method calculates start id of resultant fragment.
     *
     * @param newStart word start id
     * @return id of the beginning of the fragment
     */
    private int calculateStartId(int newStart) {
        int fragmentStartId = newStart;
        while (true) {
            if ((fragmentStartId - 1) < 0) {
                break;
            }
            if (Character.toString(stringDoc.charAt(fragmentStartId - 1)).equals(">")) {
                if (stringDoc.startsWith("</b>", fragmentStartId - 4) ||
                        stringDoc.startsWith("<b>", fragmentStartId - 3)) {
                    fragmentStartId -= 3;
                    continue;
                }
                break;
            } else if (Character.toString(stringDoc.charAt(fragmentStartId - 1)).equals("\"")) {
                break;
            } else {
                fragmentStartId--;
            }
        }
        return fragmentStartId;
    }

    /**
     * This method calculates end id of resultant fragment.
     *
     * @param newEnd word end id
     * @return id of the end of the fragment
     */
    private int calculateEndId(int newEnd) {
        int fragmentEndId = newEnd;
        while (true) {
            if ((fragmentEndId + 1) > stringDoc.length()) {
                break;
            }
            if (Character.toString(stringDoc.charAt(fragmentEndId)).equals("<")) {
                if (stringDoc.startsWith("<b>", fragmentEndId) ||
                        stringDoc.startsWith("</b>", fragmentEndId)) {
                    fragmentEndId += 3;
                    continue;
                }
                break;
            } else if (Character.toString(stringDoc.charAt(fragmentEndId)).equals("\"")) {
                break;
            } else {
                fragmentEndId++;
            }
        }
        return fragmentEndId;
    }
}
