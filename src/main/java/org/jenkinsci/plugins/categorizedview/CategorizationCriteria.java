package org.jenkinsci.plugins.categorizedview;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public abstract class CategorizationCriteria implements Describable<CategorizationCriteria>, ExtensionPoint {

	public abstract String groupNameGivenItem(TopLevelItem item);
	
    public static DescriptorExtensionList<CategorizationCriteria, Descriptor<CategorizationCriteria>> all() {
        return Jenkins.getInstance().<CategorizationCriteria, Descriptor<CategorizationCriteria>>getDescriptorList(CategorizationCriteria.class);
    }
	
	@SuppressWarnings("unchecked")
	public Descriptor<CategorizationCriteria> getDescriptor() {
		return Jenkins.getInstance().getDescriptorOrDie(getClass());
	}
}