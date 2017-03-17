package org.jenkinsci.plugins.categorizedview;

import hudson.Extension;
import hudson.Util;
import hudson.model.TopLevelItem;
import hudson.model.ViewGroup;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.ListView;
import hudson.model.ViewDescriptor;
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
	private String regexToIgnoreOnColorComputing = "";

	private DescribableList<CategorizationCriteria, Descriptor<CategorizationCriteria>> categorizationCriteria;
	
	private transient CategorizedItemsBuilder categorizedItemsBuilder;
	
	@DataBoundConstructor
	public CategorizedJobsView(String name) {
		super(name);
		if (categorizationCriteria == null)
			categorizationCriteria = new DescribableList<CategorizationCriteria, Descriptor<CategorizationCriteria>>(this);
		migrateOldFormat();
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
	
	public List<TopLevelItem> getGroupedItems() {
		if (categorizationCriteria == null) 
			categorizedItemsBuilder = new CategorizedItemsBuilder(super.getItems(), groupingRules, getRegexToIgnoreOnColorComputing());
		else
			categorizedItemsBuilder = new CategorizedItemsBuilder(super.getItems(), categorizationCriteria.toList(), getRegexToIgnoreOnColorComputing());
		
		return categorizedItemsBuilder.getRegroupedItems();
	}
	
	public String getRegexToIgnoreOnColorComputing() {
		if (regexToIgnoreOnColorComputing == null) 
			return "";
		return regexToIgnoreOnColorComputing;
	}

	public void migrateOldFormat() {
		if (categorizationCriteria !=null)
			return;
		
		if (groupingRules == null || groupingRules.size() == 0)
			categorizationCriteria = new DescribableList<CategorizationCriteria, Descriptor<CategorizationCriteria>>(this);
		else {
			categorizationCriteria = new DescribableList<CategorizationCriteria, Descriptor<CategorizationCriteria>>(this, groupingRules);
			groupingRules.clear();
		}
	}
	
	@Override
	protected void submit(StaplerRequest req) throws ServletException, FormException, IOException {
		forcefullyDisableRecurseBecauseItCausesClassCastExceptionOnJenkins1_532_1(req);
		super.submit(req);
		categorizationCriteria.rebuildHetero(req, req.getSubmittedForm(), CategorizationCriteria.all(), "categorizationCriteria");
		regexToIgnoreOnColorComputing = req.getParameter("regexToIgnoreOnColorComputing");
	}

	public void forcefullyDisableRecurseBecauseItCausesClassCastExceptionOnJenkins1_532_1( StaplerRequest req) {
		req.setAttribute("recurse", false);
	}
    
    public DescribableList<CategorizationCriteria, Descriptor<CategorizationCriteria>> getCategorizationCriteria() {
    	migrateOldFormat();
		return categorizationCriteria;
	}
    
    public String getGroupClassFor(TopLevelItem item) {
    	return categorizedItemsBuilder.getGroupClassFor(item);
    }
    
    public boolean hasLink(TopLevelItem item) {
    	return item.getShortUrl() != null;
    }
    
    public boolean isGroupTopLevelItem(TopLevelItem item) {
    	return item instanceof GroupTopLevelItem;
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
			Object columns = field.get(this);
			if (columns == null) {
				field.set(this,
						new DescribableList<ListViewColumn, Descriptor<ListViewColumn>>(
								this, CategorizedJobsListViewColumn.createDefaultCategorizedInitialColumnList()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
