package org.jenkinsci.plugins.categorizedview;

import hudson.model.TopLevelItem;

import java.util.ArrayList;
import java.util.List;

public class IndentedTopLevelItem {
	
	public final TopLevelItem target;
	private int nestLevel;
	private final String groupLabel;
	public IndentedTopLevelItem(TopLevelItem target, int nestLevel, String groupLabel, String css) {
		this.target = target;
		this.nestLevel = nestLevel;
		this.groupLabel = groupLabel;
		this.specificCss.append(css);
	}
	
	public IndentedTopLevelItem(TopLevelItem item) {
		this(item,0,"","");
	}

	public int getNestLevel() {
		return nestLevel;
	}
	
	public boolean hasLink() {
		return target.getShortUrl() != null;
	}
	
	public String getGroupClass() {
		return groupLabel.replace(".", "_").replace(" ","_");
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
	
	public void add(IndentedTopLevelItem item) {
		nestedItems.add(item);
	}
	
	public List<IndentedTopLevelItem> getNestedItems() {
		return nestedItems;
	}
	
	private List<IndentedTopLevelItem> nestedItems = new ArrayList<IndentedTopLevelItem>();
}
