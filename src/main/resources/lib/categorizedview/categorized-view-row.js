document.addEventListener("DOMContentLoaded", function() {

    const rows = document.querySelectorAll("[categoryRole='category']");

    rows.forEach(function(row) {
        const holder = row.querySelector(".view-group-data-holder");

        const view = holder.dataset.view;
        const groupClass = holder.dataset.jobgroup;

        row.addEventListener("click", function(e) {
            toggleJobGroupVisibility(view, groupClass);
        });

        restoreJobGroupCollapseState(view, groupClass);
    });
});
