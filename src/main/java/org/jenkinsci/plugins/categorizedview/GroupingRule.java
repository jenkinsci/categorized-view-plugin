package org.jenkinsci.plugins.categorizedview;

import hudson.Extension;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class GroupingRule extends CategorizationCriteria
{
	private final String groupRegex;
	private final String namingRule;
	
	@DataBoundConstructor
	public GroupingRule(String groupRegex, String namingRule) {
		this.groupRegex = groupRegex;
		this.namingRule = namingRule;
	}

	@Override
	public String groupNameGivenItem(TopLevelItem item) {
		if (!isOnGroup(item))
			return null;
		
		final String groupNamingRule = StringUtils.isEmpty(getNamingRule())?"$1":getNamingRule();
		return item.getName().replaceAll(getNormalizedGroupRegex(), groupNamingRule);
	}
	
	private boolean isOnGroup(TopLevelItem item) {
		if (StringUtils.isEmpty(getGroupRegex())) 
			return false;
		
		return item.getName().matches(getNormalizedGroupRegex()); 
	}

	String getNormalizedGroupRegex() {
		return normalizeRegex(getGroupRegex());
	}
	
	@Extension
	public static final class DescriptorImpl extends Descriptor<CategorizationCriteria> {
		@Override
		public String getDisplayName() {
			return "Regex Grouping Rule";
		}
	}
	
	private static String normalizeRegex(String groupRegex) {
		if (groupRegex == null) return "";
		String regex = groupRegex;
		if (!regex.startsWith(".*"))
			regex =".*"+regex;
		if (!regex.endsWith(".*"))
			regex +=".*";
		if (!regex.contains("(")) {
			regex = ".*("+groupRegex+").*";
		}
		return regex;
	}

	public String getGroupRegex() {
		return groupRegex;
	}

	public String getNamingRule() {
		return namingRule;
	}
}