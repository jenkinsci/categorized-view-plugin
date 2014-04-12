package org.jenkinsci.plugins.categorizedview;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import hudson.model.FreeStyleProject;

import org.junit.Test;
import org.mockito.Mockito;


public class GroovyCategorizationCriteriaTest {
	@Test
	public void groovyScript_HappyDay() {
		String groovyGroup = ""
				+ " if (jobToCategorize.name.contains('ckos'))\n"
				+ "		return 'categorized';\n"
				+ " else"
				+ "		return null;\n";
		GroovyCategorizationCriteria criteria = new GroovyCategorizationCriteria(groovyGroup );
		assertEquals("categorized", criteria.groupNameGivenItem(makeProjectNamed("foo ckos")));
		assertEquals(null, criteria.groupNameGivenItem(makeProjectNamed("foo")));
	}
	
	private FreeStyleProject makeProjectNamed(String value) {
		FreeStyleProject mock = Mockito.mock(FreeStyleProject.class);
		when(mock.getName()).thenReturn(value);
		return mock;
	}
}