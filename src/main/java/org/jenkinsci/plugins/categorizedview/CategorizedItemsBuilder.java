package org.jenkinsci.plugins.categorizedview;

import hudson.model.TopLevelItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategorizedItemsBuilder {
	final Comparator<TopLevelItem> comparator = new TopLevelItemComparator();
	private List<TopLevelItem> itemsToCategorize;
	private List<GroupTopLevelItem> groupItems = new ArrayList<GroupTopLevelItem>();
	private List<? extends CategorizationCriteria> groupingRules;
	private Map<String, TopLevelItem> itemsData;
	private String regexToIgnoreOnColorComputing;

	public CategorizedItemsBuilder(List<TopLevelItem> itemsToCategorize, List<? extends CategorizationCriteria> groupingRules) {
		this(itemsToCategorize, groupingRules, "");
	}
	
	public CategorizedItemsBuilder(List<TopLevelItem> items, List<? extends CategorizationCriteria> groupingRules, String regexToIgnoreOnColorComputing) {
		this.itemsToCategorize = items;
		this.groupingRules = groupingRules;
		this.regexToIgnoreOnColorComputing = regexToIgnoreOnColorComputing;
	}

	public List<TopLevelItem> getRegroupedItems() {
		return  buildRegroupedItems(itemsToCategorize);
	}

	private List<TopLevelItem> buildRegroupedItems(List<TopLevelItem> items) {
		return flattenList(buildCategorizedList(items));
	}
	
	private List<TopLevelItem> buildCategorizedList(List<TopLevelItem> itemsToCategorize) {
		final List<TopLevelItem> categorizedItems = new ArrayList<TopLevelItem>();
		if (groupingRules.size()==0) {
			for (TopLevelItem indentedTopLevelItem : itemsToCategorize) {
				categorizedItems.add(indentedTopLevelItem);
			}
			return categorizedItems;
		}
		
		for (TopLevelItem item : itemsToCategorize) {
			boolean categorized = tryToFitItemInCategory(groupingRules, categorizedItems, item);
			if (!categorized)
				categorizedItems.add(item);
		}
		return categorizedItems;
	}

	private boolean tryToFitItemInCategory(
			List<? extends CategorizationCriteria> groupingRules, 
			final List<TopLevelItem> categorizedItems, 
			TopLevelItem item) 
	{
		boolean grouped = false;
		for (CategorizationCriteria groupingRule : groupingRules) {
			String groupNameGivenItem = groupingRule.groupNameGivenItem(item);
			if (groupNameGivenItem!=null) {
				addItemToAppropriateGroup(groupNameGivenItem, categorizedItems, item, groupingRule);
				grouped = true;
			}
		}
		return grouped;
	}

	public void addItemToAppropriateGroup(
			final String groupName,
			final List<TopLevelItem> categorizedItems,
			TopLevelItem item, CategorizationCriteria groupingRule) 
	{
		GroupTopLevelItem groupTopLevelItem = getGroupForItemOrCreateIfNeeded(categorizedItems, groupName);
		groupTopLevelItem.add(item);
	}

	private List<TopLevelItem> flattenList(final List<TopLevelItem> groupedItems) 
	{
		final ArrayList<TopLevelItem> res = new ArrayList<TopLevelItem>();
		itemsData = new LinkedHashMap<String, TopLevelItem>();
		Collections.sort(groupedItems, comparator);
		for (TopLevelItem item : groupedItems) {
			final String groupLabel = item.getName();
			addNestedItemsAsIndentedItemsInTheResult(res, item,	groupLabel);
		}
		
		return res;
	}

	private void addNestedItemsAsIndentedItemsInTheResult(final ArrayList<TopLevelItem> res, TopLevelItem item, final String groupLabel) {
		res.add(item);
		
		itemsData.put(item.getName(), item);
	}
	
	final Map<String, GroupTopLevelItem> groupItemByGroupName = new HashMap<String, GroupTopLevelItem>();
	private GroupTopLevelItem getGroupForItemOrCreateIfNeeded(
			final List<TopLevelItem> groupedItems,
			final String groupName) 
	{
		boolean groupIsMissing = !groupItemByGroupName.containsKey(groupName);
		if (groupIsMissing) {
			GroupTopLevelItem value = new GroupTopLevelItem(groupName, regexToIgnoreOnColorComputing);
			groupItems.add(value);
			groupItemByGroupName.put(groupName, value);
			groupedItems.add(groupItemByGroupName.get(groupName));
		}
		return groupItemByGroupName.get(groupName);
	}

	public String getGroupClassFor(TopLevelItem item) {
		if (item instanceof GroupTopLevelItem)
			return ((GroupTopLevelItem)item).getGroupClass();
		return "";
	}

	public List<GroupTopLevelItem> getGroupItems() {
		return groupItems;
	}
}
