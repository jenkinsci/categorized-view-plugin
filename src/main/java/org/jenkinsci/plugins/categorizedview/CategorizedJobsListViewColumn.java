package org.jenkinsci.plugins.categorizedview;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.views.BuildButtonColumn;
import hudson.views.LastDurationColumn;
import hudson.views.LastFailureColumn;
import hudson.views.LastSuccessColumn;
import hudson.views.ListViewColumn;
import hudson.views.StatusColumn;
import hudson.views.WeatherColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract public class CategorizedJobsListViewColumn extends ListViewColumn {
	public static List<ListViewColumn> createDefaultCategorizedInitialColumnList() {
		ArrayList<ListViewColumn> r = new ArrayList<ListViewColumn>();
		DescriptorExtensionList<ListViewColumn, Descriptor<ListViewColumn>> all = ListViewColumn.all();

		for (Class<? extends ListViewColumn> d : CategorizedJobsListViewColumn.DEFAULT_CATEGORIZED_COLUMNS) {
			Descriptor<ListViewColumn> des = all.find(d);
			if (des != null) {
				try {
					r.add(des.newInstance(null, null));
				} catch (FormException e) {
					LOGGER.log(Level.WARNING, "Failed to instantiate " + des.clazz, e);
				}
			}
		}
		return r;
	}

	private static final List<Class<? extends ListViewColumn>> DEFAULT_CATEGORIZED_COLUMNS = Arrays.asList(
	        StatusColumn.class,
	        WeatherColumn.class,
	        IndentedJobColumn.class,
	        LastSuccessColumn.class,
	        LastFailureColumn.class,
	        LastDurationColumn.class,
	        BuildButtonColumn.class
	    );
	
	private static final Logger LOGGER = Logger.getLogger(CategorizedJobsListViewColumn.class.getName());
	
	
}
