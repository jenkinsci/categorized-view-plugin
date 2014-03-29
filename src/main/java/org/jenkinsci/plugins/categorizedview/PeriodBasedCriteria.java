package org.jenkinsci.plugins.categorizedview;

import hudson.Extension;
import hudson.model.TopLevelItem;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import net.sf.json.JSONObject;

import org.joda.time.DateTime;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class PeriodBasedCriteria extends CategorizationCriteria {
	
	private final DateTime startDate;
	private final DateTime finalDate;

	@DataBoundConstructor
	public PeriodBasedCriteria(DateTime start, DateTime end) {
		this.startDate = start;
		this.finalDate = end;
	}

	@Override
	public String groupNameGivenItem(TopLevelItem item) {
		if (!isOnGroup(item))
			return null;
		return "haha";
	}
	
	public DateTime getStartDate() {
		return startDate;
	}

	public DateTime getFinalDate() {
		return finalDate;
	}

	@Extension
	public static final class DescriptorImpl extends Descriptor<CategorizationCriteria> {
		@Override
		public String getDisplayName() {
			return "Build date period";
		}
		
		@Override
	    public PeriodBasedCriteria newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			DateTime startDate  = null;
			if (formData.getString("startDate") != null) {
				startDate = DateTime.parse(formData.getString("startDate"));
			}
			
			DateTime finalDate  = null;
			if (formData.getString("finalDate") != null) {
				startDate = DateTime.parse(formData.getString("finalDate"));
			}
			
			return new PeriodBasedCriteria(startDate, finalDate);
		}
	}
	
	private boolean isOnGroup(TopLevelItem item) {
		if (!(item instanceof AbstractProject))
			return false;
		AbstractProject job = (AbstractProject) item;
		DateTime lastBuildTime = new DateTime(job.getLastBuild().getTime());
		
		return false;
	}
}
