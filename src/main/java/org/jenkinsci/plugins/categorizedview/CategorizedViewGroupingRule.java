package org.jenkinsci.plugins.categorizedview;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public abstract class CategorizedViewGroupingRule implements Describable<CategorizedViewGroupingRule>, ExtensionPoint {

	public abstract boolean accepts(TopLevelItem item);

	public abstract String groupNameGivenItem(TopLevelItem item);
	
    public static DescriptorExtensionList<CategorizedViewGroupingRule, Descriptor<CategorizedViewGroupingRule>> all() {
        return Jenkins.getInstance().<CategorizedViewGroupingRule, Descriptor<CategorizedViewGroupingRule>>getDescriptorList(CategorizedViewGroupingRule.class);
    }
	
	@SuppressWarnings("unchecked")
	public Descriptor<CategorizedViewGroupingRule> getDescriptor() {
		return Jenkins.getInstance().getDescriptorOrDie(getClass());
	}
}