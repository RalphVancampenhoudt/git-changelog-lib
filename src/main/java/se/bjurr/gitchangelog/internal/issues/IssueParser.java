package se.bjurr.gitchangelog.internal.issues;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Ordering.usingToString;
import static java.util.regex.Pattern.compile;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import se.bjurr.gitchangelog.internal.git.model.GitCommit;
import se.bjurr.gitchangelog.internal.model.ParsedIssue;
import se.bjurr.gitchangelog.internal.settings.CustomIssue;
import se.bjurr.gitchangelog.internal.settings.Settings;

public class IssueParser {

 private final List<GitCommit> commits;
 private final Settings settings;

 public IssueParser(Settings settings, List<GitCommit> commits) {
  this.settings = settings;
  this.commits = commits;
 }

 public Settings getSettings() {
  return settings;
 }

 public List<GitCommit> getCommits() {
  return commits;
 }

 public List<ParsedIssue> parseForIssues() {
  Map<String, ParsedIssue> foundIssues = newHashMap();

  List<CustomIssue> patterns = getPatterns(settings);

  for (GitCommit gitCommit : commits) {
   boolean commitMappedToIssue = false;
   for (CustomIssue issuePattern : patterns) {
    Matcher matcher = compile(issuePattern.getPattern()).matcher(gitCommit.getMessage());
    if (matcher.find()) {
     String matched = matcher.group();
     if (!foundIssues.containsKey(matched)) {
      String link = issuePattern.getLink().or("") //
        .replaceAll("\\$\\{PATTERN_GROUP\\}", matched);
      for (int i = 0; i <= matcher.groupCount(); i++) {
       link = link.replaceAll("\\$\\{PATTERN_GROUP_" + i + "\\}", matcher.group(i));
      }
      foundIssues.put(matched, new ParsedIssue(//
        issuePattern.getName(),//
        matched,//
        link));
     }
     foundIssues.get(matched).addCommit(gitCommit);
     commitMappedToIssue = true;
    }
   }
   if (!commitMappedToIssue) {
    ParsedIssue noIssue = new ParsedIssue(settings.getNoIssueName(), null, null);
    if (!foundIssues.containsKey(noIssue.getName())) {
     foundIssues.put(noIssue.getName(), noIssue);
    }
    foundIssues.get(noIssue.getName()).addCommit(gitCommit);
   }
  }
  return usingToString().sortedCopy(foundIssues.values());
 }

 public static List<CustomIssue> getPatterns(Settings settings) {
  if (settings.getCustomIssues() == null) {
   return newArrayList();
  }
  List<CustomIssue> patterns = newArrayList(settings.getCustomIssues());
  if (!isNullOrEmpty(settings.getJiraIssuePattern())) {
   if (settings.getJiraServer().isPresent()) {
    patterns.add(new CustomIssue("Jira", settings.getJiraIssuePattern(), settings.getJiraServer().or("")
      + "/browse/${PATTERN_GROUP}"));
   } else {
    patterns.add(new CustomIssue("Jira", settings.getJiraIssuePattern(), settings.getJiraServer().orNull()));
   }
  }
  return patterns;
 }
}
