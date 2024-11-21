document.addEventListener("DOMContentLoaded", function() {

    const rows = document.querySelectorAll("[categoryRole='category']");
    rows.forEach(function(row) {

        const view = row.dataset.view;
        const groupClass = row.dataset.jobgroup;

        row.addEventListener("click", function(e) {
            toggleJobGroupVisibility(view, groupClass);
        });

        restoreJobGroupCollapseState(view, groupClass);
    });
});
