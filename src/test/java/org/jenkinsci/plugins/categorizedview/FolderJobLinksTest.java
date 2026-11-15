package org.jenkinsci.plugins.categorizedview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import java.net.URL;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class FolderJobLinksTest {
    @Test
    void categorizedFolderJobs_shouldHaveWorkingJobBuildAndScheduleLinks(JenkinsRule r) throws Exception {
        var folder = r.createFolder("outer folder");
        var inner = folder.createProject(MockFolder.class, "inner");
        var project = inner.createProject(FreeStyleProject.class, "build job");
        r.buildAndAssertSuccess(project);
        project.getBuildersList().add(new FailureBuilder());
        r.buildAndAssertStatus(Result.FAILURE, project);
        project.getBuildersList().clear();

        var rootView = new CategorizedJobsView("Categories");
        r.jenkins.addView(rootView);
        var folderView = new CategorizedJobsView("Categories");
        folder.addView(folderView);

        for (var view : new CategorizedJobsView[] {rootView, folderView}) {
            view.setRecurse(true);
            view.add(project);
            view.getCategorizationCriteria().add(new GroupingRule("build job", "Builds"));
            var client = r.createWebClient();
            HtmlPage page = client.goTo(view.getUrl());
            assertEquals(1, page.getByXPath("//tr[@categoryRole='category']").size());
            for (String suffix : new String[] {"", "lastSuccessfulBuild/", "lastFailedBuild/", "build?delay=0sec"}) {
                var links = page.<HtmlAnchor>getByXPath("//tr[@id='job_build job']//a");
                var expectedSuffix = project.getShortUrl() + suffix;
                var matching = links.stream()
                        .filter(link -> link.getHrefAttribute().endsWith(expectedSuffix))
                        .toList();
                assertEquals(1, matching.size(), view.getUrl() + " -> " + expectedSuffix);
                var targetUrl = new URL(page.getUrl(), matching.get(0).getHrefAttribute());
                if (suffix.startsWith("build?")) {
                    int nextBuild = project.getNextBuildNumber();
                    var request = new WebRequest(targetUrl, HttpMethod.POST);
                    client.addCrumb(request);
                    assertTrue(client.getPage(request).getWebResponse().getStatusCode() < 400);
                    r.waitUntilNoActivity();
                    assertEquals(nextBuild, project.getLastBuild().getNumber());
                    r.assertBuildStatusSuccess(project.getLastBuild());
                } else {
                    HtmlPage target = client.getPage(targetUrl);
                    assertEquals(200, target.getWebResponse().getStatusCode(), targetUrl.toString());
                    assertEquals(targetUrl.getPath(), target.getUrl().getPath());
                }
            }
        }
    }
}
