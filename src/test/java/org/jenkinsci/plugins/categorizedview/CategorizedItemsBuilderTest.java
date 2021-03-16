package org.jenkinsci.plugins.categorizedview;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import hudson.model.TopLevelItem;

public class CategorizedItemsBuilderTest {

	private List<TopLevelItem> itemsToCategorize;

	public CategorizedItemsBuilderTest() {
		itemsToCategorize = new ArrayList<>();
		itemsToCategorize.add(makeMockedItem("xa"));
		itemsToCategorize.add(makeMockedItem("ba"));
		itemsToCategorize.add(makeMockedItem("me"));
		itemsToCategorize.add(makeMockedItem("ma"));
	}

	@Test
	public void getItems_withNullRegex_ShouldReturnSortedList() {
		String groupRegex = null;
		final CategorizedItemsBuilder subject = new CategorizedItemsBuilder(itemsToCategorize,
				Arrays.asList(new GroupingRule(groupRegex, "")));

		String expected = "ba\n" +
				"ma\n" +
				"me\n" +
				"xa\n";

		String actual = buildResultToCompare(subject);
		assertEquals(expected, actual);
	}

	@Test
	public void getItems_withEmptyRegex_ShouldReturnSortedList() {
		String groupRegex = "";
		final CategorizedItemsBuilder subject = new CategorizedItemsBuilder(itemsToCategorize,
				Arrays.asList(new GroupingRule(groupRegex, "")));
		String actual = buildResultToCompare(subject);

		String expected = "ba\n" +
				"ma\n" +
				"me\n" +
				"xa\n";

		assertEquals(expected, actual);
	}

	@Test
	public void getItems_withRegex_ShouldGroupByRegex() {
		itemsToCategorize.add(makeMockedItem("8.03-bar"));
		itemsToCategorize.add(makeMockedItem("8.02-foo"));
		itemsToCategorize.add(makeMockedItem("8.02-baz"));
		itemsToCategorize.add(makeMockedItem("8.03-foo"));
		itemsToCategorize.add(makeMockedItem("a8.03-foo"));

		String groupRegex = "(8...)";
		final CategorizedItemsBuilder subject = new CategorizedItemsBuilder(itemsToCategorize,
				Arrays.asList(new GroupingRule(groupRegex, "")));

		String expected = "8.02\n" +
				"  8.02-baz\n" +
				"  8.02-foo\n" +
				"8.03\n" +
				"  8.03-bar\n" +
				"  8.03-foo\n" +
				"  a8.03-foo\n" +
				"ba\n" +
				"ma\n" +
				"me\n" +
				"xa\n";

		String actual = buildResultToCompare(subject);
		assertEquals(expected, actual);
	}

	@Test
	public void getItems_withRegex_andNaming_ShouldGroupByRegexAndNameWithNamingRule() {
		itemsToCategorize.add(makeMockedItem("8.03-bar"));
		itemsToCategorize.add(makeMockedItem("8.02-foo"));
		itemsToCategorize.add(makeMockedItem("8.02-baz"));
		itemsToCategorize.add(makeMockedItem("8.03-foo"));
		itemsToCategorize.add(makeMockedItem("a8.03-foo"));

		String groupRegex = "(8...)";
		final CategorizedItemsBuilder subject = new CategorizedItemsBuilder(itemsToCategorize,
				Arrays.asList(new GroupingRule(groupRegex, "foo $1")));
		String expected = "ba\n" +
				"foo 8.02\n" +
				"  8.02-baz\n" +
				"  8.02-foo\n" +
				"foo 8.03\n" +
				"  8.03-bar\n" +
				"  8.03-foo\n" +
				"  a8.03-foo\n" +
				"ma\n" +
				"me\n" +
				"xa\n";

		String actual = buildResultToCompare(subject);
		assertEquals(expected, actual);
	}

	@Test
	public void buildRegroupedItems_withListOfRegexAndNamingRules_ShouldUseAllRulesToCategorize() {
		itemsToCategorize.add(makeMockedItem("8.03-bar"));
		itemsToCategorize.add(makeMockedItem("8.02-foo"));
		itemsToCategorize.add(makeMockedItem("8.02-baz"));
		itemsToCategorize.add(makeMockedItem("8.03-foo"));
		itemsToCategorize.add(makeMockedItem("a8.03-foo"));
		itemsToCategorize.add(makeMockedItem("m8.03-foo"));

		List<GroupingRule> rules = Arrays.asList(
				new GroupingRule("(8...)", "Foo $1"),
				new GroupingRule("(m)", "baz $1"));
		final CategorizedItemsBuilder subject = new CategorizedItemsBuilder(itemsToCategorize, rules);
		String expected = "ba\n" +
				"baz m\n" +
				"  m8.03-foo\n" +
				"  ma\n" +
				"  me\n" +
				"Foo 8.02\n" +
				"  8.02-baz\n" +
				"  8.02-foo\n" +
				"Foo 8.03\n" +
				"  8.03-bar\n" +
				"  8.03-foo\n" +
				"  a8.03-foo\n" +
				"  m8.03-foo\n" +
				"xa\n";

		String actual = buildResultToCompare(subject);
		assertEquals(expected, actual);

		List<GroupTopLevelItem> groupItems = subject.getGroupItems();
		assertEquals(3, groupItems.size());

		Collections.sort(groupItems, new Comparator<GroupTopLevelItem>() {
			@Override
			public int compare(final GroupTopLevelItem o1, final GroupTopLevelItem o2) {
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		assertEquals("baz m", groupItems.get(0).getName());
		assertEquals(3, groupItems.get(0).getNestedItems().size());
		assertEquals("Foo 8.02", groupItems.get(1).getName());
		assertEquals("Foo 8.03", groupItems.get(2).getName());
	}

	private String buildResultToCompare(final CategorizedItemsBuilder subject) {
		List<TopLevelItem> items = subject.getRegroupedItems();
		StringBuffer sb = new StringBuffer();
		for (TopLevelItem identedItem : items) {
			sb.append(identedItem.getName());
			sb.append("\n");

			if (identedItem instanceof GroupTopLevelItem) {
				List<TopLevelItem> nestedItems = ((GroupTopLevelItem) identedItem).getNestedItems();
				for (TopLevelItem item : nestedItems) {
					sb.append("  ");
					sb.append(item.getName());
					sb.append("\n");
				}
			}
		}
		return sb.toString();
	}

	private TopLevelItem makeMockedItem(final String value) {
		TopLevelItem mockedItem = mock(TopLevelItem.class);
		when(mockedItem.getName()).thenReturn(value);
		return mockedItem;
	}
}