package org.jenkinsci.plugins.categorizedview;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;

public class IndentedJobColumn extends ListViewColumn {
	@DataBoundConstructor
	public IndentedJobColumn() {
		//
	}

	@Extension
	public static class DescriptorImpl extends ListViewColumnDescriptor {
		@Override
		public String getDisplayName() {
			return Messages.IndentedJobColumn_DisplayName();
		}

		@Override
		public boolean shownByDefault() {
			return false;
		}
	}
}
