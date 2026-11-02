package org.jenkinsci.plugins.categorizedview;

import hudson.model.TopLevelItem;
import java.io.Serializable;
import java.util.Comparator;

final class TopLevelItemComparator implements Comparator<TopLevelItem>, Serializable {
    private static final long serialVersionUID = 4675871668915456895L;

    @Override
    public int compare(final TopLevelItem o1, final TopLevelItem o2) {
        String name1 = o1 instanceof GroupTopLevelItem ? o1.getDisplayName() : o1.getName();
        String name2 = o2 instanceof GroupTopLevelItem ? o2.getDisplayName() : o2.getName();
        return name1.compareToIgnoreCase(name2);
    }
}
