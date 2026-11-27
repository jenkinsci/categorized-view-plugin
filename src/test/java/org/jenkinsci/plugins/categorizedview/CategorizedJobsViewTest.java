package org.jenkinsci.plugins.categorizedview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerColumn;
import hudson.model.BallColor;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.util.FormValidation;
import hudson.views.BuildButtonColumn;
import hudson.views.JobColumn;
import hudson.views.LastDurationColumn;
import hudson.views.LastFailureColumn;
import hudson.views.LastSuccessColumn;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import hudson.views.StatusColumn;
import hudson.views.WeatherColumn;
import java.util.List;
import java.util.Locale;
import jenkins.model.Jenkins;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlCheckBoxInput;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTableCell;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.localizer.LocaleProvider;

@WithJenkins
class CategorizedJobsViewTest {

    @Test
    void submittingInvalidExpressionsShouldLeaveConfigurationUnchanged(JenkinsRule r) throws Exception {
        CategorizedJobsView subject = new CategorizedJobsView("Validation");
        r.jenkins.addView(subject);
        subject.getCategorizationCriteria().add(new GroupingRule("first", "$1"));
        for (String[] invalid : new String[][] {
            {"regexToIgnoreOnColorComputing", "["}, {"groupRegex", "["}, {"namingRule", "${missing}"}
        }) {
            HtmlForm form =
                    r.createWebClient().goTo("view/Validation/configure").getFormByName("viewConfig");
            form.<HtmlCheckBoxInput>getInputByName("_.recurse").setChecked(true);
            form.getInputByName(invalid[0]).setValue(invalid[1]);
            var error = assertThrows(FailingHttpStatusCodeException.class, () -> r.submit(form));
            assertEquals(400, error.getStatusCode());
            assertFalse(subject.isRecurse());
        }
        assertEquals("", subject.getRegexToIgnoreOnColorComputing());
        assertEquals(
                "first", ((GroupingRule) subject.getCategorizationCriteria().get(0)).getGroupRegex());
    }

    @Test
    void aggregateIgnoreValidationShouldRejectMalformedRegex(JenkinsRule r) {
        var descriptor = r.jenkins.getDescriptorByType(CategorizedJobsView.DescriptorImpl.class);
        assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckRegexToIgnoreOnColorComputing("[").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckRegexToIgnoreOnColorComputing("").kind);
        assertEquals(FormValidation.Kind.OK, descriptor.doCheckRegexToIgnoreOnColorComputing(".*ignore.*").kind);
    }

    @Test
    void aggregateIgnorePatternShouldFollowConfigurationChanges(JenkinsRule r) throws Exception {
        CategorizedJobsView subject = new CategorizedJobsView("Ignore-pattern");
        r.jenkins.addView(subject);
        var project = r.createFreeStyleProject("job");
        r.buildAndAssertSuccess(project);
        subject.add(project);
        subject.getCategorizationCriteria().add(new GroupingRule("job", "Group"));

        for (String regex : new String[] {"job", "", "job"}) {
            HtmlForm form =
                    r.createWebClient().goTo("view/Ignore-pattern/configure").getFormByName("viewConfig");
            form.getInputByName("regexToIgnoreOnColorComputing").setValue(regex);
            r.submit(form);
            for (int render = 0; render < 2; render++) {
                GroupTopLevelItem group =
                        (GroupTopLevelItem) subject.getGroupedItems().get(0);
                assertEquals(regex.isEmpty() ? BallColor.BLUE : BallColor.NOTBUILT, group.getIconColor());
            }
        }
    }

    @Test
    void invalidLegacyConfigurationShouldStillRender(JenkinsRule r) throws Exception {
        CategorizedJobsView subject = (CategorizedJobsView) Jenkins.XSTREAM2.fromXML("""
            <org.jenkinsci.plugins.categorizedview.CategorizedJobsView>
              <name>Legacy-invalid</name>
              <jobNames><string>first</string></jobNames>
              <regexToIgnoreOnColorComputing>[</regexToIgnoreOnColorComputing>
              <groupingRules>
                <org.jenkinsci.plugins.categorizedview.GroupingRule>
                  <groupRegex>[</groupRegex><namingRule>$1</namingRule>
                </org.jenkinsci.plugins.categorizedview.GroupingRule>
                <org.jenkinsci.plugins.categorizedview.GroupingRule>
                  <groupRegex>first</groupRegex><namingRule>${missing}</namingRule>
                </org.jenkinsci.plugins.categorizedview.GroupingRule>
                <org.jenkinsci.plugins.categorizedview.GroupingRule>
                  <groupRegex>first</groupRegex><namingRule>Valid</namingRule>
                </org.jenkinsci.plugins.categorizedview.GroupingRule>
              </groupingRules>
            </org.jenkinsci.plugins.categorizedview.CategorizedJobsView>
            """);
        r.createFreeStyleProject("first");
        r.jenkins.addView(subject);
        assertEquals("Valid", subject.getGroupedItems().get(0).getDisplayName());
        r.createWebClient().goTo("view/Legacy-invalid/");
    }

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
        HtmlButton parentToggle = parent.getFirstByXPath("./td/button");
        assertEquals("button", parentToggle.getTypeAttribute());
        assertTrue(parentToggle.getAttribute("aria-label").contains("FootLocker"));
        assertEquals("false", parentToggle.getAttribute("aria-expanded"));
        assertTrue(parentContents.getAttribute("style").contains("display: none"));
        assertTrue(childContents.getAttribute("style").contains("display: none"));
        parentToggle.click();
        assertEquals("true", parentToggle.getAttribute("aria-expanded"));
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
        assertEquals(
                "true",
                reloaded.<HtmlButton>getFirstByXPath("//tr[@id='category_FootLocker']/td/button")
                        .getAttribute("aria-expanded"));
        assertEquals(
                "false",
                reloaded.<HtmlButton>getFirstByXPath("//tr[@id='category_FootLocker/FLX User']/td/button")
                        .getAttribute("aria-expanded"));
        assertFalse(reloaded.getHtmlElementById("jobNestedItems_FootLocker")
                .getAttribute("style")
                .contains("display: none"));
        assertTrue(reloaded.getHtmlElementById("jobNestedItems_FootLocker/FLX User")
                .getAttribute("style")
                .contains("display: none"));
    }

    @Test
    void getGroupedItems_shouldRefreshAggregatesForEachRender(JenkinsRule r) throws Exception {
        CategorizedJobsView view = new CategorizedJobsView("Categories");
        r.jenkins.addView(view);
        var project = r.createFreeStyleProject("job");
        view.add(project);
        view.getCategorizationCriteria().add(new GroupingRule("job", "Group"));

        GroupTopLevelItem before = (GroupTopLevelItem) view.getGroupedItems().get(0);
        assertEquals(BallColor.NOTBUILT, before.getIconColor());
        r.buildAndAssertSuccess(project);

        GroupTopLevelItem after = (GroupTopLevelItem) view.getGroupedItems().get(0);
        assertEquals(BallColor.BLUE, after.getIconColor());
        assertEquals(BallColor.NOTBUILT, before.getIconColor());
    }

    @Test
    void isHideAggregatedInfoOnCategories_shouldDefaultToFalse(JenkinsRule r) {
        assertFalse(new CategorizedJobsView("Categories").isHideAggregatedInfoOnCategories());
    }

    @Test
    void isColumnVisibleOnCategoryHeader_whenNotHidden_shouldShowSupportedColumns(JenkinsRule r) {
        CategorizedJobsView subject = new CategorizedJobsView("Categories");

        assertTrue(subject.isColumnVisibleOnCategoryHeader(new IndentedJobColumn()));
        assertTrue(subject.isColumnVisibleOnCategoryHeader(new JobColumn()));
        assertTrue(subject.isColumnVisibleOnCategoryHeader(new StatusColumn()));
        assertTrue(subject.isColumnVisibleOnCategoryHeader(new WeatherColumn()));
        assertTrue(subject.isColumnVisibleOnCategoryHeader(new LastSuccessColumn()));
        assertTrue(subject.isColumnVisibleOnCategoryHeader(new LastFailureColumn()));
        assertTrue(subject.isColumnVisibleOnCategoryHeader(new LastDurationColumn()));
        assertTrue(subject.isColumnVisibleOnCategoryHeader(new JobOwnerColumn()));
        assertFalse(subject.isColumnVisibleOnCategoryHeader(new BuildButtonColumn()));
        assertFalse(subject.isColumnVisibleOnCategoryHeader(new UnsupportedColumn()));
        assertFalse(subject.isColumnVisibleOnCategoryHeader(new StatusColumn() {}));
        assertFalse(subject.isWeatherColumnOnCategoryHeader(new WeatherColumn() {}));
        assertFalse(subject.isNameOnlyColumnOnCategoryHeader(new JobColumn() {}));
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

    @Test
    void groupHeader_inNestedFolder_shouldShowOnlyGroupName(JenkinsRule r) throws Exception {
        MockFolder folder = r.createFolder("TeamA").createProject(MockFolder.class, "ProjectX");
        CategorizedJobsView view = new CategorizedJobsView("my-view", folder);
        folder.addView(view);
        view.add(folder.createProject(FreeStyleProject.class, "app-build-backend"));
        view.getCategorizationCriteria().add(new GroupingRule("app-build-.*", "app-build"));
        view.getColumns().clear();
        view.getColumns().add(new IndentedJobColumn());
        view.getColumns().add(new JobColumn());

        HtmlPage page = r.createWebClient().goTo("job/TeamA/job/ProjectX/view/my-view/");
        String header = page.getHtmlElementById("category_app-build").asNormalizedText();
        assertTrue(header.contains("app-build"), header);
        assertFalse(header.contains(".."), header);
    }

    @Test
    void weatherOnCategoryHeaderShouldNotLinkToBuild(JenkinsRule r) throws Exception {
        CategorizedJobsView subject = new CategorizedJobsView("Weather-contract");
        r.jenkins.addView(subject);
        FreeStyleProject job = r.createFreeStyleProject("job");
        job.getBuildersList().add(new FailureBuilder());
        r.buildAndAssertStatus(Result.FAILURE, job);
        assertEquals(0, job.getBuildHealth().getScore());
        subject.add(job);
        subject.getCategorizationCriteria().add(new GroupingRule("job", "Parent/Child"));
        subject.getColumns().replaceBy(List.of(new JobColumn(), new WeatherColumn()));

        JenkinsRule.WebClient client = r.createWebClient();
        HtmlForm form = client.goTo("view/Weather-contract/configure").getFormByName("viewConfig");
        form.<HtmlCheckBoxInput>getInputByName("_.nestedGroups").setChecked(true);
        r.submit(form);

        HtmlPage page = client.goTo("view/Weather-contract/");
        for (String group : List.of("Parent", "Parent/Child")) {
            HtmlTableCell weather = page.getHtmlElementById("category_" + group)
                    .getFirstByXPath("./td[contains(@class,'healthReport')]");
            assertEquals("0", weather.getAttribute("data"));
        }
        assertEquals(
                0,
                page.getByXPath("//tr[@categoryRole='category']//a[contains(@href,'lastBuild')]")
                        .size());
    }

    @Test
    void weatherShouldRespectHiddenAggregatedInfo(JenkinsRule r) throws Exception {
        CategorizedJobsView subject = new CategorizedJobsView("Weather-hidden");
        r.jenkins.addView(subject);
        subject.add(r.createFreeStyleProject("job"));
        subject.getCategorizationCriteria().add(new GroupingRule("job", "Parent"));
        subject.getColumns().replaceBy(List.of(new JobColumn(), new WeatherColumn()));
        HtmlForm form =
                r.createWebClient().goTo("view/Weather-hidden/configure").getFormByName("viewConfig");
        form.<HtmlCheckBoxInput>getInputByName("_.hideAggregatedInfoOnCategories")
                .setChecked(true);
        r.submit(form);

        HtmlPage page = r.createWebClient().goTo("view/Weather-hidden/");
        assertEquals(
                0,
                page.getByXPath("//tr[@categoryRole='category']/td[contains(@class,'healthReport')]")
                        .size());
    }

    @Test
    void buildColumnsShouldNotLinkFromCategoryRows(JenkinsRule r) throws Exception {
        CategorizedJobsView subject = new CategorizedJobsView("Build-columns");
        r.jenkins.addView(subject);
        FreeStyleProject job = r.createFreeStyleProject("job");
        job.getBuildersList().add(new FailureBuilder());
        r.buildAndAssertStatus(Result.FAILURE, job);
        job.getBuildersList().clear();
        r.buildAndAssertSuccess(job);
        subject.add(job);
        subject.getCategorizationCriteria().add(new GroupingRule("job", "Parent"));
        subject.getColumns().replaceBy(List.of(new JobColumn(), new LastSuccessColumn(), new LastFailureColumn()));

        HtmlPage page = r.createWebClient().goTo("view/Build-columns/");
        assertEquals(
                0,
                page.getByXPath(
                                "//tr[@categoryRole='category']//a[contains(@href,'lastSuccessfulBuild') or contains(@href,'lastFailedBuild')]")
                        .size());
        assertEquals(
                1,
                page.getByXPath("//tr[@id='job_job']//a[contains(@href,'lastSuccessfulBuild')]")
                        .size());
        assertEquals(
                1,
                page.getByXPath("//tr[@id='job_job']//a[contains(@href,'lastFailedBuild')]")
                        .size());
        String categoryText = ((org.htmlunit.html.HtmlTableRow)
                        page.getByXPath("//tr[@categoryRole='category']").get(0))
                .asNormalizedText();
        assertTrue(categoryText.contains("#1"));
        assertTrue(categoryText.contains("#2"));
    }

    @Test
    void emptyCategoryBuildColumnsShouldUseCoreTranslations(JenkinsRule r) throws Exception {
        CategorizedJobsView subject = new CategorizedJobsView("Build-columns");
        r.jenkins.addView(subject);
        subject.add(r.createFreeStyleProject("job"));
        subject.getCategorizationCriteria().add(new GroupingRule("job", "Parent"));
        subject.getColumns().replaceBy(List.of(new JobColumn(), new LastSuccessColumn(), new LastFailureColumn()));

        LocaleProvider previous = LocaleProvider.getProvider();
        HtmlPage page;
        try {
            // JenkinsRule bypasses WebAppMain, which normally derives the locale from the request.
            LocaleProvider.setProvider(new LocaleProvider() {
                @Override
                public Locale get() {
                    return Locale.GERMAN;
                }
            });
            page = r.createWebClient().goTo("view/Build-columns/");
        } finally {
            LocaleProvider.setProvider(previous);
        }
        var header = page.getHtmlElementById("category_Parent");
        assertEquals(
                "Nicht anwendbar",
                header.<HtmlTableCell>getFirstByXPath("./td[3]").asNormalizedText());
        assertEquals(
                "Unbekannt", header.<HtmlTableCell>getFirstByXPath("./td[4]").asNormalizedText());
    }

    @Test
    void unsupportedColumnsShouldRenderOnlyForJobs(JenkinsRule r) throws Exception {
        CategorizedJobsView subject = new CategorizedJobsView("Column-contract");
        r.jenkins.addView(subject);
        subject.add(r.createFreeStyleProject("job"));
        subject.getCategorizationCriteria().add(new GroupingRule("job", "Parent/Child"));
        subject.getColumns().replaceBy(List.of(new JobColumn(), new UnsupportedColumn()));
        HtmlForm form =
                r.createWebClient().goTo("view/Column-contract/configure").getFormByName("viewConfig");
        form.<HtmlCheckBoxInput>getInputByName("_.nestedGroups").setChecked(true);
        r.submit(form);

        HtmlPage page = r.createWebClient().goTo("view/Column-contract/");
        assertEquals(2, page.getByXPath("//tr[@categoryRole='category']").size());
        assertEquals(0, page.getByXPath("//tr[@categoryRole='category']/td/a").size());
        assertEquals(
                0,
                page.getByXPath("//tr[@categoryRole='category']/td[@class='unsupported-column']")
                        .size());
        assertEquals(
                1,
                page.getByXPath("//tr[@id='job_job']/td[@class='unsupported-column']")
                        .size());
        assertTrue(page.getHtmlElementById("category_Parent/Child")
                .getTextContent()
                .contains("Child"));
        for (var row : page.getByXPath("//tr[@categoryRole='category']")) {
            assertEquals(4, ((org.htmlunit.html.HtmlTableRow) row).getCells().size());
        }
    }

    public static class UnsupportedColumn extends ListViewColumn {
        @org.kohsuke.stapler.DataBoundConstructor
        public UnsupportedColumn() {}

        public String label(TopLevelItem item) {
            if (item instanceof GroupTopLevelItem) {
                throw new AssertionError("Unsupported column received a category");
            }
            return item.getDisplayName();
        }

        @TestExtension
        public static class DescriptorImpl extends ListViewColumnDescriptor {
            @Override
            public String getDisplayName() {
                return "Unsupported column";
            }
        }
    }
}
