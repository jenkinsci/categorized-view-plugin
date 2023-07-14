package org.jenkinsci.plugins.categorizedview;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.BallColor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.HealthReport;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.jenkinsci.plugins.categorizedview.GroupTopLevelItem.GetBuild;
import org.junit.Test;

public class GroupTopLevelItemTest {
    GroupTopLevelItem subject = new GroupTopLevelItem("", ".*ignore-me.*");

    @Test
    public void getBuildHealth_returnsWorstHealthValue() {
        subject.add(makeProjectWithHealth(80));
        subject.add(makeProjectWithHealth(30));

        assertEquals(30, subject.getBuildHealth().getScore());
    }

    @Test
    public void getIconColor_ShouldReturnWorstBallColor() {
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
    public void getLastBuild_ShouldReturnLastBuildInTheGroup() {
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
    public void getLastBuild_WithNullBuild_ShouldNotBreak() {
        FreeStyleProject freeStyleProject = makeMockProject();
        subject.add(makeProjectWithLastBuildDate(ZonedDateTime.parse("2014-03-28T18:00:00-03:00")));
        subject.add(freeStyleProject);
        assertEquals(
                "2014-03-28T18:00:00-03:00", dateString(subject.getLastBuild().getTimestamp()));
    }

    @Test
    public void getLastSuccessfulBuild_ShouldReturnLastBuildInTheGroup() {
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
    public void getLastStableBuild_ShouldReturnLastBuildInTheGroup() {
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
    public void getLastFailedBuild_ShouldReturnLastBuildInTheGroup() {
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
    public void getLastUnsuccessfulBuild_ShouldReturnLastBuildInTheGroup() {
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

    private TopLevelItem makeProjectLastUnsuccessfulBuildDate(final ZonedDateTime parse) {
        return makeMockToGetBuild(parse, new GroupTopLevelItem.GetBuild() {
            @Override
            public Run getFrom(final Job project) {
                return project.getLastUnsuccessfulBuild();
            }
        });
    }

    private TopLevelItem makeProjectWithLastFailedBuildDate(final ZonedDateTime parse) {
        return makeMockToGetBuild(parse, new GroupTopLevelItem.GetBuild() {
            @Override
            public Run getFrom(final Job project) {
                return project.getLastFailedBuild();
            }
        });
    }

    private TopLevelItem makeProjectWithLastBuildDate(final ZonedDateTime parse) {
        return makeMockToGetBuild(parse, new GroupTopLevelItem.GetBuild() {
            @Override
            public Run getFrom(final Job project) {
                return project.getLastBuild();
            }
        });
    }

    private TopLevelItem makeProjectWithLastSuccessfulBuildDate(final ZonedDateTime parse) {
        return makeMockToGetBuild(parse, new GroupTopLevelItem.GetBuild() {
            @Override
            public Run getFrom(final Job project) {
                return project.getLastSuccessfulBuild();
            }
        });
    }

    private TopLevelItem makeProjectWithLastStableBuildDate(final ZonedDateTime parse) {
        return makeMockToGetBuild(parse, new GroupTopLevelItem.GetBuild() {
            @Override
            public Run getFrom(final Job project) {
                return project.getLastStableBuild();
            }
        });
    }

    private TopLevelItem makeProjectWithColor(final BallColor color) {
        return makeProjectWithColor(color, "");
    }

    private TopLevelItem makeProjectWithColor(final BallColor color, final String projName) {
        FreeStyleProject freeStyleProject = makeMockProject(projName);
        when(freeStyleProject.getIconColor()).thenReturn(color);
        return freeStyleProject;
    }

    private FreeStyleProject makeProjectWithHealth(final int score) {
        FreeStyleProject freeStyleProject = makeMockProject();
        HealthReport healthReport = new HealthReport();
        healthReport.setScore(score);
        when(freeStyleProject.getBuildHealth()).thenReturn(healthReport);
        return freeStyleProject;
    }

    private TopLevelItem makeMockToGetBuild(final ZonedDateTime parse, final GetBuild getBuild) {
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

    private FreeStyleProject makeMockProject() {
        return makeMockProject("");
    }

    public FreeStyleProject makeMockProject(final String projName) {
        FreeStyleProject freeStyleProject = mock(FreeStyleProject.class);
        when(freeStyleProject.getName()).thenReturn(projName);
        return freeStyleProject;
    }

    private String dateString(final Calendar timestamp) {
        return ((GregorianCalendar) timestamp).toZonedDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
