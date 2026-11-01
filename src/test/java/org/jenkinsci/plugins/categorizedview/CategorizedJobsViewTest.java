package org.jenkinsci.plugins.categorizedview;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.views.JobColumn;
import hudson.views.StatusColumn;
import hudson.views.WeatherColumn;
import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlForm;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CategorizedJobsViewTest {

    @Test
    void isHideAggregatedInfoOnCategories_shouldDefaultToFalse(JenkinsRule r) {
        assertFalse(new CategorizedJobsView("Categories").isHideAggregatedInfoOnCategories());
    }

    @Test
    void isColumnVisibleOnCategoryHeader_whenNotHidden_shouldShowAllColumns(JenkinsRule r) {
        CategorizedJobsView subject = new CategorizedJobsView("Categories");

        assertTrue(subject.isColumnVisibleOnCategoryHeader(new IndentedJobColumn()));
        assertTrue(subject.isColumnVisibleOnCategoryHeader(new JobColumn()));
        assertTrue(subject.isColumnVisibleOnCategoryHeader(new StatusColumn()));
        assertTrue(subject.isColumnVisibleOnCategoryHeader(new WeatherColumn()));
    }

    @Test
    void isColumnVisibleOnCategoryHeader_whenHidden_shouldOnlyShowJobColumns(JenkinsRule r) throws Exception {
        CategorizedJobsView subject = new CategorizedJobsView("Categories");
        r.jenkins.addView(subject);

        HtmlForm form = r.createWebClient().goTo("view/Categories/configure").getFormByName("viewConfig");
        HtmlCheckBoxInput hideAggregatedInfo = form.getInputByName("_.hideAggregatedInfoOnCategories");
        hideAggregatedInfo.setChecked(true);
        r.submit(form);

        CategorizedJobsView configured = (CategorizedJobsView) r.jenkins.getView("Categories");
        assertTrue(configured.isHideAggregatedInfoOnCategories());
        assertTrue(configured.isColumnVisibleOnCategoryHeader(new IndentedJobColumn()));
        assertTrue(configured.isColumnVisibleOnCategoryHeader(new JobColumn()));
        assertFalse(configured.isColumnVisibleOnCategoryHeader(new StatusColumn()));
        assertFalse(configured.isColumnVisibleOnCategoryHeader(new WeatherColumn()));
    }
}
