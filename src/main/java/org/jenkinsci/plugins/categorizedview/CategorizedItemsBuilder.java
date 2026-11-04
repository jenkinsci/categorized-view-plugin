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
        for (int ruleIndex = 0; ruleIndex < groupingRules.size(); ruleIndex++) {
            String groupNameGivenItem = groupingRules.get(ruleIndex).groupNameGivenItem(item);
            if (groupNameGivenItem != null) {
                addItemToAppropriateGroup(groupNameGivenItem, categorizedItems, item, ruleIndex);
                grouped = true;
            }
        }
        return grouped;
    }

    private void addItemToAppropriateGroup(
            final String groupName,
            final List<TopLevelItem> categorizedItems,
            final TopLevelItem item,
            final int ruleIndex) {
        GroupTopLevelItem groupTopLevelItem = getGroupForItemOrCreateIfNeeded(categorizedItems, groupName, ruleIndex);
        groupTopLevelItem.add(item);
    }

    private List<TopLevelItem> flattenList(final List<TopLevelItem> groupedItems) {
        final ArrayList<TopLevelItem> res = new ArrayList<>();
        itemsData = new LinkedHashMap<>();
        List<GroupTopLevelItem> groups = new ArrayList<>();
        List<TopLevelItem> ungroupedItems = new ArrayList<>();
        for (TopLevelItem item : groupedItems) {
            if (item instanceof GroupTopLevelItem) {
                groups.add((GroupTopLevelItem) item);
            } else {
                ungroupedItems.add(item);
            }
        }
        groups.sort(Comparator.comparing(
                        (GroupTopLevelItem group) -> groupRuleIndex.getOrDefault(group.getName(), Integer.MAX_VALUE))
                .thenComparing(group -> group.getName(), String.CASE_INSENSITIVE_ORDER));
        ungroupedItems.sort(comparator);
        for (TopLevelItem group : groups) {
            addNestedItemsAsIndentedItemsInTheResult(res, group);
        }
        for (TopLevelItem item : ungroupedItems) {
            addNestedItemsAsIndentedItemsInTheResult(res, item);
        }

        return res;
    }

    private void addNestedItemsAsIndentedItemsInTheResult(final ArrayList<TopLevelItem> res, final TopLevelItem item) {
        res.add(item);
        itemsData.put(item.getName(), item);
    }

    final Map<String, GroupTopLevelItem> groupItemByGroupName = new HashMap<>();
    private final Map<String, Integer> groupRuleIndex = new HashMap<>();

    private GroupTopLevelItem getGroupForItemOrCreateIfNeeded(
            final List<TopLevelItem> groupedItems, final String groupName, final int ruleIndex) {
        boolean groupIsMissing = !groupItemByGroupName.containsKey(groupName);
        if (groupIsMissing) {
            GroupTopLevelItem value = new GroupTopLevelItem(groupName, regexToIgnoreOnColorComputing);
            groupItems.add(value);
            groupItemByGroupName.put(groupName, value);
            groupedItems.add(groupItemByGroupName.get(groupName));
        }
        // Several rules can produce the same category name; its position is that of the
        // earliest matching rule, regardless of which job was processed first.
        groupRuleIndex.merge(groupName, ruleIndex, Math::min);
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
