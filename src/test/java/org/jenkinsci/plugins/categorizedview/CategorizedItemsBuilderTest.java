package org.jenkinsci.plugins.categorizedview;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                ba
                foo 8.02
                  8.02-baz
                  8.02-foo
                foo 8.03
                  8.03-bar
                  8.03-foo
                  a8.03-foo
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
                ba
                baz m
                  m8.03-foo
                  ma
                  me
                Foo 8.02
                  8.02-baz
                  8.02-foo
                Foo 8.03
                  8.03-bar
                  8.03-foo
                  a8.03-foo
                  m8.03-foo
                xa
                """;

        String actual = buildResultToCompare(subject);
        assertEquals(expected, actual);

        List<GroupTopLevelItem> groupItems = subject.getGroupItems();
        assertEquals(3, groupItems.size());

        groupItems.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        assertEquals("baz m", groupItems.get(0).getName());
        assertEquals(3, groupItems.get(0).getNestedItems().size());
        assertEquals("Foo 8.02", groupItems.get(1).getName());
        assertEquals("Foo 8.03", groupItems.get(2).getName());
    }

    private static String buildResultToCompare(final CategorizedItemsBuilder subject) {
        List<TopLevelItem> items = subject.getRegroupedItems();
        StringBuilder sb = new StringBuilder();
        for (TopLevelItem indentedItem : items) {
            sb.append(indentedItem.getName());
            sb.append("\n");

            if (indentedItem instanceof GroupTopLevelItem) {
                List<TopLevelItem> nestedItems = ((GroupTopLevelItem) indentedItem).getNestedItems();
                for (TopLevelItem item : nestedItems) {
                    sb.append("  ");
                    sb.append(item.getName());
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    private static TopLevelItem makeMockedItem(final String value) {
        TopLevelItem mockedItem = mock(TopLevelItem.class);
        when(mockedItem.getName()).thenReturn(value);
        return mockedItem;
    }
}
