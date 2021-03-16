package org.jenkinsci.plugins.categorizedview;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.acegisecurity.AccessDeniedException;

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
import jenkins.model.Jenkins;

@SuppressWarnings("rawtypes")
public class GroupTopLevelItem implements TopLevelItem {
	private final String groupName;

	private int nestLevel;
	private final String groupClass;
	protected List<TopLevelItem> nestedItems = new ArrayList<>();

	private String regexToIgnoreOnColorComputing;

	public GroupTopLevelItem(final String groupLabel, final String regexToIgnoreOnColorComputing) {
		groupName = groupLabel;
		this.regexToIgnoreOnColorComputing = regexToIgnoreOnColorComputing;
		this.nestLevel = 0;
		this.groupClass = "g_" + groupLabel.replaceAll("[^a-zA-Z0-9_]", "_") + groupLabel.hashCode();
		this.specificCss.append("font-weight:bold;");
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
		return groupName;
	}

	@Override
	public String getFullDisplayName() {
		return groupName;
	}

	public void add(final TopLevelItem item) {
		nestedItems.add(item);
	}

	@Override
	public void onLoad(final ItemGroup<? extends Item> parent, final String name) throws IOException {
	}

	@Override
	public void onCopiedFrom(final Item src) {
	}

	@Override
	public void onCreatedFromScratch() {
	}

	@Override
	public void save() throws IOException {
	}

	@Override
	public void delete() throws IOException, InterruptedException {
	}

	@Override
	public void checkPermission(final Permission permission) throws AccessDeniedException {
	}

	public BallColor getIconColor() {
		BallColor colorState = BallColor.NOTBUILT;
		for (TopLevelItem item : getNestedItems()) {
			if (item instanceof Job) {
				if (item.getName().matches(regexToIgnoreOnColorComputing)) {
					continue;
				}
				BallColor projectColorState = ((Job) item).getIconColor();
				colorState = chooseNextColor(colorState, projectColorState);
			}
		}
		return colorState;
	}

	public Run getLastBuild() {
		return getLastBuildOfType(new GetBuild() {
			@Override
			public Run getFrom(final Job project) {
				return project.getLastBuild();
			}
		});
	}

	public Run getLastSuccessfulBuild() {
		return getLastBuildOfType(new GetBuild() {
			@Override
			public Run getFrom(final Job project) {
				return project.getLastSuccessfulBuild();
			}
		});
	}

	public Run getLastStableBuild() {
		return getLastBuildOfType(new GetBuild() {
			@Override
			public Run getFrom(final Job project) {
				return project.getLastStableBuild();
			}
		});
	}

	public Run getLastFailedBuild() {
		return getLastBuildOfType(new GetBuild() {
			@Override
			public Run getFrom(final Job project) {
				return project.getLastFailedBuild();
			}
		});
	}

	public Run getLastUnsuccessfulBuild() {
		return getLastBuildOfType(new GetBuild() {
			@Override
			public Run getFrom(final Job project) {
				return project.getLastUnsuccessfulBuild();
			}
		});
	}

	public Run getLastBuildOfType(final GetBuild getBuild) {
		Run lastBuild = null;
		for (TopLevelItem item : getNestedItems()) {
			if (item instanceof Job) {
				Run build = getBuild.getFrom((Job) item);
				if (lastBuild == null) {
					lastBuild = build;
				}
				if (build == null) {
					continue;
				}

				if (build.getTimestamp().getTime().after(lastBuild.getTimestamp().getTime())) {
					lastBuild = build;
				}
			}
		}
		return lastBuild;
	}

	static interface GetBuild {
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
		HealthReport lowest = new HealthReport();
		lowest.setScore(100);
		for (TopLevelItem e : getNestedItems()) {
			if (e instanceof Job) {
				HealthReport buildHealth = ((Job) e).getBuildHealth();
				if (buildHealth.getScore() < lowest.getScore()) {
					lowest = buildHealth;
				}
			}
		}
		return lowest;
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
		return Collections.EMPTY_LIST;
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
		final Comparator<TopLevelItem> comparator = new TopLevelItemComparator();
		Collections.sort(nestedItems, comparator);
		return nestedItems;
	}
}
