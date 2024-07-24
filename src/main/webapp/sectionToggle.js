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
            e.style.display = "none"
        }
    )
    setGroupState(viewName, group, "none");
    var src = document.querySelector("#handle_" + group + " img").src;
    src = src.replace(/collapse.png/, "expand.png")
    document.querySelector("#handle_" + group + " img").src = src;
}

function showJobGroup(handle, viewName, group) {
    handle.setAttribute("collapseState", "expanded");
    document.querySelectorAll('.' + group).forEach(
        function (e) {
            e.style.display = "";
            e.style.opacity = 0;
            new YAHOO.util.Anim(e, {
                opacity: {
                    to: 1
                }
            }, 0.2, YAHOO.util.Easing.easeIn).animate();
        }
    )
    setGroupState(viewName, group, "");
    var src = document.querySelector("#handle_" + group + " img").src;
    src = src.replace(/expand.png/, "collapse.png")
    document.querySelector("#handle_" + group + " img").src = src;
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
