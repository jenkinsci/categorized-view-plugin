package org.jenkinsci.plugins.categorizedview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.TopLevelItem;
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

    @Test
    void useFullName_ShouldDefaultToFalse() {
        GroupingRule subject = new GroupingRule("(.*s)", "");
        assertFalse(subject.getUseFullName());
    }

    @Test
    void groupNameGivenItem_withUseFullNameTrue_ShouldMatchOnFullNameNotShortName() {
        TopLevelItem item = mock(TopLevelItem.class);
        when(item.getName()).thenReturn("PROJ-123");
        when(item.getFullName()).thenReturn("team-service-repo/PROJ-123");

        GroupingRule subject = new GroupingRule("([^/]+)/PROJ-", "$1 - PROJ", false, true);

        assertEquals("team-service-repo - PROJ", subject.groupNameGivenItem(item));
    }

    @Test
    void groupNameGivenItem_withUseFullNameFalse_ShouldMatchOnShortNameOnly() {
        TopLevelItem item = mock(TopLevelItem.class);
        when(item.getName()).thenReturn("PROJ-123");
        when(item.getFullName()).thenReturn("team-service-repo/PROJ-123");

        // The full-name-only pattern can't match against the short name, so the item is
        // not categorized when useFullName is left at its default (false).
        GroupingRule subject = new GroupingRule("([^/]+)/PROJ-", "$1 - PROJ", false, false);

        assertEquals(null, subject.groupNameGivenItem(item));
    }

    @Test
    void groupNameGivenItem_withUseFullNameFalse_ShouldStillGroupByShortName() {
        TopLevelItem item = mock(TopLevelItem.class);
        when(item.getName()).thenReturn("PROJ-123");
        when(item.getFullName()).thenReturn("team-service-repo/PROJ-123");

        GroupingRule subject = new GroupingRule("PROJ-.*", "", false, false);

        assertEquals("PROJ-123", subject.groupNameGivenItem(item));
    }

    @Test
    void groupNameGivenItem_withUseFullNameAndUseDisplayNameTrue_FullNameShouldWin() {
        TopLevelItem item = mock(TopLevelItem.class);
        when(item.getName()).thenReturn("PROJ-123");
        when(item.getDisplayName()).thenReturn("Display PROJ-123");
        when(item.getFullName()).thenReturn("team-service-repo/PROJ-123");

        GroupingRule subject = new GroupingRule("([^/]+)/PROJ-", "$1 - PROJ", true, true);

        assertEquals("team-service-repo - PROJ", subject.groupNameGivenItem(item));
    }
}
