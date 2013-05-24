package org.jenkinsci.plugins.categorizedview;
import hudson.model.HealthReport;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.search.SearchIndex;
import hudson.search.Search;
import hudson.security.ACL;
import hudson.security.Permission;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.acegisecurity.AccessDeniedException;

public class GroupTopLevelItem  implements TopLevelItem {
	private final String labelText;
	
	public GroupTopLevelItem(String label) {
		this.labelText = label;
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

	public String getUrl() {
		return null;
	}

	public String getShortUrl() {
		return null;
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

	public Hudson getParent() {
		return Hudson.getInstance();
	}

	public TopLevelItemDescriptor getDescriptor() {
		return null;
	}

	public HealthReport getBuildHealth() {
		return null;
	}

	public List<HealthReport> getBuildHealthReports() {
		return null;
	}

	public boolean isBuildable() {
		return false;
	}


	public String getRelativeNameFrom(ItemGroup g) {
		return null;
	}

	public String getRelativeNameFrom(Item item) {
		return null;
	}

	@SuppressWarnings("unchecked")
	public Collection<? extends Job> getAllJobs() {
		return Collections.EMPTY_LIST;
	}

	public String getName() {
		return labelText;
	}

	public String getFullName() {
		return labelText;
	}

	public String getDisplayName() {
		return labelText;
	}

	public String getFullDisplayName() {
		return labelText;
	}

	public String getCss() {
		return "font-weight:bold;";
	}
}
