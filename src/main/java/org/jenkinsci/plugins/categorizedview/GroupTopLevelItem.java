package org.jenkinsci.plugins.categorizedview;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
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

import jenkins.model.Jenkins;

import org.acegisecurity.AccessDeniedException;
import org.joda.time.DateTime;

public class GroupTopLevelItem  extends IndentedTopLevelItem  {
	private final String groupName;
	
	public GroupTopLevelItem(String label) {
		super(null, 0, label, "font-weight:bold;");
		this.groupName = label;
	}

	public String getName() {
		return groupName;
	}

	public String getFullName() {
		return groupName;
	}

	public String getDisplayName() {
		return groupName;
	}

	public String getFullDisplayName() {
		return groupName;
	}

	public void add(TopLevelItem item) {
		IndentedTopLevelItem subItem = new IndentedTopLevelItem(item, 1, groupName, "");
		nestedItems.add(subItem);
	}

	public void onLoad(ItemGroup<? extends Item> parent, String name) throws IOException {
	}

	public void onCopiedFrom(Item src) {
	}

	public void onCreatedFromScratch() {
	}

	public void save() throws IOException {
	}

	public void delete() throws IOException, InterruptedException {
	}

	public void checkPermission(Permission permission) throws AccessDeniedException {
	}
	
	public BallColor getIconColor() {
		BallColor colorState = BallColor.NOTBUILT;
		for (IndentedTopLevelItem items : getNestedItems()) {
			if (items.target instanceof AbstractProject) {
				BallColor projectColorState = ((AbstractProject)items.target).getIconColor();
				colorState = chooseNextColor(colorState, projectColorState);
			}
		}
		return colorState;
	}
	
	public Run getLastBuild() {
		return getLastBuildOfType(new GetBuild() {
			public AbstractBuild getFrom(AbstractProject project) {
				return project.getLastBuild();
			}
		});
    }
	
	public Run getLastSuccessfulBuild() {
		return getLastBuildOfType(new GetBuild() {
			public AbstractBuild getFrom(AbstractProject project) {
				return (AbstractBuild) project.getLastSuccessfulBuild();
			}
		});
	}
	
	public Run getLastStableBuild() {
		return getLastBuildOfType(new GetBuild() {
			public AbstractBuild getFrom(AbstractProject project) {
				return (AbstractBuild) project.getLastStableBuild();
			}
		});
	}

	public Run getLastFailedBuild() {
		return getLastBuildOfType(new GetBuild() {
			public AbstractBuild getFrom(AbstractProject project) {
				return (AbstractBuild) project.getLastFailedBuild();
			}
		});
	}
	
	public Run getLastUnsuccessfulBuild() {
		return getLastBuildOfType(new GetBuild() {
			public AbstractBuild getFrom(AbstractProject project) {
				return (AbstractBuild) project.getLastUnsuccessfulBuild();
			}
		});
	}

	public Run getLastBuildOfType(GetBuild getBuild) {
		AbstractBuild lastBuild = null;
		for (IndentedTopLevelItem items : getNestedItems()) {
			if (items.target instanceof AbstractProject) {
				AbstractBuild build = getBuild.getFrom((AbstractProject)items.target);
				if (lastBuild == null)
					lastBuild = build;
				else {
					if (new DateTime(build.getTimestamp()).isAfter(new DateTime(lastBuild.getTimestamp()))) {
						lastBuild = build;
					}
				}
			}
		}
		return lastBuild;
	}
	
	static interface GetBuild {
		public AbstractBuild getFrom(AbstractProject project);
	}

	public BallColor chooseNextColor(BallColor res, BallColor iconColor) {
		switch(res) {
		case ABORTED: case ABORTED_ANIME:
			switch(iconColor) {
			case YELLOW: case YELLOW_ANIME:
			case RED: case RED_ANIME:
				res = iconColor;
				break;
			default:
			}
			break;
		case BLUE: case BLUE_ANIME:
			switch(iconColor) {
			case ABORTED: case ABORTED_ANIME:
			case YELLOW: case YELLOW_ANIME:
			case RED: case RED_ANIME:
				res = iconColor;
				break;
			default:
			}
			break;
		case YELLOW: case YELLOW_ANIME:
			switch(iconColor) {
			case RED: case RED_ANIME:
				res = iconColor;
				break;
			default:
			}
			break;
		case RED: case RED_ANIME:
			break;
		default:
			res = iconColor;
		}
		return res;
	}

	public String getUrl() {
		return "";
	}

	public String getShortUrl() {
		return "";
	}

	@Deprecated
	public String getAbsoluteUrl() {
		return null;
	}

	public File getRootDir() {
		return null;
	}

	public Search getSearch() {
		return null;
	}

	public String getSearchName() {
		return "";
	}

	public String getSearchUrl() {
		return "";
	}

	public SearchIndex getSearchIndex() {
		return null;
	}

	public ACL getACL() {
		return null;
	}

	public boolean hasPermission(Permission permission) {
		return true;
	}

	public Jenkins getParent() {
		return Jenkins.getInstance();
	}

	public TopLevelItemDescriptor getDescriptor() {
		return null;
	}

	public HealthReport getBuildHealth() {
		HealthReport lowest = new HealthReport();
		lowest.setScore(100);
		for (IndentedTopLevelItem e : getNestedItems()) {
			if (e.target instanceof AbstractProject) {
				HealthReport buildHealth = ((AbstractProject)e.target).getBuildHealth();
				if (buildHealth.getScore() < lowest.getScore())
					lowest = buildHealth;
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


	public String getRelativeNameFrom(ItemGroup g) {
		return getName();
	}

	public String getRelativeNameFrom(Item item) {
		return getName();
	}

	@SuppressWarnings("unchecked")
	public Collection<? extends Job> getAllJobs() {
		return Collections.EMPTY_LIST;
	}

	public List<TopLevelItem> getGroupItems() {
		List<IndentedTopLevelItem> nestedItems2 = getNestedItems();
		List<TopLevelItem> top = new ArrayList<TopLevelItem>();
		for (IndentedTopLevelItem topLevelItem : nestedItems2) {
			top.add(topLevelItem.target);
		}
		return top;
	}
}
