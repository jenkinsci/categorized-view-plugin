package org.jenkinsci.plugins.categorizedview.mocks;

import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.AbstractItem;
import hudson.model.Job;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

@SuppressWarnings("unchecked")
public class TopLevelItemMock extends AbstractItem implements TopLevelItem {

	public TopLevelItemMock(String name) {
		super(getMockParent(), name);
	}

	private static ItemGroup<Item> getMockParent() {
		return new ItemGroup<Item>() {

			public File getRootDir() {
				throw new RuntimeException("NOT IMPLEMENTED");
			}

			public void save() throws IOException {
				throw new RuntimeException("NOT IMPLEMENTED");
			}

			public String getDisplayName() {
				return "";
			}

			public String getFullName() {
				return "";
			}

			public String getFullDisplayName() {
				return "";
			}

			public Collection<Item> getItems() {
				throw new RuntimeException("NOT IMPLEMENTED");
			}

			public String getUrl() {
				throw new RuntimeException("NOT IMPLEMENTED");
			}

			public String getUrlChildPrefix() {
				throw new RuntimeException("NOT IMPLEMENTED");
			}

			public Item getItem(final String name) {
				throw new RuntimeException("NOT IMPLEMENTED");
			}

			public File getRootDirFor(final Item child) {
				throw new RuntimeException("NOT IMPLEMENTED");
			}

			public void onRenamed(final Item item, final String oldName, final String newName)
					throws IOException {
				throw new RuntimeException("NOT IMPLEMENTED");
			}

			public void onDeleted(final Item item) throws IOException {
				throw new RuntimeException("NOT IMPLEMENTED");
			}
		};
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Collection<? extends Job> getAllJobs() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

	public TopLevelItemDescriptor getDescriptor() {
		throw new RuntimeException("NOT IMPLEMENTED");
	}

}
