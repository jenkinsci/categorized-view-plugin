package org.jenkinsci.plugins.categorizedview;

import hudson.Extension;
import hudson.Util;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.ListView;
import hudson.model.ViewDescriptor;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import hudson.views.ListViewColumn;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class CategorizedJobsView extends ListView {
	private List<GroupingRule> groupingRules = new ArrayList<GroupingRule>();
	private transient CategorizedItemsBuilder categorizedItemsBuilder;
	
	@DataBoundConstructor
	public CategorizedJobsView(String name) {
		super(name);
	}
	
	@Override
	public List<TopLevelItem> getItems() {
		categorizedItemsBuilder = new CategorizedItemsBuilder(super.getItems(), groupingRules);
		return categorizedItemsBuilder.getRegroupedItems();
	}
	
	@Override
	protected void submit(StaplerRequest req) throws ServletException, FormException, IOException {
		super.submit(req);
		groupingRules = req.bindJSONToList(GroupingRule.class, req.getSubmittedForm().get("groupingRules"));
	}
    
    public List<GroupingRule> getGroupingRules() {
        return groupingRules;
    }
    
    public String getCssFor(TopLevelItem item) {
    	return categorizedItemsBuilder.getCssFor(item);
    }
    
    public String getGroupClassFor(TopLevelItem item) {
    	return categorizedItemsBuilder.getGrouClassFor(item);
    }
    
    public boolean hasLink(TopLevelItem item) {
    	return item.getShortUrl() != null;
    }
	
	@Extension
	public static final class DescriptorImpl extends ViewDescriptor {
		public String getDisplayName() {
			return "Categorized Jobs View";
		}
		
		public FormValidation doCheckIncludeRegex(@QueryParameter String value)
				throws IOException, ServletException, InterruptedException {
			String v = Util.fixEmpty(value);
			if (v != null) {
				try {
					Pattern.compile(v);
				} catch (PatternSyntaxException pse) {
					return FormValidation.error(pse.getMessage());
				}
			}
			return FormValidation.ok();
		}
	}
	
	protected void initColumns() {
		try {
			Field field = ListView.class.getDeclaredField("columns");
			field.setAccessible(true);
			field.set(
					this,
					new DescribableList<ListViewColumn, Descriptor<ListViewColumn>>(
							this, CategorizedJobsListViewColumn.createDefaultCategorizedInitialColumnList()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
