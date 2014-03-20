package org.jenkinsci.plugins.categorizedview;

import hudson.Extension;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class GroupingRule extends CategorizedViewGroupingRule
{
	private final String groupRegex;
	private final String namingRule;
	
	@DataBoundConstructor
	public GroupingRule(String groupRegex, String namingRule) {
		this.groupRegex = groupRegex;
		this.namingRule = namingRule;
	}
	
	String getNormalizedGroupRegex() {
		return normalizeRegex(groupRegex);
	}
	
	@SuppressWarnings("unchecked")
	public Descriptor<CategorizedViewGroupingRule> getDescriptor() {
		return Jenkins.getInstance().getDescriptorOrDie(getClass());
	}
	
	@Extension
	public static final class DescriptorImpl extends Descriptor<CategorizedViewGroupingRule> {
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

	public boolean accepts(TopLevelItem item) {
		if (StringUtils.isEmpty(groupRegex)) 
			return false;
		
		return item.getName().matches(getNormalizedGroupRegex()); 
	}

	public String groupNameGivenItem(TopLevelItem item) {
		final String groupNamingRule = StringUtils.isEmpty(namingRule)?"$1":namingRule;
		return item.getName().replaceAll(getNormalizedGroupRegex(), groupNamingRule);
	}
}