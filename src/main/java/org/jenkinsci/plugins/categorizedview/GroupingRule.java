package org.jenkinsci.plugins.categorizedview;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.TopLevelItem;
import hudson.util.FormValidation;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;

public class GroupingRule extends CategorizationCriteria {
    private static final Logger LOGGER = Logger.getLogger(GroupingRule.class.getName());
    private static final Pattern DISABLED_PATTERN = Pattern.compile("(?!)");
    private transient Pattern compiledPattern;
    private final String groupRegex;
    private final String namingRule;
    private boolean useDisplayName = false;
    private boolean useFullName = false;

    @DataBoundConstructor
    public GroupingRule(String groupRegex, String namingRule, boolean useDisplayName, boolean useFullName) {
        this.groupRegex = groupRegex;
        this.namingRule = namingRule;
        this.useDisplayName = useDisplayName;
        this.useFullName = useFullName;
    }

    public GroupingRule(String groupRegex, String namingRule, boolean useDisplayName) {
        this.groupRegex = groupRegex;
        this.namingRule = namingRule;
        this.useDisplayName = useDisplayName;
        this.useFullName = false;
    }

    public GroupingRule(String groupRegex, String namingRule) {
        this.groupRegex = groupRegex;
        this.namingRule = namingRule;
        this.useDisplayName = false;
        this.useFullName = false;
    }

    @Override
    public String groupNameGivenItem(TopLevelItem item) {
        Matcher matcher = getCompiledPattern().matcher(getItemName(item));
        return matcher.matches() ? matcher.replaceAll(effectiveNamingRule(namingRule)) : null;
    }

    private synchronized Pattern getCompiledPattern() {
        if (compiledPattern == null) {
            try {
                compiledPattern = compileRule(groupRegex, namingRule);
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                // Invalid legacy rules are disabled so they cannot break view rendering.
                compiledPattern = DISABLED_PATTERN;
                LOGGER.log(
                        Level.WARNING,
                        "Disabling invalid categorization rule: regex=" + groupRegex + ", namingRule=" + namingRule,
                        e);
            }
        }
        return compiledPattern;
    }

    private static String effectiveNamingRule(String namingRule) {
        return namingRule == null || namingRule.isEmpty() ? "$1" : namingRule;
    }

    private static Pattern compileRule(String groupRegex, String namingRule) {
        if (groupRegex == null || groupRegex.isEmpty()) {
            return DISABLED_PATTERN;
        }
        Pattern pattern = Pattern.compile(Utils.normalizeRegex(groupRegex));
        // Keep a successful empty match while switching to the rule's capture groups.
        // This lets Java validate replacement syntax and references without a sample job name.
        Matcher validation = Pattern.compile("").matcher("");
        validation.matches();
        validation.usePattern(pattern);
        validation.appendReplacement(new StringBuilder(), effectiveNamingRule(namingRule));
        return pattern;
    }

    private String getItemName(TopLevelItem item) {
        // useFullName takes precedence over useDisplayName when both are enabled, since the
        // full (slash-separated) name is needed to disambiguate items with the same short name
        // living in different folders / multibranch pipelines.
        if (useFullName) {
            return item.getFullName();
        }

        if (!useDisplayName) {
            return item.getName();
        }

        if (item.getDisplayName() == null) {
            return item.getName();
        }

        return item.getDisplayName();
    }

    String getNormalizedGroupRegex() {
        return Utils.normalizeRegex(getGroupRegex());
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<CategorizationCriteria> {
        @Override
        public CategorizationCriteria newInstance(StaplerRequest2 req, JSONObject formData) throws FormException {
            try {
                compileRule(formData.optString("groupRegex"), formData.optString("namingRule"));
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                throw new FormException("Invalid categorization regex or naming rule.", e, "groupRegex");
            }
            return super.newInstance(req, formData);
        }

        public FormValidation doCheckGroupRegex(@QueryParameter String value, @QueryParameter String namingRule) {
            return validateRule(value, namingRule);
        }

        public FormValidation doCheckNamingRule(@QueryParameter String value, @QueryParameter String groupRegex) {
            return validateRule(groupRegex, value);
        }

        private FormValidation validateRule(String groupRegex, String namingRule) {
            try {
                compileRule(groupRegex, namingRule);
                return FormValidation.ok();
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                return FormValidation.error(e.getMessage());
            }
        }

        @Override
        public String getDisplayName() {
            return "Regex Grouping Rule";
        }
    }

    public String getGroupRegex() {
        return groupRegex;
    }

    public String getNamingRule() {
        return namingRule;
    }

    public boolean getUseDisplayName() {
        return useDisplayName;
    }

    public boolean getUseFullName() {
        return useFullName;
    }
}
