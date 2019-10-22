package com.bluberry.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The wrapper class over the standard tools for java regular expression Instead
 * of classes Pattern, Matcher and cycles, use the approach and use the same php
 */
public class RegexpUtils {
    /**
     * Interface which must implement the one who wants to process, replace
     * every occurrence of the program
     */
    public static interface Replacer {
        /**
         * The method should return a string which will be the replacement of
         * the found regexp-th fragment
         *
         * @Param matches a list with information about the fragments found, the
         * zero element of the list Contains the entire text of
         * "coincidences" The rest of the elements 1,2, ... contain
         * values for the groups within the regular expression
         * @Return
         */
        public String onMatch(List<String> matches);
    }

    /**
     * Cache, which stores the compiled regexp-expression
     */
    private static HashMap<String, Pattern> cache = new HashMap<String, Pattern>();

    /**
     * Ochiska cache compiled regexp-expression
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Do a search in the pattern string and replace it with a new value is
     * calculated dynamically, the user
     *
     * @Param pattern pattern (regexp)
     * @Param input string, where search
     * @Param by object Replacer - sets the value to make the change that
     * @Return string after replacement
     */
    public static String preg_replace_callback(String pattern, String input, Replacer by) {
        Pattern p = compile(pattern, false);
        Matcher m = p.matcher(input);
        final int gcount = m.groupCount();
        StringBuffer sb = new StringBuffer();
        ArrayList<String> row = new ArrayList<String>();

        while (m.find()) {
            try {
                row.clear();
                for (int i = 0; i <= gcount; i++)
                    row.add(m.group(i));
                m.appendReplacement(sb, by.onMatch(row));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }// end -- while --
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Do a search in the pattern string and replace it with the new values are
     * calculated by means of Regexp-expression
     *
     * @Param pattern pattern (regexp)
     * @Param input string, where search
     * @Param by a string, which must be replaced by the value found
     * @Return string after replacement
     */
    public static String preg_replace(String pattern, String input, String by) {
        Pattern p = compile(pattern, false);
        Matcher m = p.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            try {
                m.appendReplacement(sb, by);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }// end -- while --
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Check whether the line of assotsiirutsya pattern
     *
     * @Param pattern pattern (regexp)
     * @Param input string, where search
     * @Param rez list which to store information about the match: Zero element
     * of the list contains all the text matches 1, 2, ... contain the
     * values of the groups
     * @Return boolean - a sign that the association occurred
     */
    public static boolean preg_match(String pattern, String input, List<String> rez) {
        Pattern p = compile(pattern, true);
        Matcher m = p.matcher(input);
        final int gcount = m.groupCount();
        if (rez != null)
            rez.clear();
        if (m.matches())
            for (int i = 0; i <= gcount; i++) {
                if (rez != null)
                    rez.add(m.group(i));
            }
        return rez.size() > 0;
    }

    /**
     * Verifying that a string contains a certain pattern and returns a list
     * with all found groups of matches
     *
     * @Param pattern pattern (regexp)
     * @Param input string, where search
     * @Param rez list, which will be placed all found matching, a list of two
     * levels: the first level Lists the objects lists, each of which
     * contains information about Another coincidence, in the same format
     * as the method preg_match
     * @Return
     */
    public static boolean preg_match_all(String pattern, String input, List<List<String>> rez) {
        Pattern p = compile(pattern, true);
        Matcher m = p.matcher(input);
        final int gcount = m.groupCount();
        if (rez != null)
            rez.clear();
        while (m.find()) {
            ArrayList<String> row = new ArrayList<String>();
            for (int i = 0; i <= gcount; i++) {
                if (rez != null)
                    row.add(m.group(i));
            }
            if (rez != null)
                rez.add(row);
        }
        return rez.size() > 0;
    }

    /**
     * Slezhebny method compiles a regexp-and store it in the cache
     *
     * @Param pattern the text of the regular expression
     * @Param surroundBy indication whether expression surround. *?
     * @Return the compiled Pattern
     */
    private static Pattern compile(String pattern, boolean surroundBy) {
        if (cache.containsKey(pattern))
            return cache.get(pattern);
        final String pattern_orig = pattern;

        final char firstChar = pattern.charAt(0);
        char endChar = firstChar;
        if (firstChar == '(')
            endChar = '}';
        if (firstChar == '[')
            endChar = ']';
        if (firstChar == '{')
            endChar = '}';
        if (firstChar == '<')
            endChar = '>';

        int lastPos = pattern.lastIndexOf(endChar);
        if (lastPos == -1)
            throw new RuntimeException("Invalid pattern: " + pattern);

        char[] modifiers = pattern.substring(lastPos + 1).toCharArray();
        int mod = 0;
        for (int i = 0; i < modifiers.length; i++) {
            char modifier = modifiers[i];
            switch (modifier) {
                case 'i':
                    mod |= Pattern.CASE_INSENSITIVE;
                    break;
                case 'd':
                    mod |= Pattern.UNIX_LINES;
                    break;
                case 'x':
                    mod |= Pattern.COMMENTS;
                    break;
                case 'm':
                    mod |= Pattern.MULTILINE;
                    break;
                case 's':
                    mod |= Pattern.DOTALL;
                    break;
                case 'u':
                    mod |= Pattern.UNICODE_CASE;
                    break;
            }
        }
        pattern = pattern.substring(1, lastPos);
        if (surroundBy) {
            if (pattern.charAt(0) != '^')
                pattern = ".*?" + pattern;
            if (pattern.charAt(pattern.length() - 1) != '$')
                pattern = pattern + ".*?";
        }

        final Pattern rezPattern = Pattern.compile(pattern, mod);
        cache.put(pattern_orig, rezPattern);
        return rezPattern;
    }
}