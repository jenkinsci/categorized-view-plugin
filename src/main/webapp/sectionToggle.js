function restoreJobGroupCollapseState(viewName, groupName) 
{
	var collapseState = getGroupState(viewName,groupName);
	
	if (collapseState == 'none') {
		hideJobGroup(viewName,groupName)
	}
	else {
		showJobGroup(viewName,groupName)
	}
}

function toggleJobGroupVisibility(handle, viewName, group) 
{
	if (handle.getAttribute("collapseState") == "collapsed") {
		showJobGroup(viewName,group)
	}
	else {
		hideJobGroup(viewName,group)
	}
}

function hideJobGroup(viewName, group) {
	$$("#handle_"+group).first().setAttribute("collapseState", "collapsed");
	$$('.'+group).each(
		function(e){
			e.parentNode.style.display="none"
		}
	)
	setGroupState(viewName,group, "none");
	var src = $$("#handle_"+group+" img").first().src;
	src = src.replace(/images\/.*/,"images/collapsed.png")
	$$("#handle_"+group+" img").first().src = src;
}

function showJobGroup(viewName, group) {
	$$("#handle_"+group).first().setAttribute("collapseState", "expanded");
	$$('.'+group).each(
		function(e){
			e.parentNode.style.display=""
		}
	)
	setGroupState(viewName, group, "");
		var src = $$("#handle_"+group+" img").first().src;
	src = src.replace(/images\/.*/,"images/expanded.png")
	$$("#handle_"+group+" img").first().src = src;	
}

function getGroupStates(viewName) {
	var stateCookie = YAHOO.util.Cookie.get("jenkins.categorized-view-collapse-state_"+viewName);
	if (stateCookie == null)
		return {};
	return JSON.parse(stateCookie);
}

function getGroupState(viewName, groupName) {
	var groupStates = getGroupStates(viewName)
	
	if (groupStates[groupName]==null) {
		setGroupState(viewName, groupName, "none");
		return "none";
	}
	return groupStates[groupName];
}

function setGroupState(viewName, groupName, state) 
{
	var groupStates = getGroupStates(viewName)
	groupStates[groupName]=state
	YAHOO.util.Cookie.set("jenkins.categorized-view-collapse-state_"+viewName, Object.toJSON(groupStates));
}
