package org.jenkinsci.plugins.categorizedview;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UtilsTest {

    @Test
    void normalizeRegex_withNullInput_ShouldReturnEmptyString() {
        assertEquals("", Utils.normalizeRegex(null));
    }

    @Test
    void normalizeRegex_alreadyWrappedInAsterisks_ShouldBeUnchanged() {
        assertEquals(".*(s).*", Utils.normalizeRegex(".*(s).*"));
    }

    @ParameterizedTest(name = "{0} is a single wrapping group")
    @CsvSource(
            delimiter = ';',
            value = {
                "(.*s)            ; .*(.*s).*",
                "(8...)           ; .*(8...).*",
                "((a)b)           ; .*((a)b).*",
                "([)]a*)          ; .*([)]a*).*",
                "([^)]+)          ; .*([^)]+).*",
                "([]a]+)          ; .*([]a]+).*",
                "([^]a]+)         ; .*([^]a]+).*",
                "([a-z[)]]+)      ; .*([a-z[)]]+).*",
                "(\\Q(x)\\E-\\d+) ; .*(\\Q(x)\\E-\\d+).*",
                "(\\(a\\))        ; .*(\\(a\\)).*",
            })
    void normalizeRegex_singleWrappingGroup_ShouldKeepGreedyPrefix(String input, String expected) {
        assertEquals(expected, Utils.normalizeRegex(input));
    }

    @ParameterizedTest(name = "{0} is not a single wrapping group")
    @CsvSource(
            delimiter = ';',
            value = {
                "([^/]+)/PROJ-    ; .*?([^/]+)/PROJ-.*",
                "([^-]+)-         ; .*?([^-]+)-.*",
                "(a)(b)           ; .*?(a)(b).*",
                "(a)[)]           ; .*?(a)[)].*",
                "x(a)             ; .*?x(a).*",
            })
    void normalizeRegex_groupThatDoesNotSpanWholePattern_ShouldUseReluctantPrefix(String input, String expected) {
        assertEquals(expected, Utils.normalizeRegex(input));
    }

    @ParameterizedTest(name = "{0} has no capturing group")
    @CsvSource(
            delimiter = ';',
            value = {
                "8...             ; .*(8...).*",
                "\\(a\\)          ; .*(\\(a\\)).*",
                "(?:DX|GS)-       ; .*((?:DX|GS)-).*",
                "[(]              ; .*([(]).*",
            })
    void normalizeRegex_withoutCapturingGroup_ShouldWrapWholePatternInGroup(String input, String expected) {
        assertEquals(expected, Utils.normalizeRegex(input));
    }

    @Test
    void normalizeRegex_withInvalidRegex_ShouldNotThrow() {
        assertDoesNotThrow(() -> Utils.normalizeRegex("(abc"));
    }

    @ParameterizedTest(name = "{0} against {1} captures {2}")
    @CsvSource(
            delimiter = ';',
            value = {
                "([^/]+)/PROJ-    ; team-service-repo/PROJ-123 ; team-service-repo",
                "([^-]+)-         ; PROJ-123                   ; PROJ",
                "([^-]+)-         ; PROJX-5                    ; PROJX",
                "(8...)           ; 8.02-8.03                  ; 8.03",
            })
    void normalizeRegex_ShouldCaptureExpectedGroup(String regex, String input, String expectedGroup) {
        // Regression test: a greedy leading ".*" placed directly before a capturing group would
        // "steal" characters from that group, leaving only its last character. Single wrapping
        // groups keep the historical greedy behavior (last occurrence wins).
        // See https://github.com/jenkinsci/categorized-view-plugin/issues/14.
        Matcher matcher = Pattern.compile(Utils.normalizeRegex(regex)).matcher(input);

        assertTrue(matcher.matches());
        assertEquals(expectedGroup, matcher.group(1));
    }
}
