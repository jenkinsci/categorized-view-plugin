package org.jenkinsci.plugins.categorizedview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hudson.model.BallColor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.HealthReport;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import org.jenkinsci.plugins.categorizedview.GroupTopLevelItem.GetBuild;
import org.junit.jupiter.api.Test;

class GroupTopLevelItemTest {

    private final GroupTopLevelItem subject = new GroupTopLevelItem("", ".*ignore-me.*");

    @Test
    void invalidLegacyIgnoreRegexShouldIncludeAllJobs() {
        GroupTopLevelItem group = new GroupTopLevelItem("Group", "[");
        group.add(makeProjectWithColor(BallColor.RED, "failure"));
        assertEquals(BallColor.RED, group.getIconColor());
        assertEquals(BallColor.RED, group.getIconColor());
    }

    @Test
    void nestedGroups_shouldAggregateDescendantJobs() {
        GroupTopLevelItem child = new GroupTopLevelItem("Child", ".*ignore-me.*");
        GroupTopLevelItem grandchild = new GroupTopLevelItem("Child/Grandchild", ".*ignore-me.*");
        child.add(grandchild);
        child.add(makeProjectWithColor(BallColor.BLUE));
        grandchild.add(makeProjectWithColor(BallColor.YELLOW));
        grandchild.add(makeProjectWithColor(BallColor.RED, "ignore-me"));
        assertEquals(BallColor.YELLOW, child.getIconColor());

        GroupTopLevelItem healthChild = new GroupTopLevelItem("Health", "");
        GroupTopLevelItem healthGrandchild = new GroupTopLevelItem("Health/Grandchild", "");
        healthChild.add(healthGrandchild);
        healthChild.add(makeProjectWithHealth(80));
        healthGrandchild.add(makeProjectWithHealth(30));
        assertEquals(30, healthChild.getBuildHealth().getScore());

        GroupTopLevelItem buildChild = new GroupTopLevelItem("Build", "");
        GroupTopLevelItem buildGrandchild = new GroupTopLevelItem("Build/Grandchild", "");
        buildChild.add(buildGrandchild);
        buildGrandchild.add(makeProjectWithLastBuildDate(ZonedDateTime.parse("2014-03-30T18:00:00-03:00")));
        buildChild.add(makeProjectWithLastBuildDate(ZonedDateTime.parse("2014-03-28T18:00:00-03:00")));
        assertEquals(
                "2014-03-30T18:00:00-03:00",
                dateString(buildChild.getLastBuild().getTimestamp()));
    }

    @Test
    void nestedItemsShouldKeepInsertionOrderWhileReadApisAreSorted() {
        subject.add(makeMockProject("z"));
        subject.add(makeMockProject("a"));
        assertEquals(
                List.of("z", "a"),
                subject.nestedItems.stream().map(TopLevelItem::getName).toList());
        assertEquals(
                List.of("a", "z"),
                subject.getNestedItems().stream().map(TopLevelItem::getName).toList());
        assertEquals(
                List.of("a", "z"),
                subject.getGroupItems().stream().map(TopLevelItem::getName).toList());
    }

    @Test
    void groupShouldUseMembershipListAssignedBySubclass() {
        TopLevelItem first = makeMockProject("first");
        TopLevelItem second = makeMockProject("second");
        GroupTopLevelItem group = new GroupTopLevelItem("Group", "") {
            {
                nestedItems = new ArrayList<>(List.of(first));
            }
        };

        assertEquals(List.of(first), group.getNestedItems());
        group.add(second);
        assertEquals(List.of(first, second), group.getNestedItems());
    }

    @Test
    void aggregationShouldUseSubclassHooks() {
        TopLevelItem job =
                makeProjectWithColorHealthAndBuild(BallColor.RED, 20, ZonedDateTime.parse("2014-03-30T18:00:00-03:00"));
        GroupTopLevelItem child = new GroupTopLevelItem("Child", "") {
            @Override
            public List<TopLevelItem> getNestedItems() {
                return List.of(job);
            }
        };
        GroupTopLevelItem parent = new GroupTopLevelItem("Parent", "") {
            @Override
            public List<TopLevelItem> getNestedItems() {
                return List.of(child);
            }

            @Override
            public BallColor chooseNextColor(BallColor current, BallColor next) {
                return next == BallColor.RED ? BallColor.YELLOW : current;
            }
        };

        assertEquals(BallColor.YELLOW, parent.getIconColor());
        assertEquals(20, parent.getBuildHealth().getScore());
        assertEquals(((Job<?, ?>) job).getLastBuild(), parent.getLastBuild());
    }

    @Test
    void healthReportsShouldContainTheBuildHealth() {
        FreeStyleProject project = makeProjectWithHealth(80);
        subject.add(project);
        assertEquals(List.of(project.getBuildHealth()), subject.getBuildHealthReports());

        HealthReport overridden = new HealthReport();
        overridden.setScore(30);
        GroupTopLevelItem group = new GroupTopLevelItem("Group", "") {
            @Override
            public HealthReport getBuildHealth() {
                return overridden;
            }
        };
        assertEquals(List.of(overridden), group.getBuildHealthReports());
    }

    @Test
    void add_toBuilderCreatedNestedChild_shouldRefreshAlreadyReadParentAggregates() {
        FreeStyleProject initial = makeMockProject("job");
        when(initial.getIconColor()).thenReturn(BallColor.BLUE);
        HealthReport initialHealth = new HealthReport();
        initialHealth.setScore(100);
        when(initial.getBuildHealth()).thenReturn(initialHealth);
        CategorizedItemsBuilder builder = new CategorizedItemsBuilder(
                List.of(initial), List.of(new GroupingRule("job", "Parent/Child/Grandchild")), ".*ignore-me.*", true);
        GroupTopLevelItem parent =
                (GroupTopLevelItem) builder.getRegroupedItems().get(0);
        GroupTopLevelItem child = (GroupTopLevelItem) parent.getNestedItems().get(0);
        GroupTopLevelItem grandchild =
                (GroupTopLevelItem) child.getNestedItems().get(0);

        for (GroupTopLevelItem group : List.of(parent, child)) {
            assertEquals(BallColor.BLUE, group.getIconColor());
            assertEquals(100, group.getBuildHealth().getScore());
            assertNull(group.getLastBuild());
        }

        grandchild.add(makeProjectWithColorHealthAndBuild(
                BallColor.RED, 20, ZonedDateTime.parse("2014-03-30T18:00:00-03:00")));

        for (GroupTopLevelItem group : List.of(parent, child)) {
            assertEquals(BallColor.RED, group.getIconColor());
            assertEquals(20, group.getBuildHealth().getScore());
            assertNotNull(group.getLastBuild());
            assertEquals(
                    "2014-03-30T18:00:00-03:00", dateString(group.getLastBuild().getTimestamp()));
        }
    }

    @Test
    void getters_shouldComputeRequestedValuesOnceIncludingMissingBuilds() {
        FreeStyleProject project = makeMockProject("job");
        when(project.getIconColor()).thenReturn(BallColor.BLUE);
        HealthReport health = new HealthReport();
        health.setScore(100);
        when(project.getBuildHealth()).thenReturn(health);
        FreeStyleBuild build = mock(FreeStyleBuild.class);
        when(build.getTimestamp()).thenReturn(new GregorianCalendar(2026, Calendar.JANUARY, 1));
        when(project.getLastBuild()).thenReturn(build);
        subject.add(project);

        for (int i = 0; i < 2; i++) {
            assertEquals(BallColor.BLUE, subject.getIconColor());
            assertEquals(100, subject.getBuildHealth().getScore());
            assertEquals(build, subject.getLastBuild());
            assertNull(subject.getLastFailedBuild());
        }

        verify(project).getIconColor();
        verify(project).getBuildHealth();
        verify(project).getLastBuild();
        verify(project).getLastFailedBuild();
        verify(project, never()).getLastSuccessfulBuild();
        verify(project, never()).getLastStableBuild();
        verify(project, never()).getLastUnsuccessfulBuild();
    }

    @Test
    void getNestedItems_shouldReturnSortedSnapshotsWithoutExposingTheMutableList() {
        TopLevelItem second = makeMockProject("b");
        TopLevelItem first = makeMockProject("a");
        subject.add(second);
        List<TopLevelItem> before = subject.getNestedItems();
        subject.add(first);

        assertEquals(List.of(second), before);
        assertEquals(List.of(first, second), subject.getNestedItems());
        assertThrows(UnsupportedOperationException.class, () -> before.add(first));
    }

    @Test
    void getBuildHealth_returnsWorstHealthValue() {
        subject.add(makeProjectWithHealth(80));
        assertEquals(80, subject.getBuildHealth().getScore());
        subject.add(makeProjectWithHealth(30));

        assertEquals(30, subject.getBuildHealth().getScore());
    }

    @Test
    void getIconColor_ShouldReturnWorstBallColor() {
        assertEquals(BallColor.NOTBUILT, subject.getIconColor());

        subject.add(makeProjectWithColor(BallColor.NOTBUILT));
        assertEquals(BallColor.NOTBUILT, subject.getIconColor());

        subject.add(makeProjectWithColor(BallColor.DISABLED));
        assertEquals(BallColor.DISABLED, subject.getIconColor());

        subject.add(makeProjectWithColor(BallColor.BLUE));
        assertEquals(BallColor.BLUE, subject.getIconColor());

        subject.add(makeProjectWithColor(BallColor.DISABLED));
        assertEquals(BallColor.BLUE, subject.getIconColor());

        subject.add(makeProjectWithColor(BallColor.ABORTED, "ignore-me"));
        assertEquals(BallColor.BLUE, subject.getIconColor());

        subject.add(makeProjectWithColor(BallColor.ABORTED));
        assertEquals(BallColor.ABORTED, subject.getIconColor());

        subject.add(makeProjectWithColor(BallColor.BLUE));
        assertEquals(BallColor.ABORTED, subject.getIconColor());

        subject.add(makeProjectWithColor(BallColor.YELLOW));
        assertEquals(BallColor.YELLOW, subject.getIconColor());

        subject.add(makeProjectWithColor(BallColor.ABORTED));
        assertEquals(BallColor.YELLOW, subject.getIconColor());

        subject.add(makeProjectWithColor(BallColor.RED));
        assertEquals(BallColor.RED, subject.getIconColor());

        subject.add(makeProjectWithColor(BallColor.YELLOW));
        assertEquals(BallColor.RED, subject.getIconColor());
    }

    @Test
    void getLastBuild_ShouldReturnLastBuildInTheGroup() {
        subject.add(makeProjectWithLastBuildDate(ZonedDateTime.parse("2014-03-28T18:00:00-03:00")));
        assertEquals(
                "2014-03-28T18:00:00-03:00", dateString(subject.getLastBuild().getTimestamp()));
        subject.add(makeProjectWithLastBuildDate(ZonedDateTime.parse("2014-03-21T18:00:00-03:00")));
        assertEquals(
                "2014-03-28T18:00:00-03:00", dateString(subject.getLastBuild().getTimestamp()));
        subject.add(makeProjectWithLastBuildDate(ZonedDateTime.parse("2014-03-30T18:00:00-03:00")));
        assertEquals(
                "2014-03-30T18:00:00-03:00", dateString(subject.getLastBuild().getTimestamp()));
    }

    @Test
    void getLastBuild_WithNullBuild_ShouldNotBreak() {
        FreeStyleProject freeStyleProject = makeMockProject();
        subject.add(makeProjectWithLastBuildDate(ZonedDateTime.parse("2014-03-28T18:00:00-03:00")));
        subject.add(freeStyleProject);
        assertEquals(
                "2014-03-28T18:00:00-03:00", dateString(subject.getLastBuild().getTimestamp()));
    }

    @Test
    void getLastSuccessfulBuild_ShouldReturnLastBuildInTheGroup() {
        subject.add(makeProjectWithLastSuccessfulBuildDate(ZonedDateTime.parse("2014-03-28T18:00:00-03:00")));
        assertEquals(
                "2014-03-28T18:00:00-03:00",
                dateString(subject.getLastSuccessfulBuild().getTimestamp()));
        subject.add(makeProjectWithLastSuccessfulBuildDate(ZonedDateTime.parse("2014-03-21T18:00:00-03:00")));
        assertEquals(
                "2014-03-28T18:00:00-03:00",
                dateString(subject.getLastSuccessfulBuild().getTimestamp()));
        subject.add(makeProjectWithLastSuccessfulBuildDate(ZonedDateTime.parse("2014-03-30T18:00:00-03:00")));
        assertEquals(
                "2014-03-30T18:00:00-03:00",
                dateString(subject.getLastSuccessfulBuild().getTimestamp()));
    }

    @Test
    void getLastStableBuild_ShouldReturnLastBuildInTheGroup() {
        subject.add(makeProjectWithLastStableBuildDate(ZonedDateTime.parse("2014-03-28T18:00:00-03:00")));
        assertEquals(
                "2014-03-28T18:00:00-03:00",
                dateString(subject.getLastStableBuild().getTimestamp()));
        subject.add(makeProjectWithLastStableBuildDate(ZonedDateTime.parse("2014-03-21T18:00:00-03:00")));
        assertEquals(
                "2014-03-28T18:00:00-03:00",
                dateString(subject.getLastStableBuild().getTimestamp()));
        subject.add(makeProjectWithLastStableBuildDate(ZonedDateTime.parse("2014-03-30T18:00:00-03:00")));
        assertEquals(
                "2014-03-30T18:00:00-03:00",
                dateString(subject.getLastStableBuild().getTimestamp()));
    }

    @Test
    void getLastFailedBuild_ShouldReturnLastBuildInTheGroup() {
        subject.add(makeProjectWithLastFailedBuildDate(ZonedDateTime.parse("2014-03-28T18:00:00-03:00")));
        assertEquals(
                "2014-03-28T18:00:00-03:00",
                dateString(subject.getLastFailedBuild().getTimestamp()));
        subject.add(makeProjectWithLastFailedBuildDate(ZonedDateTime.parse("2014-03-21T18:00:00-03:00")));
        assertEquals(
                "2014-03-28T18:00:00-03:00",
                dateString(subject.getLastFailedBuild().getTimestamp()));
        subject.add(makeProjectWithLastFailedBuildDate(ZonedDateTime.parse("2014-03-30T18:00:00-03:00")));
        assertEquals(
                "2014-03-30T18:00:00-03:00",
                dateString(subject.getLastFailedBuild().getTimestamp()));
    }

    @Test
    void getLastUnsuccessfulBuild_ShouldReturnLastBuildInTheGroup() {
        subject.add(makeProjectLastUnsuccessfulBuildDate(ZonedDateTime.parse("2014-03-28T18:00:00-03:00")));
        assertEquals(
                "2014-03-28T18:00:00-03:00",
                dateString(subject.getLastUnsuccessfulBuild().getTimestamp()));
        subject.add(makeProjectLastUnsuccessfulBuildDate(ZonedDateTime.parse("2014-03-21T18:00:00-03:00")));
        assertEquals(
                "2014-03-28T18:00:00-03:00",
                dateString(subject.getLastUnsuccessfulBuild().getTimestamp()));
        subject.add(makeProjectLastUnsuccessfulBuildDate(ZonedDateTime.parse("2014-03-30T18:00:00-03:00")));
        assertEquals(
                "2014-03-30T18:00:00-03:00",
                dateString(subject.getLastUnsuccessfulBuild().getTimestamp()));
    }

    private static TopLevelItem makeProjectLastUnsuccessfulBuildDate(final ZonedDateTime parse) {
        return makeMockToGetBuild(parse, Job::getLastUnsuccessfulBuild);
    }

    private static TopLevelItem makeProjectWithLastFailedBuildDate(final ZonedDateTime parse) {
        return makeMockToGetBuild(parse, Job::getLastFailedBuild);
    }

    private static TopLevelItem makeProjectWithLastBuildDate(final ZonedDateTime parse) {
        return makeMockToGetBuild(parse, Job::getLastBuild);
    }

    private static TopLevelItem makeProjectWithLastSuccessfulBuildDate(final ZonedDateTime parse) {
        return makeMockToGetBuild(parse, Job::getLastSuccessfulBuild);
    }

    private static TopLevelItem makeProjectWithLastStableBuildDate(final ZonedDateTime parse) {
        return makeMockToGetBuild(parse, Job::getLastStableBuild);
    }

    private static TopLevelItem makeProjectWithColor(final BallColor color) {
        return makeProjectWithColor(color, "");
    }

    private static TopLevelItem makeProjectWithColor(final BallColor color, final String projName) {
        FreeStyleProject freeStyleProject = makeMockProject(projName);
        when(freeStyleProject.getIconColor()).thenReturn(color);
        return freeStyleProject;
    }

    private static FreeStyleProject makeProjectWithHealth(final int score) {
        FreeStyleProject freeStyleProject = makeMockProject();
        HealthReport healthReport = new HealthReport();
        healthReport.setScore(score);
        when(freeStyleProject.getBuildHealth()).thenReturn(healthReport);
        return freeStyleProject;
    }

    private static FreeStyleProject makeProjectWithColorHealthAndBuild(
            final BallColor color, final int healthScore, final ZonedDateTime lastBuildDate) {
        FreeStyleProject project = makeMockProject();
        when(project.getIconColor()).thenReturn(color);
        HealthReport healthReport = new HealthReport();
        healthReport.setScore(healthScore);
        when(project.getBuildHealth()).thenReturn(healthReport);
        FreeStyleBuild lastBuild = mock(FreeStyleBuild.class);
        when(lastBuild.getTimestamp()).thenReturn(GregorianCalendar.from(lastBuildDate));
        when(project.getLastBuild()).thenReturn(lastBuild);
        return project;
    }

    private static TopLevelItem makeMockToGetBuild(final ZonedDateTime parse, final GetBuild getBuild) {
        FreeStyleProject freeStyleProject = makeMockProject();
        FreeStyleBuild lastBuild = mock(FreeStyleBuild.class);
        if (parse == null) {
            when(lastBuild.getTimestamp()).thenReturn(null);
        } else {
            when(lastBuild.getTimestamp()).thenReturn(GregorianCalendar.from(parse));
        }
        when(getBuild.getFrom(freeStyleProject)).thenReturn(lastBuild);
        return freeStyleProject;
    }

    private static FreeStyleProject makeMockProject() {
        return makeMockProject("");
    }

    public static FreeStyleProject makeMockProject(final String projName) {
        FreeStyleProject freeStyleProject = mock(FreeStyleProject.class);
        when(freeStyleProject.getName()).thenReturn(projName);
        return freeStyleProject;
    }

    private static String dateString(final Calendar timestamp) {
        return ((GregorianCalendar) timestamp).toZonedDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
