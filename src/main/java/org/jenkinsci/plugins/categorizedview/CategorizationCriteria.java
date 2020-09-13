package org.jenkinsci.plugins.categorizedview;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.TopLevelItem;
import jenkins.model.Jenkins;

public abstract class CategorizationCriteria implements Describable<CategorizationCriteria>, ExtensionPoint {

	/**
	 * @param item This is the item that will be passed to categorization
	 * @return returns the group name, or null if the item can't be categorized
	 */
	public abstract String groupNameGivenItem(TopLevelItem item);

	public static DescriptorExtensionList<CategorizationCriteria, Descriptor<CategorizationCriteria>> all() {
		return Jenkins.get().<CategorizationCriteria, Descriptor<CategorizationCriteria>> getDescriptorList(CategorizationCriteria.class);
	}

	@Override
	public Descriptor<CategorizationCriteria> getDescriptor() {
		return Jenkins.get().getDescriptorOrDie(getClass());
	}
}