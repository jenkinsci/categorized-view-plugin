package org.jenkinsci.plugins.categorizedview;

import java.util.Comparator;

final class TopLevelItemComparator implements
		Comparator<IndentedTopLevelItem> {
	public int compare(IndentedTopLevelItem o1, IndentedTopLevelItem o2) {
		return o1.target.getName().compareToIgnoreCase(o2.target.getName());
	}
}