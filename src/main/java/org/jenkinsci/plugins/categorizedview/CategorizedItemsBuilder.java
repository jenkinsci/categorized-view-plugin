package org.jenkinsci.plugins.categorizedview;

import hudson.model.TopLevelItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategorizedItemsBuilder {
    final Comparator<TopLevelItem> comparator = new TopLevelItemComparator();
    private List<TopLevelItem> itemsToCategorize;
    private List<GroupTopLevelItem> groupItems = new ArrayList<>();
    private List<? extends CategorizationCriteria> groupingRules;
    private Map<String, TopLevelItem> itemsData;
    private String regexToIgnoreOnColorComputing;

    public CategorizedItemsBuilder(
            final List<TopLevelItem> itemsToCategorize, final List<? extends CategorizationCriteria> groupingRules) {
        this(itemsToCategorize, groupingRules, "");
    }

    public CategorizedItemsBuilder(
            final List<TopLevelItem> items,
            final List<? extends CategorizationCriteria> groupingRules,
            final String regexToIgnoreOnColorComputing) {
        this.itemsToCategorize = items;
        this.groupingRules = groupingRules;
        this.regexToIgnoreOnColorComputing = regexToIgnoreOnColorComputing;
    }

    public List<TopLevelItem> getRegroupedItems() {
        return buildRegroupedItems();
    }

    private List<TopLevelItem> buildRegroupedItems() {
        return flattenList(buildCategorizedList());
    }

    private List<TopLevelItem> buildCategorizedList() {
        final List<TopLevelItem> categorizedItems = new ArrayList<>();
        if (groupingRules.size() == 0) {
            categorizedItems.addAll(itemsToCategorize);
            return categorizedItems;
        }

        for (TopLevelItem item : itemsToCategorize) {
            boolean categorized = tryToFitItemInCategory(categorizedItems, item);
            if (!categorized) {
                categorizedItems.add(item);
            }
        }
        return categorizedItems;
    }

    private boolean tryToFitItemInCategory(final List<TopLevelItem> categorizedItems, final TopLevelItem item) {
        boolean grouped = false;
        for (CategorizationCriteria groupingRule : groupingRules) {
            String groupNameGivenItem = groupingRule.groupNameGivenItem(item);
            if (groupNameGivenItem != null) {
                addItemToAppropriateGroup(groupNameGivenItem, categorizedItems, item);
                grouped = true;
            }
        }
        return grouped;
    }

    public void addItemToAppropriateGroup(
            final String groupName, final List<TopLevelItem> categorizedItems, final TopLevelItem item) {
        GroupTopLevelItem groupTopLevelItem = getGroupForItemOrCreateIfNeeded(categorizedItems, groupName);
        groupTopLevelItem.add(item);
    }

    private List<TopLevelItem> flattenList(final List<TopLevelItem> groupedItems) {
        final ArrayList<TopLevelItem> res = new ArrayList<>();
        itemsData = new LinkedHashMap<>();
        groupedItems.sort(comparator);
        for (TopLevelItem item : groupedItems) {
            addNestedItemsAsIndentedItemsInTheResult(res, item);
        }

        return res;
    }

    private void addNestedItemsAsIndentedItemsInTheResult(final ArrayList<TopLevelItem> res, final TopLevelItem item) {
        res.add(item);
        itemsData.put(item.getName(), item);
    }

    final Map<String, GroupTopLevelItem> groupItemByGroupName = new HashMap<>();

    private GroupTopLevelItem getGroupForItemOrCreateIfNeeded(
            final List<TopLevelItem> groupedItems, final String groupName) {
        boolean groupIsMissing = !groupItemByGroupName.containsKey(groupName);
        if (groupIsMissing) {
            GroupTopLevelItem value = new GroupTopLevelItem(groupName, regexToIgnoreOnColorComputing);
            groupItems.add(value);
            groupItemByGroupName.put(groupName, value);
            groupedItems.add(groupItemByGroupName.get(groupName));
        }
        return groupItemByGroupName.get(groupName);
    }

    public String getGroupClassFor(final TopLevelItem item) {
        if (item instanceof GroupTopLevelItem) {
            return ((GroupTopLevelItem) item).getGroupClass();
        }
        return "";
    }

    public List<GroupTopLevelItem> getGroupItems() {
        return groupItems;
    }
}
