/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.pull.shared.connectors;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.User;
import org.jboss.pull.shared.BuildResult;
import org.jboss.pull.shared.Constants;
import org.jboss.pull.shared.connectors.common.Issue;
import org.jboss.pull.shared.connectors.github.GithubHelper;

public class RedhatPullRequest {
    private Logger LOG = Logger.getLogger(RedhatPullRequest.class.getName());

    private PullRequest pullRequest;

    private List<RedhatPullRequest> relatedPullRequests = null;

    private List<IssueHelper<?>> issueHelpers;
    private GithubHelper ghHelper;

    public RedhatPullRequest(PullRequest pullRequest, List<IssueHelper<?>> issueHelpers, GithubHelper ghHelper) {
        this.pullRequest = pullRequest;
        this.issueHelpers = issueHelpers;
        this.ghHelper = ghHelper;

    }

    /**
     * Returns a merged list of both Bugzilla and Jira Issues found in the body of a Pull Request.
     *
     * @return
     */
    public List<Issue> getIssues() {
        List<Issue> issues = new ArrayList<Issue>();
        for(IssueHelper<?> issueHelper : issueHelpers) {
            String description = pullRequest.getBody();
            if(issueHelper.hasLinkInDescription(description)) {
                for(URL url : issueHelper.extractURLs(description)) {
                    try {
                       issues.add(issueHelper.findIssue(url));
                    } catch(IssueUnavailableException e) {
                        LOG.warning("Failed to locate the issue " + url);
                    }
                }
            }
        }
        return issues;
    }

    public boolean hasBugLinkInDescription() {
        for(IssueHelper<?> issueHelper : issueHelpers) {
            String description = pullRequest.getBody();
            if(issueHelper.hasLinkInDescription(description)) {
               return true;
            }
        }
        return false;
    }

    /**
     * Returns true if PR link is in the description
     *
     * @return
     */
    public boolean hasRelatedPullRequestInDescription() {
        boolean retVal = false;
        if (Constants.RELATED_PR_PATTERN.matcher(getGithubDescription()).find()) {
            retVal = true;
        }

        if (Constants.ABBREVIATED_RELATED_PR_PATTERN.matcher(getGithubDescription()).find()) {
            retVal = true;
        }

        if (Constants.COMMIT_RELATED_PR_PATTERN.matcher(getGithubDescription()).find()) {
            retVal = true;
        }

        return retVal;
    }

    public List<RedhatPullRequest> getRelatedPullRequests() {
        if (relatedPullRequests != null) {
            return relatedPullRequests;
        } else {
            return relatedPullRequests = getPRFromDescription();
        }
    }

    public boolean isUpstreamRequired() {
        return !Constants.UPSTREAM_NOT_REQUIRED.matcher(pullRequest.getBody()).find();
    }

    private List<RedhatPullRequest> getPRFromDescription() {
        Matcher matcher = Constants.RELATED_PR_PATTERN.matcher(getGithubDescription());

        List<RedhatPullRequest> relatedPullRequests = new ArrayList<RedhatPullRequest>();
        while (matcher.find()) {
            PullRequest relatedPullRequest = ghHelper.getPullRequest(matcher.group(1), matcher.group(2),
                    Integer.valueOf(matcher.group(3)));
            if (relatedPullRequest != null) {
                relatedPullRequests.add(new RedhatPullRequest(relatedPullRequest, issueHelpers, ghHelper));
            }
        }

        Matcher abbreviatedMatcher = Constants.ABBREVIATED_RELATED_PR_PATTERN.matcher(getGithubDescription());

        while (abbreviatedMatcher.find()) {
            String match = abbreviatedMatcher.group();
            System.out.println("Match: " + match);
            Matcher abbreviatedExternalMatcher = Constants.ABBREVIATED_RELATED_PR_PATTERN_EXTERNAL_REPO.matcher(match);

            if (abbreviatedExternalMatcher.find()) {
                System.out.println("Attempting External Match: " + match);
                PullRequest relatedPullRequest = ghHelper.getPullRequest(abbreviatedExternalMatcher.group(1),
                        abbreviatedExternalMatcher.group(2), Integer.valueOf(abbreviatedExternalMatcher.group(3)));
                if (relatedPullRequest != null) {
                    System.out.println("External Match Found: " + match);
                    relatedPullRequests.add(new RedhatPullRequest(relatedPullRequest, issueHelpers, ghHelper));
                    continue;
                }

            }

            System.out.println("Attempting Internal Match: " + match);
            PullRequest relatedPullRequest = ghHelper.getPullRequest(getOrganization(), getRepository(),
                    Integer.valueOf(abbreviatedMatcher.group(2)));
            if (relatedPullRequest != null) {
                System.out.println("Internal Match Found: " + match);
                relatedPullRequests.add(new RedhatPullRequest(relatedPullRequest, issueHelpers, ghHelper));
            }

        }

        return relatedPullRequests;
    }

    public int getNumber() {
        return pullRequest.getNumber();
    }

    public void postGithubComment(String comment) {
        ghHelper.postGithubComment(pullRequest, comment);
    }

    public Milestone getMilestone() {
        return pullRequest.getMilestone();
    }

    public void setMilestone(Milestone milestone) {
        org.eclipse.egit.github.core.Issue issue = ghHelper.getIssue(pullRequest);

        issue.setMilestone(milestone);
        ghHelper.editIssue(issue);
    }

    public String getTargetBranchTitle() {
        return pullRequest.getBase().getRef();
    }

    public String getSourceBranchSha() {
        return pullRequest.getHead().getSha();
    }

    public User getGithubUser() {
        return pullRequest.getUser();
    }

    public List<Comment> getGithubComments() {
        return ghHelper.getComments(pullRequest);
    }

    public void postGithubStatus(String targetUrl, String status) {
        ghHelper.postGithubStatus(pullRequest, targetUrl, status);
    }

    public String getGithubDescription() {
        return pullRequest.getBody();
    }

    public Date getGithubUpdatedAt() {
        return pullRequest.getUpdatedAt();
    }

    public Comment getLastMatchingGithubComment(Pattern pattern) {
        return ghHelper.getLastMatchingComment(pullRequest, pattern);
    }

    public String getState() {
        return pullRequest.getState();
    }

    public String getHtmlUrl() {
        return pullRequest.getHtmlUrl();
    }

    public boolean isMerged() {
        return ghHelper.isMerged(pullRequest);
    }

    public BuildResult getBuildResult() {
        BuildResult buildResult = BuildResult.UNKNOWN;
        Comment comment = ghHelper.getLastMatchingComment(pullRequest, Constants.BUILD_OUTCOME);

        if (comment != null) {
            Matcher matcher = Constants.BUILD_OUTCOME.matcher(comment.getBody());
            while (matcher.find()) {
                buildResult = BuildResult.valueOf(matcher.group(2));
            }
        }

        return buildResult;
    }

    public String getOrganization() {
        Matcher matcher = Constants.RELATED_PR_PATTERN.matcher(pullRequest.getUrl());
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    public String getRepository() {
        Matcher matcher = Constants.RELATED_PR_PATTERN.matcher(pullRequest.getUrl());
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return null;
    }

    public boolean updateStatus(Issue issue, Enum status) throws IllegalArgumentException {
        for(IssueHelper<?> issueHelper : issueHelpers) {
           if(issueHelper.accepts(issue.getUrl())) {
              return issueHelper.updateStatus(issue.getUrl(), status);
           }
        }
        return false;
    }

    public boolean isGithubMilestoneNullOrDefault() {
        return (pullRequest.getMilestone() == null || pullRequest.getMilestone().getTitle().contains("x"));
    }

    public List<Label> getGithubLabels() {
        return ghHelper.getLabels(pullRequest);
    }

    public void addLabel(Label newLabel) {
        ghHelper.addLabel(pullRequest, newLabel);
    }

    public void removeLabel(Label newLabel) {
        ghHelper.removeLabel(pullRequest, newLabel);
    }


}
