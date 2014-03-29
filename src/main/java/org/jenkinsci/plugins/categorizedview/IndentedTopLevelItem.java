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
		builder.append("padding-left:");
		builder.append(String.valueOf((getNestLevel() + 1) * 20));
		builder.append("px;");
		builder.append(specificCss.toString());
		return builder;
	}

	StringBuilder specificCss = new StringBuilder();
	
	public List<IndentedTopLevelItem> getNestedItems() {
		return nestedItems;
	}
	
	protected List<IndentedTopLevelItem> nestedItems = new ArrayList<IndentedTopLevelItem>();
	public String getName() {
		return target.getName();
	}

	public ItemGroup<? extends Item> getParent() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public Collection<? extends Job> getAllJobs() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public String getFullName() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public String getDisplayName() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public String getFullDisplayName() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public String getRelativeNameFrom(ItemGroup g) {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public String getRelativeNameFrom(Item item) {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public String getUrl() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public String getShortUrl() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public String getAbsoluteUrl() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public void onLoad(ItemGroup<? extends Item> parent, String name)
			throws IOException {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public void onCopiedFrom(Item src) {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public void onCreatedFromScratch() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public void save() throws IOException {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public void delete() throws IOException, InterruptedException {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public File getRootDir() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public Search getSearch() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public String getSearchName() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public String getSearchUrl() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public SearchIndex getSearchIndex() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public ACL getACL() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public void checkPermission(Permission permission)
			throws AccessDeniedException {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public boolean hasPermission(Permission permission) {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public TopLevelItemDescriptor getDescriptor() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

}
