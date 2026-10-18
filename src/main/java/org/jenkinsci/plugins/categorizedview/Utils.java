package org.jenkinsci.plugins.categorizedview;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Utils {

    static String normalizeRegex(String groupRegex) {
        if (groupRegex == null) {
            return "";
        }
        String regex = groupRegex;
        boolean hasGroup = hasCapturingGroup(regex);
        // A greedy leading ".*" placed directly in front of a capturing group competes with
        // that group for characters: the leftmost quantifier wins, leaving the group with only
        // the minimum it needs (e.g. matching "([^-]+)-" against "PROJ-123" would capture just
        // "J" for group 1 instead of "PROJ"). Using a reluctant ".*?" avoids this when the group
        // doesn't already span the whole pattern. Patterns that are a single wrapping group
        // (e.g. "(8...)") keep the historical greedy prefix so that their behavior is unchanged.
        String leadingWildcard = hasGroup && !isSingleWrappingGroup(regex) ? ".*?" : ".*";
        if (!regex.startsWith(".*")) {
            regex = leadingWildcard + regex;
        }
        if (!regex.endsWith(".*")) {
            regex += ".*";
        }
        if (!hasGroup) {
            regex = ".*(" + groupRegex + ").*";
        }
        return regex;
    }

    private static boolean hasCapturingGroup(String regex) {
        try {
            return Pattern.compile(regex).matcher("").groupCount() > 0;
        } catch (PatternSyntaxException e) {
            // Invalid regex: fall back to the historical heuristic and let the real error
            // surface when the pattern is actually used for matching.
            return regex.contains("(");
        }
    }

    /**
     * @return true if the whole of regex is a single group, e.g. "(8...)" or "((a)b)", as opposed
     *     to a group that is only part of the pattern, e.g. "([^/]+)/PROJ-" or "(a)(b)".
     *     Parentheses that are escaped, inside a character class, or inside a \Q...\E quoted
     *     section are not treated as group delimiters.
     */
    private static boolean isSingleWrappingGroup(String regex) {
        if (!regex.startsWith("(") || !regex.endsWith(")")) {
            return false;
        }
        // Because regex ends with ')', none of '\', '[' or '[^' can be the final character(s),
        // so the look-aheads below (i + 1, and j / j + 1 in skipClassOpener) are always in range.
        int depth = 0;
        int classDepth = 0;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (c == '\\') {
                if (regex.charAt(i + 1) == 'Q') {
                    int end = regex.indexOf("\\E", i + 2);
                    i = end < 0 ? regex.length() : end + 1;
                } else {
                    i++;
                }
                continue;
            }
            if (c == '[') {
                classDepth++;
                i = skipClassOpener(regex, i);
                continue;
            }
            if (classDepth > 0) {
                if (c == ']') {
                    classDepth--;
                }
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i == regex.length() - 1;
                }
            }
        }
        return false;
    }

    /**
     * Having found a '[' at index i, returns the index of the last character that belongs to the
     * class opener: an optional '^', and a ']' directly after it, which Java treats as a literal
     * member of the class rather than its end (e.g. "[]a]" and "[^]a]"). Assumes '[' is not
     * the last character and that '^' is not the last character (see isSingleWrappingGroup).
     */
    private static int skipClassOpener(String regex, int i) {
        int j = i + 1;
        if (regex.charAt(j) == '^') {
            j++;
        }
        if (regex.charAt(j) == ']') {
            return j;
        }
        return j - 1;
    }
}
