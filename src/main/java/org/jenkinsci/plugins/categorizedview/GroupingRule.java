package org.jenkinsci.plugins.categorizedview;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.TopLevelItem;

public class GroupingRule extends CategorizationCriteria
{
	private final String groupRegex;
	private final String namingRule;
	private boolean useDisplayName = false;
	
	@DataBoundConstructor
	public GroupingRule(String groupRegex, String namingRule, boolean useDisplayName) {
		this.groupRegex = groupRegex;
		this.namingRule = namingRule;
		this.useDisplayName = useDisplayName;
	}
	
	public GroupingRule(String groupRegex, String namingRule) {
		this.groupRegex = groupRegex;
		this.namingRule = namingRule;
		this.useDisplayName = false;
	}

	@Override
	public String groupNameGivenItem(TopLevelItem item) {
		if (!isOnGroup(item))
			return null;
		
		final String groupNamingRule = StringUtils.isEmpty(getNamingRule())?"$1":getNamingRule();
		return getItemName(item).replaceAll(getNormalizedGroupRegex(), groupNamingRule);
	}
	private boolean isOnGroup(TopLevelItem item) {
		if (StringUtils.isEmpty(getGroupRegex())) 
			return false;
		
		return getItemName(item).matches(getNormalizedGroupRegex()); 
	}
	
	private String getItemName(TopLevelItem item) {
		if (!useDisplayName)
			return item.getName();
		
		if (item.getDisplayName() == null)
			return item.getName();
		
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
}