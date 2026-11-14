package org.jenkinsci.plugins.categorizedview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerColumn;
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper;
import hudson.model.FreeStyleProject;
import hudson.model.User;
import java.util.Set;
import org.jenkinsci.plugins.ownership.model.OwnershipHelperLocator;
import org.jenkinsci.plugins.ownership.model.OwnershipInfo;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class GroupOwnershipHelperLocatorTest {

    @Test
    void locate_shouldFindHelperForGroupItems(JenkinsRule r) {
        assertTrue(
                OwnershipHelperLocator.locate(new GroupTopLevelItem("Group", ""))
                        instanceof GroupOwnershipHelperLocator.GroupOwnershipHelper);
    }

    @Test
    void nestedJobsWithCommonOwner_shouldBeReportedOnCategoryRow(JenkinsRule r) throws Exception {
        User.getById("owner", true);
        JobOwnerHelper.setOwnership(r.createFreeStyleProject("a"), new OwnershipDescription(true, "owner", null));
        JobOwnerHelper.setOwnership(r.createFreeStyleProject("b"), new OwnershipDescription(true, "owner", null));

        JobOwnerColumn column = new JobOwnerColumn();
        assertTrue(column.isOwnerExists(groupOf(r, "a", "b")));
        assertEquals("owner", column.getJobOwner(groupOf(r, "a", "b")));
    }

    @Test
    void nestedJobsWithDifferentOwners_shouldShowUnknownOnCategoryRow(JenkinsRule r) throws Exception {
        User.getById("ownerA", true);
        User.getById("ownerB", true);
        JobOwnerHelper.setOwnership(r.createFreeStyleProject("a"), new OwnershipDescription(true, "ownerA", null));
        JobOwnerHelper.setOwnership(r.createFreeStyleProject("b"), new OwnershipDescription(true, "ownerB", null));

        JobOwnerColumn column = new JobOwnerColumn();
        assertEquals(
                OwnershipInfo.DISABLED_INFO.getDescription(),
                helperFor(groupOf(r, "a", "b"))
                        .getOwnershipInfo(groupOf(r, "a", "b"))
                        .getDescription());
        assertEquals(User.getUnknown().getId(), column.getJobOwner(groupOf(r, "a", "b")));
    }

    @Test
    void nestedJobsWithoutOwnership_shouldShowUnknownOnCategoryRow(JenkinsRule r) throws Exception {
        r.createFreeStyleProject("a");
        r.createFreeStyleProject("b");

        JobOwnerColumn column = new JobOwnerColumn();
        assertEquals(User.getUnknown().getId(), column.getJobOwner(groupOf(r, "a", "b")));
    }

    @Test
    void differentSecondaryOwners_shouldNotHideCommonPrimaryOwner(JenkinsRule r) throws Exception {
        User.getById("owner", true);
        User.getById("coOwnerA", true);
        User.getById("coOwnerB", true);
        JobOwnerHelper.setOwnership(
                r.createFreeStyleProject("a"), new OwnershipDescription(true, "owner", Set.of("coOwnerA")));
        JobOwnerHelper.setOwnership(
                r.createFreeStyleProject("b"), new OwnershipDescription(true, "owner", Set.of("coOwnerB")));

        JobOwnerColumn column = new JobOwnerColumn();
        assertTrue(column.isOwnerExists(groupOf(r, "a", "b")));
        assertEquals("owner", column.getJobOwner(groupOf(r, "a", "b")));
    }

    @Test
    void disabledOwnershipOnSomeNestedJobs_shouldNotHideCommonOwner(JenkinsRule r) throws Exception {
        User.getById("owner", true);
        JobOwnerHelper.setOwnership(r.createFreeStyleProject("a"), new OwnershipDescription(true, "owner", null));
        r.createFreeStyleProject("b");

        JobOwnerColumn column = new JobOwnerColumn();
        assertTrue(column.isOwnerExists(groupOf(r, "a", "b")));
        assertEquals("owner", column.getJobOwner(groupOf(r, "a", "b")));
    }

    private GroupOwnershipHelperLocator.GroupOwnershipHelper helperFor(GroupTopLevelItem group) {
        return (GroupOwnershipHelperLocator.GroupOwnershipHelper) OwnershipHelperLocator.locate(group);
    }

    private GroupTopLevelItem groupOf(JenkinsRule r, String... jobNames) {
        GroupTopLevelItem group = new GroupTopLevelItem("Group", "");
        for (String jobName : jobNames) {
            group.add((FreeStyleProject) r.jenkins.getItemByFullName(jobName));
        }
        return group;
    }
}
