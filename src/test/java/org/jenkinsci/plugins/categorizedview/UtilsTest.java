package org.jenkinsci.plugins.categorizedview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class UtilsTest {

    @Test
    void normalizeRegex_withNullInput_ShouldReturnEmptyString() {
        assertEquals("", Utils.normalizeRegex(null));
    }

    @Test
    void normalizeRegex_alreadyWrappedInAsterisks_ShouldBeUnchanged() {
        assertEquals(".*(s).*", Utils.normalizeRegex(".*(s).*"));
    }

    @Test
    void normalizeRegex_singleWrappingGroup_ShouldKeepGreedyPrefix() {
        assertEquals(".*(.*s).*", Utils.normalizeRegex("(.*s)"));
    }

    @Test
    void normalizeRegex_groupThatDoesNotSpanWholePattern_ShouldUseReluctantPrefix() {
        assertEquals(".*?([^/]+)/PROJ-.*", Utils.normalizeRegex("([^/]+)/PROJ-"));
    }

    @Test
    void normalizeRegex_groupThatDoesNotSpanWholePattern_ShouldCaptureFullSegment() {
        // Regression test: a greedy leading ".*" placed directly before a capturing group would
        // "steal" characters from that group, leaving only the last character of the folder
        // name. See https://github.com/jenkinsci/categorized-view-plugin/issues/14.
        String normalized = Utils.normalizeRegex("([^/]+)/PROJ-");
        Matcher matcher = Pattern.compile(normalized).matcher("team-service-repo/PROJ-123");

        assertTrue(matcher.matches());
        assertEquals("team-service-repo", matcher.group(1));
    }
}
