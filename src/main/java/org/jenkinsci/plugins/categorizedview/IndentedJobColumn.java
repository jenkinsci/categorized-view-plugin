package org.jenkinsci.plugins.categorizedview;

import hudson.Extension;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

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
