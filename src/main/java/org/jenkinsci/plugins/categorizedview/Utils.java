package org.jenkinsci.plugins.categorizedview;

public class Utils {

    static String normalizeRegex(String groupRegex) {
        if (groupRegex == null) {
            return "";
        }
        String regex = groupRegex;
        boolean hasGroup = regex.contains("(");
        // A greedy leading ".*" placed directly in front of a capturing group competes with
        // that group for characters: the leftmost quantifier wins, leaving the group with only
        // the minimum it needs (e.g. matching "([^/]+)/PROJ-" against "folder/PROJ-123" would
        // capture just "r" for group 1 instead of "folder"). Using a reluctant ".*?" avoids this
        // when the group doesn't already span the whole pattern. Patterns that are a single
        // wrapping group (e.g. "(8...)") are unaffected, since there is no competition for
        // characters outside the group in that case.
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

    /**
     * @return true if the whole of regex is a single capturing/non-capturing group, e.g.
     *     "(8...)", as opposed to a group that is only part of the pattern, e.g. "([^/]+)/PROJ-".
     */
    private static boolean isSingleWrappingGroup(String regex) {
        if (!regex.startsWith("(") || !regex.endsWith(")")) {
            return false;
        }
        int depth = 0;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (c == '\\') {
                i++;
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
}
