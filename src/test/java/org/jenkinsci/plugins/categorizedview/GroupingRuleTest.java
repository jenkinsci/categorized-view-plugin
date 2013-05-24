package org.jenkinsci.plugins.categorizedview;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GroupingRuleTest {
	@Test
	public void regexShouldNotBeNormalizedIfNotNeeded() {
		GroupingRule subject = new GroupingRule(".*(s).*","");
		assertEquals(".*(s).*", subject.getNormalizedGroupRegex());
	}
	
	@Test
	public void regexWithoutLeadingAndEndingAsterisk_ShoulAddThem() {
		GroupingRule subject = new GroupingRule("(.*s)","");
		assertEquals(".*(.*s).*", subject.getNormalizedGroupRegex());
	}
}