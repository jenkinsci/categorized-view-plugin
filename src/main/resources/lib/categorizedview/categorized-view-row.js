document.addEventListener("DOMContentLoaded", function() {
    document.querySelectorAll(".restoreJobGroupCollapseStateParams").forEach(function(elem) {
        const view = elem.dataset.view;
        const groupClass = elem.dataset.jobgroup;
        restoreJobGroupCollapseState(view, groupClass);
    });
});