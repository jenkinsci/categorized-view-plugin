function restoreJobGroupCollapseState(viewName, groupName) 
{
	var collapseState = getGroupState(viewName,groupName);
	handle=$$("#handle_"+groupName	).first();
	if (collapseState == 'none') {
		hideJobGroup(handle, viewName,groupName)
	}
	else {
		showJobGroup(handle, viewName,groupName)
	}
}

function toggleJobGroupVisibility(viewName, group) 
{
	var handle=$$("#handle_"+group).first();
	if (handle.getAttribute("collapseState") == "collapsed") {
		showJobGroup(handle, viewName,group)
	}
	else {
		hideJobGroup(handle, viewName,group)
	}
}

function hideJobGroup(handle, viewName, group) {
	handle.setAttribute("collapseState", "collapsed");
	$$('.'+group).each(
		function(e){
			e.style.display="none"
		}
	)
	setGroupState(viewName,group, "none");
	var src = $$("#handle_"+group+" img").first().src;
	src = src.replace(/collapse.png/,"expand.png")
	$$("#handle_"+group+" img").first().src = src;
}

function showJobGroup(handle, viewName, group) {
	handle.setAttribute("collapseState", "expanded");
	$$('.'+group).each(
		function(e){
			e.style.display="";
			$(e).setOpacity(0)
			new YAHOO.util.Anim(e, {
                opacity: { to:1 }
            }, 0.2, YAHOO.util.Easing.easeIn).animate();
		}
	)
	setGroupState(viewName, group, "");
	var src = $$("#handle_"+group+" img").first().src;
	src = src.replace(/expand.png/,"collapse.png")
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
