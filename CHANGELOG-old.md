# Old Changelog

This is the old changelog, see
[GitHub Releases](https://github.com/jenkinsci/categorized-view-plugin/releases)
for recent versions.

## Version 1.10

- Thanks to GitHub's dcendents PR, the plugin has been fixed to be compatible with Pipeline jobs.

## Version 1.9

After a long time, I'm finally releasing a new version with a couple of changes.

- It's now possible to choose whether the jobs should be grouped by "Display Name" instead of their internal names
- You can specify a regex to exclude jobs from "status computing". That is, if a job is broken, but its **actual name** (not display name) matches the regex, it won't affect the group status.

## Version 1.8

- \[JENKINS-22624\] Support Jenkins 1.532.1

## Version 1.7

- \[JENKINS-22585\] When editing an existing categorized view, the plugin wasn't showing jobs that have been previously checked and where grouped.

## Version 1.6

- Botched.

## Version 1.5

- JENKINS-22580 - Fixed a NPE that happened due jobs that had no builds being mixed with jobs that had.

## Version 1.4

- Fixed a small glitch that would show up the hidden header borders on Firefox.

## Version 1.3

- Just removed a sample extension that was released by accident.

## Version 1.2

- Complete UI Revamp
- \[JENKINS-22415\] Category line now summarizes the columns result columns, health status, last build success and failure and so on.
- \[JENKINS-19466\] Fixed the bug that caused the sorting break.
- \[JENKINS-21343\] Categorization rules are now extensible throught the "CategorizationCriteria" extension point
