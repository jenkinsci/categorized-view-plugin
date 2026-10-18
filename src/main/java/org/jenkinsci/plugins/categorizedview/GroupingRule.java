package org.jenkinsci.plugins.categorizedview;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.TopLevelItem;
import org.kohsuke.stapler.DataBoundConstructor;

public class GroupingRule extends CategorizationCriteria {
    private final String groupRegex;
    private final String namingRule;
    private boolean useDisplayName = false;
    private boolean useFullName = false;

    @DataBoundConstructor
    public GroupingRule(String groupRegex, String namingRule, boolean useDisplayName, boolean useFullName) {
        this.groupRegex = groupRegex;
        this.namingRule = namingRule;
        this.useDisplayName = useDisplayName;
        this.useFullName = useFullName;
    }

    public GroupingRule(String groupRegex, String namingRule, boolean useDisplayName) {
        this.groupRegex = groupRegex;
        this.namingRule = namingRule;
        this.useDisplayName = useDisplayName;
        this.useFullName = false;
    }

    public GroupingRule(String groupRegex, String namingRule) {
        this.groupRegex = groupRegex;
        this.namingRule = namingRule;
        this.useDisplayName = false;
        this.useFullName = false;
    }

    @Override
    public String groupNameGivenItem(TopLevelItem item) {
        if (!isOnGroup(item)) {
            return null;
        }

        final String groupNamingRule = namingRule == null || namingRule.isEmpty() ? "$1" : namingRule;
        return getItemName(item).replaceAll(getNormalizedGroupRegex(), groupNamingRule);
    }

    private boolean isOnGroup(TopLevelItem item) {
        if (groupRegex == null || groupRegex.isEmpty()) {
            return false;
        }

        return getItemName(item).matches(getNormalizedGroupRegex());
    }

    private String getItemName(TopLevelItem item) {
        // useFullName takes precedence over useDisplayName when both are enabled, since the
        // full (slash-separated) name is needed to disambiguate items with the same short name
        // living in different folders / multibranch pipelines.
        if (useFullName) {
            return item.getFullName();
        }

        if (!useDisplayName) {
            return item.getName();
        }

        if (item.getDisplayName() == null) {
            return item.getName();
        }

        return item.getDisplayName();
    }

    String getNormalizedGroupRegex() {
        return Utils.normalizeRegex(getGroupRegex());
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<CategorizationCriteria> {
        @Override
        public String getDisplayName() {
            return "Regex Grouping Rule";
        }
    }

    public String getGroupRegex() {
        return groupRegex;
    }

    public String getNamingRule() {
        return namingRule;
    }

    public boolean getUseDisplayName() {
        return useDisplayName;
    }

    public boolean getUseFullName() {
        return useFullName;
    }
}
