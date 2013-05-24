package org.jenkinsci.plugins.categorizedview;

import hudson.model.TopLevelItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class CategorizedItemsBuilder {
	final Comparator<IndentedTopLevelItem> comparator = new TopLevelItemComparator();
	private List<TopLevelItem> itemsToCategorize;
	private List<GroupingRule> groupingRules;
	private Map<String, IndentedTopLevelItem> itemsData;

	public CategorizedItemsBuilder(List<TopLevelItem> itemsToCategorize, List<GroupingRule> groupingRules) {
		this.itemsToCategorize = itemsToCategorize;
		this.groupingRules = groupingRules;
	}
	
	public List<TopLevelItem> getRegroupedItems() {
		return buildRegroupedItems(itemsToCategorize, groupingRules);
	}

	private List<TopLevelItem> buildRegroupedItems(List<TopLevelItem> items, List<GroupingRule> groupingRules) {
		final List<IndentedTopLevelItem> groupedItems = buildCategorizedList(items, groupingRules);
		return flattenList(groupedItems);
	}
	
	private List<IndentedTopLevelItem> buildCategorizedList(List<TopLevelItem> itemsToCategorize, List<GroupingRule> groupingRules) {
		final List<IndentedTopLevelItem> categorizedItems = new ArrayList<IndentedTopLevelItem>();
		if (groupingRules.size()==0) {
			for (TopLevelItem indentedTopLevelItem : itemsToCategorize) {
				categorizedItems.add(new IndentedTopLevelItem(indentedTopLevelItem));
			}
			return categorizedItems;
		}
		
		for (TopLevelItem item : itemsToCategorize) {
			boolean categorized = tryToFitItemToRegexCategory(groupingRules, categorizedItems, item);
			if (!categorized)
				categorizedItems.add(new IndentedTopLevelItem(item));
		}
		return categorizedItems;
	}

	private boolean tryToFitItemToRegexCategory( List<GroupingRule> groupingRules, final List<IndentedTopLevelItem> categorizedItems, TopLevelItem item) 
	{
		boolean grouped = false;
		for (GroupingRule groupingRule : groupingRules) {
			if (StringUtils.isEmpty(groupingRule.getGroupRegex())) 
				continue;
			
			if (item.getName().matches(groupingRule.getNormalizedGroupRegex())) {
				addItemInMatchingGroup(categorizedItems, groupingRule.getNormalizedGroupRegex(), item, groupingRule.getNamingRule());
				grouped = true;
			}
		}
		return grouped;
	}

	private void addItemInMatchingGroup(final List<IndentedTopLevelItem> groupedItems, String regex, TopLevelItem item, String namingRule) 
	{
		final String groupNamingRule = StringUtils.isEmpty(namingRule)?"$1":namingRule;
		final String groupName = item.getName().replaceAll(regex, groupNamingRule);
		IndentedTopLevelItem groupTopLevelItem = getGroupForItemOrCreateIfNeeded(groupedItems, groupName);
		IndentedTopLevelItem subItem = new IndentedTopLevelItem(item, 1, groupName, "");
		groupTopLevelItem.add(subItem);
	}

	private List<TopLevelItem> flattenList(final List<IndentedTopLevelItem> groupedItems) 
	{
		final ArrayList<TopLevelItem> res = new ArrayList<TopLevelItem>();
		itemsData = new LinkedHashMap<String, IndentedTopLevelItem>();
		Collections.sort(groupedItems, comparator);
		for (IndentedTopLevelItem item : groupedItems) {
			final String groupLabel = item.target.getName();
			addNestedItemsAsIndentedItemsInTheResult(res, item,	groupLabel);
		}
		
		return res;
	}

	private void addNestedItemsAsIndentedItemsInTheResult(final ArrayList<TopLevelItem> res, IndentedTopLevelItem item, final String groupLabel) {
		res.add(item.target);
		itemsData.put(item.target.getName(), item);
		if (item.getNestedItems().size() > 0) {
			List<IndentedTopLevelItem> nestedItems = item.getNestedItems();
			Collections.sort(nestedItems, comparator);
			for (IndentedTopLevelItem indentedTopLevelItem : nestedItems) {
				res.add(indentedTopLevelItem.target);
				itemsData.put(indentedTopLevelItem.target.getName(), indentedTopLevelItem);
			}
		}
	}
	
	final Map<String, IndentedTopLevelItem> groupItemByGroupName = new HashMap<String, IndentedTopLevelItem>();
	private IndentedTopLevelItem getGroupForItemOrCreateIfNeeded(
			final List<IndentedTopLevelItem> groupedItems,
			final String groupName) {
		boolean groupIsMissing = !groupItemByGroupName.containsKey(groupName);
		if (groupIsMissing) {
			GroupTopLevelItem value = new GroupTopLevelItem(groupName);
			IndentedTopLevelItem value2 = new IndentedTopLevelItem(value, 0, groupName, value.getCss());
			groupItemByGroupName.put(groupName, value2);
			groupedItems.add(groupItemByGroupName.get(groupName));
		}
		return groupItemByGroupName.get(groupName);
	}

	public int getNestLevelFor(TopLevelItem identedItem) {
		return itemsData.get(identedItem.getName()).getNestLevel();
	}

	public String getCssFor(TopLevelItem identedItem) {
		return itemsData.get(identedItem.getName()).getCss();
	}

	public String getGrouClassFor(TopLevelItem item) {
		return itemsData.get(item.getName()).getGroupClass();
	}
}
