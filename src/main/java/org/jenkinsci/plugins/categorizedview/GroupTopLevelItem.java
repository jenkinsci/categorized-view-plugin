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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import jenkins.model.Jenkins;
import org.springframework.security.access.AccessDeniedException;

@SuppressWarnings("rawtypes")
public class GroupTopLevelItem implements TopLevelItem {
    private static final Logger LOGGER = Logger.getLogger(GroupTopLevelItem.class.getName());
    private final String groupName;
    private final String displayName;

    private int nestLevel;
    private final String groupClass;
    private GroupTopLevelItem parentGroup;
    protected List<TopLevelItem> nestedItems = new ArrayList<>();

    private final Pattern ignorePattern;
    private List<TopLevelItem> sortedItems;
    private BallColor iconColor;
    private HealthReport buildHealth;
    private final Map<GetBuild, Run> lastBuilds = new HashMap<>();

    public GroupTopLevelItem(final String groupLabel, final String regexToIgnoreOnColorComputing) {
        this(groupLabel, groupLabel, regexToIgnoreOnColorComputing);
    }

    public GroupTopLevelItem(
            final String groupLabel, final String displayName, final String regexToIgnoreOnColorComputing) {
        this(groupLabel, displayName, compileIgnorePattern(regexToIgnoreOnColorComputing));
    }

    GroupTopLevelItem(final String groupLabel, final String displayName, final Pattern ignorePattern) {
        groupName = groupLabel;
        this.displayName = displayName;
        this.ignorePattern = ignorePattern;
        this.nestLevel = 0;
        this.groupClass = "g_" + groupLabel.replaceAll("[^a-zA-Z0-9_]", "_") + groupLabel.hashCode();
        this.specificCss.append("font-weight:bold;");
    }

    static Pattern compileIgnorePattern(final String regex) {
        try {
            if (regex != null && !regex.isEmpty()) {
                return Pattern.compile(regex);
            }
        } catch (PatternSyntaxException e) {
            LOGGER.log(Level.WARNING, "Invalid aggregate-ignore regex; no jobs will be excluded: " + regex, e);
        }
        return null;
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
        nestedItems.add(item);
        if (item instanceof GroupTopLevelItem group) {
            group.parentGroup = this;
        }
        invalidateAggregates();
    }

    private void invalidateAggregates() {
        sortedItems = null;
        iconColor = null;
        buildHealth = null;
        lastBuilds.clear();
        if (parentGroup != null) {
            parentGroup.invalidateAggregates();
        }
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
        if (iconColor != null) {
            return iconColor;
        }
        BallColor colorState = BallColor.NOTBUILT;
        for (TopLevelItem item : getAggregationItems()) {
            if (item instanceof Job) {
                if (ignorePattern != null
                        && ignorePattern.matcher(item.getName()).matches()) {
                    continue;
                }
                BallColor projectColorState = ((Job) item).getIconColor();
                colorState = chooseNextColor(colorState, projectColorState);
            }
        }
        iconColor = colorState;
        return iconColor;
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
        if (lastBuilds.containsKey(getBuild)) {
            return lastBuilds.get(getBuild);
        }
        Run lastBuild = null;
        for (TopLevelItem item : getAggregationItems()) {
            if (item instanceof Job) {
                Run build = getBuild.getFrom((Job) item);
                if (lastBuild == null) {
                    lastBuild = build;
                }
                if (build == null) {
                    continue;
                }

                if (build.getTimestamp()
                        .getTime()
                        .after(lastBuild.getTimestamp().getTime())) {
                    lastBuild = build;
                }
            }
        }
        // A missing build is also a result; computeIfAbsent would repeat that lookup.
        lastBuilds.put(getBuild, lastBuild);
        return lastBuild;
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
        switch (res) {
            case ABORTED:
            case ABORTED_ANIME:
                switch (iconColor) {
                    case YELLOW:
                    case YELLOW_ANIME:
                    case RED:
                    case RED_ANIME:
                        res = iconColor;
                        break;
                    default:
                        break;
                }
                break;
            case BLUE:
            case BLUE_ANIME:
                switch (iconColor) {
                    case ABORTED:
                    case ABORTED_ANIME:
                    case YELLOW:
                    case YELLOW_ANIME:
                    case RED:
                    case RED_ANIME:
                        res = iconColor;
                        break;
                    default:
                        break;
                }
                break;
            case YELLOW:
            case YELLOW_ANIME:
                switch (iconColor) {
                    case RED:
                    case RED_ANIME:
                        res = iconColor;
                        break;
                    default:
                        break;
                }
                break;
            case RED:
            case RED_ANIME:
                break;
            default:
                res = iconColor;
        }
        return res;
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
    public Jenkins getParent() {
        return Jenkins.get();
    }

    @Override
    public TopLevelItemDescriptor getDescriptor() {
        return null;
    }

    public HealthReport getBuildHealth() {
        if (buildHealth != null) {
            return buildHealth;
        }
        HealthReport lowest = new HealthReport();
        lowest.setScore(100);
        for (TopLevelItem e : getAggregationItems()) {
            if (e instanceof Job) {
                HealthReport buildHealth = ((Job) e).getBuildHealth();
                if (buildHealth.getScore() < lowest.getScore()) {
                    lowest = buildHealth;
                }
            }
        }
        buildHealth = lowest;
        return buildHealth;
    }

    public List<HealthReport> getBuildHealthReports() {
        return null;
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
        if (sortedItems == null) {
            List<TopLevelItem> items = new ArrayList<>(nestedItems);
            final Comparator<TopLevelItem> comparator = new TopLevelItemComparator();
            items.sort(comparator);
            sortedItems = List.copyOf(items);
        }
        return sortedItems;
    }

    private List<TopLevelItem> getAggregationItems() {
        List<TopLevelItem> items = new ArrayList<>();
        for (TopLevelItem item : getNestedItems()) {
            if (item instanceof GroupTopLevelItem group) {
                items.addAll(group.getAggregationItems());
            } else {
                items.add(item);
            }
        }
        return items;
    }
}
