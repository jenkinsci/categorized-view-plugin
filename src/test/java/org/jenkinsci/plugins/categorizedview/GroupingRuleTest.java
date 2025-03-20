package org.jenkinsci.plugins.categorizedview;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GroupingRuleTest {

    @Test
    void regexShouldNotBeNormalizedIfNotNeeded() {
        GroupingRule subject = new GroupingRule(".*(s).*", "");
        assertEquals(".*(s).*", subject.getNormalizedGroupRegex());
    }

    @Test
    void regexWithoutLeadingAndEndingAsterisk_ShoulAddThem() {
        GroupingRule subject = new GroupingRule("(.*s)", "");
        assertEquals(".*(.*s).*", subject.getNormalizedGroupRegex());
    }
}
