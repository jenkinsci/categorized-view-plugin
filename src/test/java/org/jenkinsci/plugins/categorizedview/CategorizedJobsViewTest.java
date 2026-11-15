package org.jenkinsci.plugins.categorizedview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.views.JobColumn;
import hudson.views.StatusColumn;
import hudson.views.WeatherColumn;
import java.util.List;
import jenkins.model.Jenkins;
import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CategorizedJobsViewTest {

    @Test
    void getGroupedItems_shouldSupportLegacyRulesWithAndWithoutNesting(JenkinsRule r) throws Exception {
        var project = r.createFreeStyleProject("first");
        for (boolean nested : new boolean[] {false, true}) {
            CategorizedJobsView subject = (CategorizedJobsView) Jenkins.XSTREAM2.fromXML("""
                    <org.jenkinsci.plugins.categorizedview.CategorizedJobsView>
                      <name>Legacy-%s</name>
                      <jobNames><string>first</string></jobNames>
                      <groupingRules>
                        <org.jenkinsci.plugins.categorizedview.GroupingRule>
                          <groupRegex>first</groupRegex>
                          <namingRule>Parent/Child</namingRule>
                        </org.jenkinsci.plugins.categorizedview.GroupingRule>
                      </groupingRules>
                      <nestedGroups>%s</nestedGroups>
                    </org.jenkinsci.plugins.categorizedview.CategorizedJobsView>
                    """.formatted(nested, nested));
            r.jenkins.addView(subject);

            var groups = subject.getGroupedItems();
            assertEquals(1, groups.size());
            GroupTopLevelItem group = (GroupTopLevelItem) groups.get(0);
            assertEquals(nested ? "Parent" : "Parent/Child", group.getDisplayName());
            if (nested) {
                assertEquals(1, group.getNestedItems().size());
                group = (GroupTopLevelItem) group.getNestedItems().get(0);
                assertEquals("Child", group.getDisplayName());
            }
            assertEquals(List.of(project), group.getNestedItems());
        }
    }

    @Test
    void nestedGroups_shouldRoundTripConfigurationAndRenderSubgroups(JenkinsRule r) throws Exception {
        CategorizedJobsView subject = new CategorizedJobsView("Categories");
        assertFalse(subject.isNestedGroups());
        r.jenkins.addView(subject);
        subject.add(r.createFreeStyleProject("first"));
        subject.add(r.createFreeStyleProject("second"));
        subject.getCategorizationCriteria().add(new GroupingRule("first", "FootLocker/FLX User/Build"));
        subject.getCategorizationCriteria().add(new GroupingRule("second", "FootLocker/Non-FLX User"));

        HtmlForm form = r.createWebClient().goTo("view/Categories/configure").getFormByName("viewConfig");
        form.<HtmlCheckBoxInput>getInputByName("_.nestedGroups").setChecked(true);
        r.submit(form);
        assertTrue(subject.isNestedGroups());
        r.configRoundtrip(subject);
        assertTrue(subject.isNestedGroups());

        JenkinsRule.WebClient client = r.createWebClient().withJavaScriptEnabled(true);
        HtmlPage page = client.goTo("view/Categories/");
        assertEquals(4, page.getByXPath("//tr[@categoryRole='category']").size());
        assertEquals(
                1,
                page.getByXPath("//tr[@id='jobNestedItems_FootLocker']//tr[@id='jobNestedItems_FootLocker/FLX User']"
                                + "//tr[@id='category_FootLocker/FLX User/Build']")
                        .size());
        assertEquals(1, page.getByXPath("//a[contains(@href, '/job/first/')]").size());
        assertEquals(1, page.getByXPath("//a[contains(@href, '/job/second/')]").size());

        var parent = page.getHtmlElementById("category_FootLocker");
        var child = page.getHtmlElementById("category_FootLocker/FLX User");
        var parentContents = page.getHtmlElementById("jobNestedItems_FootLocker");
        var childContents = page.getHtmlElementById("jobNestedItems_FootLocker/FLX User");
        assertTrue(parentContents.getAttribute("style").contains("display: none"));
        assertTrue(childContents.getAttribute("style").contains("display: none"));
        parent.click();
        child.click();
        assertTrue(page.getHtmlElementById("category_FootLocker/FLX User/Build")
                .asNormalizedText()
                .contains("Build"));
        assertFalse(page.getHtmlElementById("category_FootLocker/FLX User/Build")
                .asNormalizedText()
                .contains("FootLocker/"));
        assertFalse(childContents.getAttribute("style").contains("display: none"));
        parent.click();
        parent.click();
        assertFalse(childContents.getAttribute("style").contains("display: none"));
        child.click();
        parent.click();
        parent.click();
        assertTrue(childContents.getAttribute("style").contains("display: none"));

        HtmlPage reloaded = client.goTo("view/Categories/");
        assertFalse(reloaded.getHtmlElementById("jobNestedItems_FootLocker")
                .getAttribute("style")
                .contains("display: none"));
        assertTrue(reloaded.getHtmlElementById("jobNestedItems_FootLocker/FLX User")
                .getAttribute("style")
                .contains("display: none"));
    }

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
