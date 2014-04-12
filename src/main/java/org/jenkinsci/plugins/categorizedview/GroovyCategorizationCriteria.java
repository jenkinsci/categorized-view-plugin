package org.jenkinsci.plugins.categorizedview;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.Extension;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor;

import org.kohsuke.stapler.DataBoundConstructor;

public class GroovyCategorizationCriteria extends CategorizationCriteria {
	
	private final String groovyScript;

	@DataBoundConstructor
	public GroovyCategorizationCriteria(String groovyScript) 
	{
		this.groovyScript =groovyScript;
	}

	@Override
	public String groupNameGivenItem(TopLevelItem item) {
		Binding binding = new Binding();
		binding.setVariable("jobToCategorize", item);
		GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), binding);

		String x= "import jenkins.model.*;\n"+groovyScript;
		return (String) shell.evaluate(x);
	}

	public String getGroovyScript() {
		return groovyScript;
	}
	
	
	@Extension
	public static final class DescriptorImpl extends Descriptor<CategorizationCriteria> {
		@Override
		public String getDisplayName() {
			return "Groovy Grouping Criteria";
		}
	}
}
