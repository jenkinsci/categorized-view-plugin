package org.jenkinsci.plugins.categorizedview;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.TopLevelItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

public class CategorizedItemsBuilderTest {
	
	private List<TopLevelItem> itemsToCategorize;

	public CategorizedItemsBuilderTest() {
		itemsToCategorize = new ArrayList<TopLevelItem>();
		itemsToCategorize.add(makeMockedItem("xa"));
		itemsToCategorize.add(makeMockedItem("ba"));
		itemsToCategorize.add(makeMockedItem("me"));
		itemsToCategorize.add(makeMockedItem("ma"));
	}
	
	@Test
	public void getItems_withNullRegex_ShouldReturnSortedList() {
		String groupRegex = null;
		final CategorizedItemsBuilder subject = new CategorizedItemsBuilder(itemsToCategorize, Arrays.asList(new GroupingRule(groupRegex, "")));
		
		String expected = 
				"ba    css:padding-left:20px;\n" + 
				"ma    css:padding-left:20px;\n" + 
				"me    css:padding-left:20px;\n" + 
				"xa    css:padding-left:20px;\n";
		
		String actual = buildResultToCompare(subject);
		assertEquals(expected, actual);
	}
	
	@Test
	public void getItems_withEmptyRegex_ShouldReturnSortedList() {
		String groupRegex = "";
		final CategorizedItemsBuilder subject = new CategorizedItemsBuilder(itemsToCategorize, Arrays.asList(new GroupingRule(groupRegex, "")));
		String actual = buildResultToCompare(subject);
		
		String expected = 
				"ba    css:padding-left:20px;\n" + 
				"ma    css:padding-left:20px;\n" + 
				"me    css:padding-left:20px;\n" + 
				"xa    css:padding-left:20px;\n";
		
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
		final CategorizedItemsBuilder subject = new CategorizedItemsBuilder(itemsToCategorize, Arrays.asList(new GroupingRule(groupRegex, "")));
		
		String expected =
				"8.02    css:padding-left:20px;font-weight:bold;\n" + 
				"  8.02-baz    css:padding-left:40px;\n" + 
				"  8.02-foo    css:padding-left:40px;\n" + 
				"8.03    css:padding-left:20px;font-weight:bold;\n" + 
				"  8.03-bar    css:padding-left:40px;\n" + 
				"  8.03-foo    css:padding-left:40px;\n" + 
				"  a8.03-foo    css:padding-left:40px;\n" + 
				"ba    css:padding-left:20px;\n" + 
				"ma    css:padding-left:20px;\n" + 
				"me    css:padding-left:20px;\n" + 
				"xa    css:padding-left:20px;\n";
		
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
		final CategorizedItemsBuilder subject = new CategorizedItemsBuilder(itemsToCategorize, Arrays.asList(new GroupingRule(groupRegex, "foo $1")));
		String expected =
				"ba    css:padding-left:20px;\n" + 
				"foo 8.02    css:padding-left:20px;font-weight:bold;\n" + 
				"  8.02-baz    css:padding-left:40px;\n" + 
				"  8.02-foo    css:padding-left:40px;\n" + 
				"foo 8.03    css:padding-left:20px;font-weight:bold;\n" + 
				"  8.03-bar    css:padding-left:40px;\n" + 
				"  8.03-foo    css:padding-left:40px;\n" + 
				"  a8.03-foo    css:padding-left:40px;\n" + 
				"ma    css:padding-left:20px;\n" + 
				"me    css:padding-left:20px;\n" + 
				"xa    css:padding-left:20px;\n";
		
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
				new GroupingRule("(m)", "baz $1")
				);
		final CategorizedItemsBuilder subject = new CategorizedItemsBuilder(itemsToCategorize, rules);
		String expected =
				"ba    css:padding-left:20px;\n" + 
				"baz m    css:padding-left:20px;font-weight:bold;\n" + 
				"  m8.03-foo    css:padding-left:40px;\n" + 
				"  ma    css:padding-left:40px;\n" + 
				"  me    css:padding-left:40px;\n" + 
				"Foo 8.02    css:padding-left:20px;font-weight:bold;\n" + 
				"  8.02-baz    css:padding-left:40px;\n" + 
				"  8.02-foo    css:padding-left:40px;\n" + 
				"Foo 8.03    css:padding-left:20px;font-weight:bold;\n" + 
				"  8.03-bar    css:padding-left:40px;\n" + 
				"  8.03-foo    css:padding-left:40px;\n" + 
				"  a8.03-foo    css:padding-left:40px;\n" + 
				"  m8.03-foo    css:padding-left:40px;\n" + 
				"xa    css:padding-left:20px;\n"; 
		
		String actual = buildResultToCompare(subject);
		assertEquals(expected, actual);
	}

	private String buildResultToCompare(CategorizedItemsBuilder subject) {
		List<TopLevelItem> items = subject.getRegroupedItems();
		StringBuffer sb = new StringBuffer();
		for (TopLevelItem identedItem : items) {
			sb.append(StringUtils.repeat("  ", subject.getNestLevelFor(identedItem)));
			sb.append(identedItem.getName());
			sb.append("    css:");
			sb.append(subject.getCssFor(identedItem));
			sb.append("\n");
		}
		return sb.toString();
	}
	
	private TopLevelItem makeMockedItem(String value) {
		TopLevelItem mockedItem = mock(TopLevelItem.class);
		when(mockedItem.getName()).thenReturn(value);
		return mockedItem;
	}
}