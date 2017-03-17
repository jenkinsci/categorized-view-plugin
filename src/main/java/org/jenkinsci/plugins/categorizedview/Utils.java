package org.jenkinsci.plugins.categorizedview;

public class Utils {

	static String normalizeRegex(String groupRegex) {
		if (groupRegex == null) return "";
		String regex = groupRegex;
		if (!regex.startsWith(".*"))
			regex =".*"+regex;
		if (!regex.endsWith(".*"))
			regex +=".*";
		if (!regex.contains("(")) {
			regex = ".*("+groupRegex+").*";
		}
		return regex;
	}

}
