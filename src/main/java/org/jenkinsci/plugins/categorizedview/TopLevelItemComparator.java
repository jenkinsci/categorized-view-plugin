package org.jenkinsci.plugins.categorizedview;

import hudson.model.TopLevelItem;

import java.util.Comparator;

final class TopLevelItemComparator implements
		Comparator<TopLevelItem> {
	public int compare(TopLevelItem o1, TopLevelItem o2) {
		return o1.getName().compareToIgnoreCase(o2.getName());
	}
}