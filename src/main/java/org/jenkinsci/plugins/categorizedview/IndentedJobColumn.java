package org.jenkinsci.plugins.categorizedview;

import hudson.Extension;
import hudson.views.ListViewColumnDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class IndentedJobColumn extends CategorizedJobsListViewColumn {
	@DataBoundConstructor
    public IndentedJobColumn() {
		//
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {
        @Override
        public String getDisplayName() {
        	return " Categorized - Job";
        }
        
        @Override
        public boolean shownByDefault() {
        	return false;
        }
    }
}
