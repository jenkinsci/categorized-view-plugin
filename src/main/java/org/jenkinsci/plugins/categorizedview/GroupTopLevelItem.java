package org.jenkinsci.plugins.categorizedview;

import hudson.model.BallColor;
import hudson.model.HealthReport;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.search.Search;
import hudson.search.SearchIndex;
import hudson.security.ACL;
import hudson.security.Permission;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import org.springframework.security.access.AccessDeniedException;

/**
 * Compatibility adapter for category rows. Categories have no persisted configuration,
 * descriptor, build action, or page of their own; only supported header columns receive them.
 */
@SuppressWarnings("rawtypes")
public class GroupTopLevelItem implements TopLevelItem {
    private final String groupName;
    private final String displayName;
    // The view's owner, so relative names of the header row resolve against the view's folder.
    private final ItemGroup<? extends Item> parent;

    private int nestLevel;
    private final String groupClass;
    // Keep the field for subclasses compiled against earlier releases. Use add() to invalidate cached summaries.
    protected List<TopLevelItem> nestedItems = new ArrayList<>();
    private final CategoryGroup group;

    public GroupTopLevelItem(final String groupLabel, final String regexToIgnoreOnColorComputing) {
        this(groupLabel, groupLabel, regexToIgnoreOnColorComputing);
    }

    public GroupTopLevelItem(
            final String groupLabel, final String displayName, final String regexToIgnoreOnColorComputing) {
        this(groupLabel, displayName, CategoryGroup.compileIgnorePattern(regexToIgnoreOnColorComputing), null);
    }

    GroupTopLevelItem(
            final String groupLabel,
            final String displayName,
            final Pattern ignorePattern,
            final ItemGroup<? extends Item> parent) {
        groupName = groupLabel;
        this.displayName = displayName;
        this.parent = parent;
        this.group = new CategoryGroup(ignorePattern, this);
        this.nestLevel = 0;
        this.groupClass = "g_" + groupLabel.replaceAll("[^a-zA-Z0-9_]", "_") + groupLabel.hashCode();
        this.specificCss.append("font-weight:bold;");
    }

    CategoryGroup getGroup() {
        return group;
    }

    @Override
    public String getName() {
        return groupName;
    }

    @Override
    public String getFullName() {
        return groupName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getFullDisplayName() {
        return groupName;
    }

    public void add(final TopLevelItem item) {
        group.add(item);
    }

    @Override
    public void onLoad(final ItemGroup<? extends Item> parent, final String name) throws IOException {}

    @Override
    public void onCopiedFrom(final Item src) {}

    @Override
    public void onCreatedFromScratch() {}

    @Override
    public void save() throws IOException {}

    @Override
    public void delete() throws IOException, InterruptedException {}

    @Override
    public void checkPermission(final Permission permission) throws AccessDeniedException {}

    public BallColor getIconColor() {
        return group.getIconColor();
    }

    public Run getLastBuild() {
        return getLastBuildOfType(GetBuild.LAST);
    }

    public Run getLastSuccessfulBuild() {
        return getLastBuildOfType(GetBuild.SUCCESSFUL);
    }

    public Run getLastStableBuild() {
        return getLastBuildOfType(GetBuild.STABLE);
    }

    public Run getLastFailedBuild() {
        return getLastBuildOfType(GetBuild.FAILED);
    }

    public Run getLastUnsuccessfulBuild() {
        return getLastBuildOfType(GetBuild.UNSUCCESSFUL);
    }

    public Run getLastBuildOfType(final GetBuild getBuild) {
        return group.getLastBuildOfType(getBuild);
    }

    static interface GetBuild {
        GetBuild LAST = Job::getLastBuild;
        GetBuild SUCCESSFUL = Job::getLastSuccessfulBuild;
        GetBuild STABLE = Job::getLastStableBuild;
        GetBuild FAILED = Job::getLastFailedBuild;
        GetBuild UNSUCCESSFUL = Job::getLastUnsuccessfulBuild;

        public Run getFrom(Job project);
    }

    public BallColor chooseNextColor(BallColor res, final BallColor iconColor) {
        return CategoryGroup.chooseNextColor(res, iconColor);
    }

    @Override
    public String getUrl() {
        return "";
    }

    @Override
    public String getShortUrl() {
        return "";
    }

    @Override
    @Deprecated
    public String getAbsoluteUrl() {
        return null;
    }

    @Override
    public File getRootDir() {
        return null;
    }

    @Override
    public Search getSearch() {
        return null;
    }

    @Override
    public String getSearchName() {
        return "";
    }

    @Override
    public String getSearchUrl() {
        return "";
    }

    @Override
    public SearchIndex getSearchIndex() {
        return null;
    }

    @Override
    public ACL getACL() {
        return Jenkins.get().getAuthorizationStrategy().getRootACL();
    }

    @Override
    public boolean hasPermission(final Permission permission) {
        return true;
    }

    @Override
    public ItemGroup<? extends Item> getParent() {
        return parent != null ? parent : Jenkins.get();
    }

    @Override
    public TopLevelItemDescriptor getDescriptor() {
        return null;
    }

    public HealthReport getBuildHealth() {
        return group.getBuildHealth();
    }

    public List<HealthReport> getBuildHealthReports() {
        return Collections.singletonList(getBuildHealth());
    }

    public boolean isBuildable() {
        return false;
    }

    @Override
    public String getRelativeNameFrom(final ItemGroup g) {
        return getName();
    }

    @Override
    public String getRelativeNameFrom(final Item item) {
        return getName();
    }

    @Override
    public Collection<? extends Job> getAllJobs() {
        return Collections.emptyList();
    }

    public List<TopLevelItem> getGroupItems() {
        return getNestedItems();
    }

    public int getNestLevel() {
        return nestLevel;
    }

    public boolean hasLink() {
        return false;
    }

    public String getGroupClass() {
        return groupClass;
    }

    public String getCss() {
        StringBuilder builder = getBasicCss();
        return builder.toString();
    }

    private StringBuilder getBasicCss() {
        StringBuilder builder = new StringBuilder();
        builder.append(specificCss.toString());
        return builder;
    }

    StringBuilder specificCss = new StringBuilder();

    public List<TopLevelItem> getNestedItems() {
        return group.getNestedItems();
    }
}
