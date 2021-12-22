package org.jenkinsci.plugins.categorizedview;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.ListView;
import hudson.model.TopLevelItem;
import hudson.model.ViewDescriptor;
import hudson.model.ViewGroup;
import hudson.model.Descriptor.FormException;
import hudson.util.DescribableList;
import hudson.util.FormValidation;

public class CategorizedJobsView extends ListView {
	private List<GroupingRule> groupingRules = new ArrayList<>();
	private String regexToIgnoreOnColorComputing = "";

	private DescribableList<CategorizationCriteria, Descriptor<CategorizationCriteria>> categorizationCriteria;

	private transient CategorizedItemsBuilder categorizedItemsBuilder;

	@DataBoundConstructor
	public CategorizedJobsView(final String name) {
		super(name);
		if (categorizationCriteria == null) {
			categorizationCriteria = new DescribableList<>(this);
		}
		migrateOldFormat();
	}

	public CategorizedJobsView(final String name, final ViewGroup owner) {
		super(name, owner);
	}

	@Override
	protected Object readResolve() {
		super.readResolve();
		try {
			Field field = ListView.class.getDeclaredField("jobNames");
			field.setAccessible(true);
			Object jobNames = field.get(this);
			if (jobNames == null) {
				field.set(this, new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this;
	}

	public List<TopLevelItem> getGroupedItems() {
		if (categorizationCriteria == null) {
			categorizedItemsBuilder = new CategorizedItemsBuilder(super.getItems(), groupingRules, getRegexToIgnoreOnColorComputing());
		} else {
			categorizedItemsBuilder = new CategorizedItemsBuilder(super.getItems(), categorizationCriteria.toList(),
					getRegexToIgnoreOnColorComputing());
		}

		return categorizedItemsBuilder.getRegroupedItems();
	}

	public String getRegexToIgnoreOnColorComputing() {
		if (regexToIgnoreOnColorComputing == null) {
			return "";
		}
		return regexToIgnoreOnColorComputing;
	}

	public void migrateOldFormat() {
		if (categorizationCriteria != null) {
			return;
		}

		if (groupingRules == null || groupingRules.size() == 0) {
			categorizationCriteria = new DescribableList<>(this);
		} else {
			categorizationCriteria = new DescribableList<>(this, groupingRules);
			groupingRules.clear();
		}
	}

	@Override
	protected void submit(final StaplerRequest req) throws ServletException, FormException, IOException {
		forcefullyDisableRecurseBecauseItCausesClassCastExceptionOnJenkins1_532_1(req);
		super.submit(req);
		categorizationCriteria.rebuildHetero(req, req.getSubmittedForm(), CategorizationCriteria.all(), "categorizationCriteria");
		regexToIgnoreOnColorComputing = req.getParameter("regexToIgnoreOnColorComputing");
	}

	public void forcefullyDisableRecurseBecauseItCausesClassCastExceptionOnJenkins1_532_1(final StaplerRequest req) {
		req.setAttribute("recurse", false);
	}

	public DescribableList<CategorizationCriteria, Descriptor<CategorizationCriteria>> getCategorizationCriteria() {
		migrateOldFormat();
		return categorizationCriteria;
	}

	public String getGroupClassFor(final TopLevelItem item) {
		return categorizedItemsBuilder.getGroupClassFor(item);
	}

	public boolean hasLink(final TopLevelItem item) {
		return item.getUrl() != null;
	}

	public boolean isGroupTopLevelItem(final TopLevelItem item) {
		return item instanceof GroupTopLevelItem;
	}

	@Extension
	public static final class DescriptorImpl extends ViewDescriptor {
		@Override
		public String getDisplayName() {
			return "Categorized Jobs View";
		}

		public FormValidation doCheckIncludeRegex(@QueryParameter final String value) {
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

	@Override
	protected void initColumns() {
		try {
			Field field = ListView.class.getDeclaredField("columns");
			field.setAccessible(true);
			Object columns = field.get(this);
			if (columns == null) {
				field.set(this,
						new DescribableList<>(
								this, CategorizedJobsListViewColumn.createDefaultCategorizedInitialColumnList()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
