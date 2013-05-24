package org.jenkinsci.plugins.categorizedview;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public class GroupingRule implements Describable<GroupingRule>{
	private final String groupRegex;
	private final String namingRule;
	
	@DataBoundConstructor
	public GroupingRule(String groupRegex, String namingRule) {
		this.groupRegex = groupRegex;
		this.namingRule = namingRule;
	}
	
	public String getGroupRegex() {
		return groupRegex;
	}
	
	public String getNamingRule() {
		return namingRule;
	}
	
	public String getNormalizedGroupRegex() {
		return normalizeRegex(groupRegex);
	}
	
	public Descriptor<GroupingRule> getDescriptor() {
		return Jenkins.getInstance().getDescriptorOrDie(getClass());
	}
	
	@Extension
	public static final class DescriptorImpl extends Descriptor<GroupingRule> {

		@Override
		public String getDisplayName() {
			return "Grouping Rule";
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

}
