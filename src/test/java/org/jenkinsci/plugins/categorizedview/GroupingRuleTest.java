package org.jenkinsci.plugins.categorizedview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.Descriptor.FormException;
import hudson.model.TopLevelItem;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.Test;

class GroupingRuleTest {

    @Test
    void invalidLegacyRulesShouldLeaveItemsUngrouped() {
        TopLevelItem item = mock(TopLevelItem.class);
        when(item.getName()).thenReturn("first");
        for (String[] rule :
                new String[][] {{"[", "$1"}, {"first", "$2"}, {"first", "${missing}"}, {"first", "$"}, {"first", "\\"}
                }) {
            GroupingRule subject = new GroupingRule(rule[0], rule[1]);
            assertNull(subject.groupNameGivenItem(item));
            assertNull(subject.groupNameGivenItem(item));
        }
    }

    @Test
    void validationShouldCheckReferencesWithoutNeedingAMatchingJob() throws Exception {
        var descriptor = new GroupingRule.DescriptorImpl();
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckGroupRegex("[", "$1").kind);
        for (String replacement : new String[] {"$2", "${missing}", "$", "\\"}) {
            assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckNamingRule(replacement, "(?<name>first)").kind);
            JSONObject form = new JSONObject();
            form.put("groupRegex", "(?<name>first)");
            form.put("namingRule", replacement);
            assertThrows(
                    FormException.class,
                    () -> descriptor.newInstance((org.kohsuke.stapler.StaplerRequest2) null, form));
        }
        for (String replacement : new String[] {"", "$0", "$1", "$12", "${name}", "\\$1", "\\\\"}) {
            assertEquals(FormValidation.Kind.OK, descriptor.doCheckNamingRule(replacement, "(?<name>first)").kind);
        }
    }

    @Test
    void namedAndEscapedReplacementsShouldKeepJavaSemantics() {
        TopLevelItem item = mock(TopLevelItem.class);
        when(item.getName()).thenReturn("first");
        assertEquals("first2", new GroupingRule("(?<name>first)", "$12").groupNameGivenItem(item));
        assertEquals("first", new GroupingRule("(?<name>first)", "${name}").groupNameGivenItem(item));
        assertEquals("$1", new GroupingRule("first", "\\$1").groupNameGivenItem(item));
    }

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
    void threeArgConstructor_ShouldUseDisplayNameAndDefaultUseFullNameToFalse() {
        TopLevelItem item = mock(TopLevelItem.class);
        when(item.getName()).thenReturn("PROJ-123");
        when(item.getDisplayName()).thenReturn("Billing PROJ-123");
        when(item.getFullName()).thenReturn("team-service-repo/PROJ-123");

        GroupingRule subject = new GroupingRule("(\\w+) PROJ-", "$1", true);

        assertFalse(subject.getUseFullName());
        assertEquals("Billing", subject.groupNameGivenItem(item));
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
