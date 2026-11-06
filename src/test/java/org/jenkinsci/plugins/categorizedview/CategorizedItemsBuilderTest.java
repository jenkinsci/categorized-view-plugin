package org.jenkinsci.plugins.categorizedview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.TopLevelItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategorizedItemsBuilderTest {

    private final List<TopLevelItem> itemsToCategorize = new ArrayList<>(
            Arrays.asList(makeMockedItem("xa"), makeMockedItem("ba"), makeMockedItem("me"), makeMockedItem("ma")));

    @Test
    void getItems_withNullRegex_ShouldReturnSortedList() {
        String groupRegex = null;
        final CategorizedItemsBuilder subject =
                new CategorizedItemsBuilder(itemsToCategorize, List.of(new GroupingRule(groupRegex, "")));

        String expected = """
                ba
                ma
                me
                xa
                """;

        String actual = buildResultToCompare(subject);
        assertEquals(expected, actual);
    }

    @Test
    void getItems_withEmptyRegex_ShouldReturnSortedList() {
        String groupRegex = "";
        final CategorizedItemsBuilder subject =
                new CategorizedItemsBuilder(itemsToCategorize, List.of(new GroupingRule(groupRegex, "")));
        String actual = buildResultToCompare(subject);

        String expected = """
                ba
                ma
                me
                xa
                """;

        assertEquals(expected, actual);
    }

    @Test
    void getItems_withRegex_ShouldGroupByRegex() {
        itemsToCategorize.add(makeMockedItem("8.03-bar"));
        itemsToCategorize.add(makeMockedItem("8.02-foo"));
        itemsToCategorize.add(makeMockedItem("8.02-baz"));
        itemsToCategorize.add(makeMockedItem("8.03-foo"));
        itemsToCategorize.add(makeMockedItem("a8.03-foo"));

        String groupRegex = "(8...)";
        final CategorizedItemsBuilder subject =
                new CategorizedItemsBuilder(itemsToCategorize, List.of(new GroupingRule(groupRegex, "")));

        String expected = """
                8.02
                  8.02-baz
                  8.02-foo
                8.03
                  8.03-bar
                  8.03-foo
                  a8.03-foo
                ba
                ma
                me
                xa
                """;

        String actual = buildResultToCompare(subject);
        assertEquals(expected, actual);
    }

    @Test
    void getItems_withRegex_andNaming_ShouldGroupByRegexAndNameWithNamingRule() {
        itemsToCategorize.add(makeMockedItem("8.03-bar"));
        itemsToCategorize.add(makeMockedItem("8.02-foo"));
        itemsToCategorize.add(makeMockedItem("8.02-baz"));
        itemsToCategorize.add(makeMockedItem("8.03-foo"));
        itemsToCategorize.add(makeMockedItem("a8.03-foo"));

        String groupRegex = "(8...)";
        final CategorizedItemsBuilder subject =
                new CategorizedItemsBuilder(itemsToCategorize, List.of(new GroupingRule(groupRegex, "foo $1")));
        String expected = """
                foo 8.02
                  8.02-baz
                  8.02-foo
                foo 8.03
                  8.03-bar
                  8.03-foo
                  a8.03-foo
                ba
                ma
                me
                xa
                """;

        String actual = buildResultToCompare(subject);
        assertEquals(expected, actual);
    }

    @Test
    void buildRegroupedItems_withListOfRegexAndNamingRules_ShouldUseAllRulesToCategorize() {
        itemsToCategorize.add(makeMockedItem("8.03-bar"));
        itemsToCategorize.add(makeMockedItem("8.02-foo"));
        itemsToCategorize.add(makeMockedItem("8.02-baz"));
        itemsToCategorize.add(makeMockedItem("8.03-foo"));
        itemsToCategorize.add(makeMockedItem("a8.03-foo"));
        itemsToCategorize.add(makeMockedItem("m8.03-foo"));

        List<GroupingRule> rules = List.of(new GroupingRule("(8...)", "Foo $1"), new GroupingRule("(m)", "baz $1"));
        final CategorizedItemsBuilder subject = new CategorizedItemsBuilder(itemsToCategorize, rules);
        String expected = """
                Foo 8.02
                  8.02-baz
                  8.02-foo
                Foo 8.03
                  8.03-bar
                  8.03-foo
                  a8.03-foo
                  m8.03-foo
                baz m
                  m8.03-foo
                  ma
                  me
                ba
                xa
                """;

        String actual = buildResultToCompare(subject);
        assertEquals(expected, actual);

        List<GroupTopLevelItem> groupItems = subject.getGroupItems();
        assertEquals(3, groupItems.size());
    }

    @Test
    void getItems_withUseFullNameTrue_ShouldGroupByFullNamePath() {
        List<TopLevelItem> multibranchItems = new ArrayList<>(Arrays.asList(
                makeMockedItemWithFullName("PROJ-123", "team-service-repo/PROJ-123"),
                makeMockedItemWithFullName("PROJ-456", "other-service-repo/PROJ-456")));

        String groupRegex = "([^/]+)/PROJ-";
        final CategorizedItemsBuilder subject = new CategorizedItemsBuilder(
                multibranchItems, List.of(new GroupingRule(groupRegex, "$1 - PROJ", false, true)));

        String expected = """
                other-service-repo - PROJ
                  PROJ-456
                team-service-repo - PROJ
                  PROJ-123
                """;

        String actual = buildResultToCompare(subject);
        assertEquals(expected, actual);
    }

    @Test
    void getItems_withUseFullNameFalse_ShouldGroupOnlyByShortName() {
        // Both branches share the short name pattern "PROJ-*" even though they come from
        // different parent jobs (visible only via getFullName()), so with useFullName=false
        // they land in the same group, keyed off of a fixed (non-capturing) naming rule.
        List<TopLevelItem> multibranchItems = new ArrayList<>(Arrays.asList(
                makeMockedItemWithFullName("PROJ-123", "team-service-repo/PROJ-123"),
                makeMockedItemWithFullName("PROJ-456", "other-service-repo/PROJ-456")));

        String groupRegex = "PROJ-.*";
        final CategorizedItemsBuilder subject = new CategorizedItemsBuilder(
                multibranchItems, List.of(new GroupingRule(groupRegex, "PROJ Jobs", false, false)));

        String expected = """
                PROJ Jobs
                  PROJ-123
                  PROJ-456
                """;

        String actual = buildResultToCompare(subject);
        assertEquals(expected, actual);
    }

    @Test
    void buildRegroupedItems_withSharedCategoryNameAcrossRules_ShouldUseEarliestMatchingRuleOrder() {
        List<TopLevelItem> items =
                new ArrayList<>(Arrays.asList(makeMockedItem("a"), makeMockedItem("b"), makeMockedItem("z")));

        // The first and last rules both produce the category "Shared"; the "a" job is
        // processed first, so the shared category is created via the last rule and must
        // still be ordered by the first rule.
        List<GroupingRule> rules = List.of(
                new GroupingRule("(z)", "Shared"),
                new GroupingRule("(b)", "Middle"),
                new GroupingRule("(a)", "Shared"));
        final CategorizedItemsBuilder subject = new CategorizedItemsBuilder(items, rules);

        String expected = """
                Shared
                  a
                  z
                Middle
                  b
                """;

        assertEquals(expected, buildResultToCompare(subject));
    }

    @Test
    void nestedGroups_withSharedParentAcrossRules_shouldUseEarliestMatchingRuleOrder() {
        CategorizedItemsBuilder subject = new CategorizedItemsBuilder(
                List.of(makeMockedItem("a"), makeMockedItem("b"), makeMockedItem("z")),
                List.of(
                        new GroupingRule("z", "Shared/Zulu"),
                        new GroupingRule("b", "Middle/Child"),
                        new GroupingRule("a", "Shared/Alpha")),
                "",
                true);

        assertEquals("""
                Shared
                  Alpha
                    a
                  Zulu
                    z
                Middle
                  Child
                    b
                """, buildResultToCompare(subject));
    }

    @Test
    void nestedGroups_shouldBuildAndMergePathsAtAnyDepth() {
        TopLevelItem first = makeMockedItem("first");
        TopLevelItem second = makeMockedItem("second");
        TopLevelItem direct = makeMockedItem("direct");
        CategorizedItemsBuilder subject = new CategorizedItemsBuilder(
                List.of(first, second, direct),
                List.of(
                        new GroupingRule("first", "FootLocker/FLX User/Build"),
                        new GroupingRule("second", "FootLocker/FLX User/Build"),
                        new GroupingRule("direct", "FootLocker"),
                        new GroupingRule("first", "Other/FLX User/Build")),
                "",
                true);

        assertEquals("""
                FootLocker
                  direct
                  FLX User
                    Build
                      first
                      second
                Other
                  FLX User
                    Build
                      first
                """, buildResultToCompare(subject));
        assertEquals(6, subject.getGroupItems().size());
        GroupTopLevelItem firstLeaf = subject.groupItemByGroupName.get("FootLocker/FLX User/Build");
        GroupTopLevelItem otherLeaf = subject.groupItemByGroupName.get("Other/FLX User/Build");
        assertNotEquals(firstLeaf.getGroupClass(), otherLeaf.getGroupClass());
        assertEquals("Build", firstLeaf.getDisplayName());
        assertEquals("FootLocker/FLX User/Build", firstLeaf.getFullName());
    }

    @Test
    void nestedGroups_shouldSortSubgroupsByLabelAlongsideDirectJobs() {
        CategorizedItemsBuilder subject = new CategorizedItemsBuilder(
                List.of(makeMockedItem("first"), makeMockedItem("Beta"), makeMockedItem("last")),
                List.of(
                        new GroupingRule("first", "Parent/alpha"),
                        new GroupingRule("Beta", "Parent"),
                        new GroupingRule("last", "Parent/Zulu")),
                "",
                true);

        assertEquals("""
                Parent
                  alpha
                    first
                  Beta
                  Zulu
                    last
                """, buildResultToCompare(subject));
    }

    @Test
    void nestedGroups_shouldExpandCaptureSubstitutions() {
        CategorizedItemsBuilder subject = new CategorizedItemsBuilder(
                List.of(makeMockedItem("FootLocker-FLX-build")),
                List.of(new GroupingRule("^([^-]+)-([^-]+)-.*$", "$1/$2")),
                "",
                true);

        assertEquals("""
                FootLocker
                  FLX
                    FootLocker-FLX-build
                """, buildResultToCompare(subject));
    }

    @Test
    void nestedGroups_whenDisabled_shouldPreserveSlashesInNames() {
        CategorizedItemsBuilder subject = new CategorizedItemsBuilder(
                List.of(makeMockedItem("job")), List.of(new GroupingRule("job", "FootLocker/FLX User")));

        assertEquals("""
                FootLocker/FLX User
                  job
                """, buildResultToCompare(subject));
        assertEquals(1, subject.getGroupItems().size());
    }

    @Test
    void nestedGroups_shouldPreserveNamesWithEmptySegments() {
        for (String name : List.of("/A", "A/", "A//B", "")) {
            CategorizationCriteria rule = mock(CategorizationCriteria.class);
            TopLevelItem item = makeMockedItem("job");
            when(rule.groupNameGivenItem(item)).thenReturn(name);
            CategorizedItemsBuilder subject = new CategorizedItemsBuilder(List.of(item), List.of(rule), "", true);

            assertEquals(name + "\n  job\n", buildResultToCompare(subject));
            assertEquals(1, subject.getGroupItems().size());
        }
    }

    private static String buildResultToCompare(final CategorizedItemsBuilder subject) {
        List<TopLevelItem> items = subject.getRegroupedItems();
        StringBuilder sb = new StringBuilder();
        appendItems(sb, items, "");
        return sb.toString();
    }

    private static void appendItems(StringBuilder sb, List<TopLevelItem> items, String indent) {
        for (TopLevelItem item : items) {
            sb.append(indent).append(item.getDisplayName()).append("\n");
            if (item instanceof GroupTopLevelItem group) {
                appendItems(sb, group.getNestedItems(), indent + "  ");
            }
        }
    }

    private static TopLevelItem makeMockedItem(final String value) {
        TopLevelItem mockedItem = mock(TopLevelItem.class);
        when(mockedItem.getName()).thenReturn(value);
        when(mockedItem.getDisplayName()).thenReturn(value);
        return mockedItem;
    }

    private static TopLevelItem makeMockedItemWithFullName(final String shortName, final String fullName) {
        TopLevelItem mockedItem = mock(TopLevelItem.class);
        when(mockedItem.getName()).thenReturn(shortName);
        when(mockedItem.getDisplayName()).thenReturn(shortName);
        when(mockedItem.getFullName()).thenReturn(fullName);
        return mockedItem;
    }
}
