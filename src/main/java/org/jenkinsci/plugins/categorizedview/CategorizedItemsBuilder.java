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
	final Comparator<IndentedTopLevelItem> comparator = new TopLevelItemComparator();
	private List<TopLevelItem> itemsToCategorize;
	private List<GroupTopLevelItem> groupItems = new ArrayList<GroupTopLevelItem>();
	private List<? extends CategorizationCriteria> groupingRules;
	private Map<String, IndentedTopLevelItem> itemsData;

	public CategorizedItemsBuilder(List<TopLevelItem> itemsToCategorize, List<? extends CategorizationCriteria> groupingRules) {
		this.itemsToCategorize = itemsToCategorize;
		this.groupingRules = groupingRules;
	}
	
	public List<TopLevelItem> getRegroupedItems() {
		return buildRegroupedItems(itemsToCategorize);
	}

	private List<TopLevelItem> buildRegroupedItems(List<TopLevelItem> items) {
		return flattenList(buildCategorizedList(items));
	}
	
	private List<IndentedTopLevelItem> buildCategorizedList(List<TopLevelItem> itemsToCategorize) {
		final List<IndentedTopLevelItem> categorizedItems = new ArrayList<IndentedTopLevelItem>();
		if (groupingRules.size()==0) {
			for (TopLevelItem indentedTopLevelItem : itemsToCategorize) {
				categorizedItems.add(new IndentedTopLevelItem(indentedTopLevelItem));
			}
			return categorizedItems;
		}
		
		for (TopLevelItem item : itemsToCategorize) {
			boolean categorized = tryToFitItemInCategory(groupingRules, categorizedItems, item);
			if (!categorized)
				categorizedItems.add(new IndentedTopLevelItem(item));
		}
		return categorizedItems;
	}

	private boolean tryToFitItemInCategory(
			List<? extends CategorizationCriteria> groupingRules, 
			final List<IndentedTopLevelItem> categorizedItems, 
			TopLevelItem item) 
	{
		boolean grouped = false;
		for (CategorizationCriteria groupingRule : groupingRules) {
			if (groupingRule.groupNameGivenItem(item)!=null) {
				addItemToAppropriateGroup(categorizedItems, item, groupingRule);
				grouped = true;
			}
		}
		return grouped;
	}

	public void addItemToAppropriateGroup(
			final List<IndentedTopLevelItem> categorizedItems,
			TopLevelItem item, CategorizationCriteria groupingRule) 
	{
		final String groupName = groupingRule.groupNameGivenItem(item);
		GroupTopLevelItem groupTopLevelItem = getGroupForItemOrCreateIfNeeded(categorizedItems, groupName);
		groupTopLevelItem.add(item);
	}

	private List<TopLevelItem> flattenList(final List<IndentedTopLevelItem> groupedItems) 
	{
		final ArrayList<TopLevelItem> res = new ArrayList<TopLevelItem>();
		itemsData = new LinkedHashMap<String, IndentedTopLevelItem>();
		Collections.sort(groupedItems, comparator);
		for (IndentedTopLevelItem item : groupedItems) {
			final String groupLabel = item.getName();
			addNestedItemsAsIndentedItemsInTheResult(res, item,	groupLabel);
		}
		
		return res;
	}

	private void addNestedItemsAsIndentedItemsInTheResult(final ArrayList<TopLevelItem> res, IndentedTopLevelItem item, final String groupLabel) {
		if (item.target != null)
			res.add(item.target);
		else
			res.add(item);
		
		itemsData.put(item.getName(), item);
	}
	
	final Map<String, GroupTopLevelItem> groupItemByGroupName = new HashMap<String, GroupTopLevelItem>();
	private GroupTopLevelItem getGroupForItemOrCreateIfNeeded(
			final List<IndentedTopLevelItem> groupedItems,
			final String groupName) 
	{
		boolean groupIsMissing = !groupItemByGroupName.containsKey(groupName);
		if (groupIsMissing) {
			GroupTopLevelItem value = new GroupTopLevelItem(groupName);
			groupItems.add(value);
			groupItemByGroupName.put(groupName, value);
			groupedItems.add(groupItemByGroupName.get(groupName));
		}
		return groupItemByGroupName.get(groupName);
	}

	public String getGroupClassFor(TopLevelItem item) {
		return itemsData.get(item.getName()).getGroupClass();
	}

	public List<GroupTopLevelItem> getGroupItems() {
		return groupItems;
	}
}
