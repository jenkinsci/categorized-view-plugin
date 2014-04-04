package org.jenkinsci.plugins.categorizedview;

import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.Job;
import hudson.search.SearchIndex;
import hudson.search.Search;
import hudson.security.ACL;
import hudson.security.Permission;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.acegisecurity.AccessDeniedException;

public class IndentedTopLevelItem implements TopLevelItem {
	
	public TopLevelItem target;
	private int nestLevel;
	private final String groupClass;
	public IndentedTopLevelItem(TopLevelItem target, int nestLevel, String groupLabel, String css) {
		this.target = target;
		this.nestLevel = nestLevel;
		// make a unique (barely) readable css-classname that works with for all possible labels
		this.groupClass = "g_"+groupLabel.replaceAll("[^a-zA-Z0-9_]","_")+groupLabel.hashCode();		
		this.specificCss.append(css);
	}
	
	public IndentedTopLevelItem(TopLevelItem item) {
		this(item,0,"","");
	}

	public int getNestLevel() {
		return nestLevel;
	}
	
	public boolean hasLink() {
		if (target == null) return false;
		return target.getShortUrl() != null;
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
	
	public List<IndentedTopLevelItem> getNestedItems() {
		final Comparator<IndentedTopLevelItem> comparator = new TopLevelItemComparator();
		Collections.sort(nestedItems,comparator);
		return nestedItems;
	}
	
	protected List<IndentedTopLevelItem> nestedItems = new ArrayList<IndentedTopLevelItem>();
	public String getName() {
		return target.getName();
	}

	public ItemGroup<? extends Item> getParent() {
		return null;
	}

	public Collection<? extends Job> getAllJobs() {
		return null;
	}

	public String getFullName() {
		return null;
	}

	public String getDisplayName() {
		return null;
	}

	public String getFullDisplayName() {
		return null;
	}

	public String getRelativeNameFrom(ItemGroup g) {
		return null;
	}

	public String getRelativeNameFrom(Item item) {
		return null;
	}

	public String getUrl() {
		return null;
	}

	public String getShortUrl() {
		return null;
	}

	public String getAbsoluteUrl() {
		return null;
	}

	public void onLoad(ItemGroup<? extends Item> parent, String name)
			throws IOException {
	}

	public void onCopiedFrom(Item src) {
	}

	public void onCreatedFromScratch() {
	}

	public void save() throws IOException {
	}

	public void delete() throws IOException, InterruptedException {
	}

	public File getRootDir() {
		return null;
	}

	public Search getSearch() {
		return null;
	}

	public String getSearchName() {
		return null;
	}

	public String getSearchUrl() {
		return null;
	}

	public SearchIndex getSearchIndex() {
		return null;
	}

	public ACL getACL() {
		return null;
	}

	public void checkPermission(Permission permission)
			throws AccessDeniedException {
	}

	public boolean hasPermission(Permission permission) {
		return true;
	}

	public TopLevelItemDescriptor getDescriptor() {
		return null;
	}
}
