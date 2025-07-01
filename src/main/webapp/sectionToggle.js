function restoreJobGroupCollapseState(viewName, groupName) {
    var collapseState = getGroupState(viewName, groupName);
    handle = document.querySelector("#handle_" + groupName);
    if (collapseState == 'none') {
        hideJobGroup(handle, viewName, groupName)
    } else {
        showJobGroup(handle, viewName, groupName)
    }
}

function toggleJobGroupVisibility(viewName, group) {
    var handle = document.querySelector("#handle_" + group);
    if (handle.getAttribute("collapseState") == "collapsed") {
        showJobGroup(handle, viewName, group)
    } else {
        hideJobGroup(handle, viewName, group)
    }
}

function hideJobGroup(handle, viewName, group) {
    handle.setAttribute("collapseState", "collapsed");
    document.querySelectorAll('.' + group).forEach(
        function (e) {
            e.style.display = "none";
        }
    )
    setGroupState(viewName, group, "none");
}

function showJobGroup(handle, viewName, group) {
    handle.setAttribute("collapseState", "expanded");
    document.querySelectorAll('.' + group).forEach(
        function (e) {
            e.style.display = "";
            e.style.animation = "cat-view-animate-in 0.5s";
        }
    )
    setGroupState(viewName, group, "");
}

function getGroupStates(viewName) {
    var stateCookie = localStorage.getItem("jenkins.categorized-view-collapse-state_" + viewName);
    if (stateCookie == null)
        return {};
    return JSON.parse(stateCookie);
}

function getGroupState(viewName, groupName) {
    var groupStates = getGroupStates(viewName)

    if (groupStates[groupName] == null) {
        setGroupState(viewName, groupName, "none");
        return "none";
    }
    return groupStates[groupName];
}

function setGroupState(viewName, groupName, state) {
    var groupStates = getGroupStates(viewName)
    groupStates[groupName] = state
    localStorage.setItem("jenkins.categorized-view-collapse-state_" + viewName, JSON.stringify(groupStates));
}
