package org.jenkinsci.plugins.categorizedview;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.ListView;
import hudson.model.TopLevelItem;
import hudson.model.ViewDescriptor;
import hudson.model.ViewGroup;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;

public class CategorizedJobsView extends ListView {
    private List<GroupingRule> groupingRules = new ArrayList<>();
    private String regexToIgnoreOnColorComputing = "";

    private DescribableList<CategorizationCriteria, Descriptor<CategorizationCriteria>> categorizationCriteria;

    private transient CategorizedItemsBuilder categorizedItemsBuilder;

    @DataBoundConstructor
    @SuppressFBWarnings(
            value = {"MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", "UR_UNINIT_READ"},
            justification = "let me just make the 'init' build pass")
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

    public List<TopLevelItem> getGroupedItems() {
        if (categorizationCriteria == null) {
            categorizedItemsBuilder =
                    new CategorizedItemsBuilder(super.getItems(), groupingRules, getRegexToIgnoreOnColorComputing());
        } else {
            categorizedItemsBuilder = new CategorizedItemsBuilder(
                    super.getItems(), categorizationCriteria.toList(), getRegexToIgnoreOnColorComputing());
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
    protected void submit(final StaplerRequest2 req) throws ServletException, FormException, IOException {
        super.submit(req);
        categorizationCriteria.rebuildHetero(
                req, req.getSubmittedForm(), CategorizationCriteria.all(), "categorizationCriteria");
        regexToIgnoreOnColorComputing = req.getParameter("regexToIgnoreOnColorComputing");
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
                field.set(
                        this,
                        new DescribableList<>(
                                this, CategorizedJobsListViewColumn.createDefaultCategorizedInitialColumnList()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
