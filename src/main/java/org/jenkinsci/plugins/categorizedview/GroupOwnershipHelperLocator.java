package org.jenkinsci.plugins.categorizedview;

import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription;
import com.synopsys.arc.jenkins.plugins.ownership.util.AbstractOwnershipHelper;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import java.util.Objects;
import org.jenkinsci.plugins.ownership.model.OwnershipHelperLocator;
import org.jenkinsci.plugins.ownership.model.OwnershipInfo;

/**
 * Category rows are synthetic items with no ownership data of their own.
 */
@Extension(optional = true)
public class GroupOwnershipHelperLocator extends OwnershipHelperLocator<GroupTopLevelItem> {
    private static final GroupOwnershipHelper HELPER = new GroupOwnershipHelper();

    @Override
    public AbstractOwnershipHelper<GroupTopLevelItem> findHelper(Object item) {
        if (item instanceof GroupTopLevelItem) {
            return HELPER;
        }
        return null;
    }

    static class GroupOwnershipHelper extends AbstractOwnershipHelper<GroupTopLevelItem> {
        @Override
        public @NonNull OwnershipDescription getOwnershipDescription(@NonNull GroupTopLevelItem item) {
            return getOwnershipInfo(item).getDescription();
        }

        @Override
        public @NonNull String getItemTypeName(@NonNull GroupTopLevelItem item) {
            return "category";
        }

        @Override
        public @NonNull String getItemDisplayName(@NonNull GroupTopLevelItem item) {
            return item.getDisplayName();
        }

        @Override
        public @CheckForNull String getItemURL(@NonNull GroupTopLevelItem item) {
            // Category rows have no page of their own
            return null;
        }

        @Override
        public @NonNull OwnershipInfo getOwnershipInfo(@NonNull GroupTopLevelItem item) {
            OwnershipInfo common = null;
            for (TopLevelItem nested : item.getNestedItems()) {
                OwnershipInfo info = getOwnershipInfo(nested);
                // Unowned nested jobs don't count as a disagreement; they just carry no vote.
                if (info == null || !info.getDescription().isOwnershipEnabled()) {
                    continue;
                }
                if (common == null) {
                    common = info;
                    // The Primary Owner column only cares about the primary owner; secondary
                    // owners may differ between nested jobs without hiding the shared owner.
                } else if (!Objects.equals(
                        common.getDescription().getPrimaryOwnerId(),
                        info.getDescription().getPrimaryOwnerId())) {
                    return OwnershipInfo.DISABLED_INFO;
                }
            }
            return common != null ? common : OwnershipInfo.DISABLED_INFO;
        }

        private OwnershipInfo getOwnershipInfo(TopLevelItem item) {
            if (item instanceof GroupTopLevelItem) {
                return getOwnershipInfo((GroupTopLevelItem) item);
            }
            if (item instanceof Job<?, ?>) {
                AbstractOwnershipHelper<Job> helper = OwnershipHelperLocator.locate(item, Job.class);
                if (helper != null) {
                    return helper.getOwnershipInfo((Job) item);
                }
            }
            return null;
        }
    }
}
