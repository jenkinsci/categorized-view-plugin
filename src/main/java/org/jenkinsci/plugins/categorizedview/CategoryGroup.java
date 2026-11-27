package org.jenkinsci.plugins.categorizedview;

import hudson.model.BallColor;
import hudson.model.HealthReport;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.jenkinsci.plugins.categorizedview.GroupTopLevelItem.GetBuild;

/** Group membership and live summaries for a category row. */
@SuppressWarnings("rawtypes")
final class CategoryGroup {
    private static final Logger LOGGER = Logger.getLogger(CategoryGroup.class.getName());
    private final GroupTopLevelItem adapter;
    private final Pattern ignorePattern;
    private CategoryGroup parent;
    private List<TopLevelItem> sortedItems;
    private BallColor iconColor;
    private HealthReport buildHealth;
    private final Map<GetBuild, Run> lastBuilds = new HashMap<>();

    CategoryGroup(Pattern ignorePattern, GroupTopLevelItem adapter) {
        this.ignorePattern = ignorePattern;
        this.adapter = adapter;
    }

    void add(TopLevelItem item) {
        adapter.nestedItems.add(item);
        if (item instanceof GroupTopLevelItem child) {
            child.getGroup().parent = this;
        }
        invalidateAggregates();
    }

    private void invalidateAggregates() {
        sortedItems = null;
        iconColor = null;
        buildHealth = null;
        lastBuilds.clear();
        if (parent != null) {
            parent.invalidateAggregates();
        }
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
                colorState = adapter.chooseNextColor(colorState, projectColorState);
            }
        }
        iconColor = colorState;
        return iconColor;
    }

    Run getLastBuildOfType(final GetBuild getBuild) {
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

    static BallColor chooseNextColor(BallColor res, final BallColor iconColor) {
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

    List<TopLevelItem> getNestedItems() {
        if (sortedItems == null) {
            List<TopLevelItem> items = new ArrayList<>(adapter.nestedItems);
            items.sort(new TopLevelItemComparator());
            sortedItems = List.copyOf(items);
        }
        return sortedItems;
    }

    private List<TopLevelItem> getAggregationItems() {
        List<TopLevelItem> items = new ArrayList<>();
        for (TopLevelItem item : adapter.getNestedItems()) {
            if (item instanceof GroupTopLevelItem group) {
                items.addAll(group.getGroup().getAggregationItems());
            } else {
                items.add(item);
            }
        }
        return items;
    }
}
