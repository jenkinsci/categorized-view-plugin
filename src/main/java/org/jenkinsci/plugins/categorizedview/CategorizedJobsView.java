package org.jenkinsci.plugins.categorizedview;

import hudson.Extension;
import hudson.Util;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.ListView;
import hudson.model.ViewDescriptor;
import hudson.model.ViewGroup;
import hudson.util.CaseInsensitiveComparator;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import hudson.views.ListViewColumn;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
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

	public CategorizedJobsView(String name, ViewGroup owner) {
		super(name, owner);
	}

	private Object readResolve() {
		try {
			Method readResolve = ListView.class.getDeclaredMethod("readResolve");
			readResolve.setAccessible(true);
			readResolve.invoke(this);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		try {
			Field field = ListView.class.getDeclaredField("jobNames");
			field.setAccessible(true);
			Object jobNames = field.get(this);
			if(jobNames==null)
			{
				field.set( this,  new TreeSet<String>(CaseInsensitiveComparator.INSTANCE));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return this;
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
		return categorizedItemsBuilder.getGroupClassFor(item);
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
			if(field.get(this)==null){
				field.set(
					this,
					new DescribableList<ListViewColumn, Descriptor<ListViewColumn>>(
							this, CategorizedJobsListViewColumn.createDefaultCategorizedInitialColumnList()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
