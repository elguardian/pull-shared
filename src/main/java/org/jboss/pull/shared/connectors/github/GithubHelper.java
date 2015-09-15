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

package org.jboss.pull.shared.connectors.github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.LabelService;
import org.eclipse.egit.github.core.service.MilestoneService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.jboss.pull.shared.Util;

public class GithubHelper {
    private static final Logger LOG = Logger.getLogger(GithubHelper.class.getName());

    private String GITHUB_ORGANIZATION;
    private String GITHUB_REPO;
    private String GITHUB_LOGIN;
    private String GITHUB_TOKEN;

    private IRepositoryIdProvider repository;

    private CommitService commitService;
    private IssueService issueService;
    private PullRequestService pullRequestService;
    private MilestoneService milestoneService;
    private RepositoryService repositoryService;
    private LabelService labelService;

    private List<RepositoryBranch> branches;
    private Properties properties;

    private Map<String, Label> labelsCache;

    public void init(Properties properties) throws Exception {
        this.properties = properties;
        GITHUB_ORGANIZATION = Util.require(this.properties, "github.organization");
        GITHUB_REPO = Util.require(this.properties, "github.repo");

        GITHUB_LOGIN = Util.require(this.properties, "github.login");
        GITHUB_TOKEN = Util.get(this.properties, "github.token");

        GitHubClient client = new GitHubClient();
        if (GITHUB_TOKEN != null && GITHUB_TOKEN.length() > 0) {
            client.setOAuth2Token(GITHUB_TOKEN);
        }

        repository = RepositoryId.create(GITHUB_ORGANIZATION, GITHUB_REPO);
        commitService = new CommitService(client);
        issueService = new IssueService(client);
        pullRequestService = new PullRequestService(client);
        milestoneService = new MilestoneService(client);
        repositoryService = new RepositoryService(client);
        labelService = new LabelService(client);

        // init the labels, we don't need to access everytime
        labelsCache = new HashMap<String, Label>();
        List<Label> tmpLabels = labelService.getLabels(repository);
        for(Label label : tmpLabels) {
            labelsCache.put(label.getName(), label);
        }
    }

    public List<RepositoryBranch> getBranches() {
        if (branches == null) {
            branches = new ArrayList<RepositoryBranch>();
            try {
                branches = repositoryService.getBranches(repository);
            } catch (IOException e) {
                System.err.println("Error retrieving branches from repository");
                e.printStackTrace();
            }
        }
        return branches;
    }

    public PullRequest getPullRequest(int id) {
        return getPullRequest(repository, id);
    }

    public PullRequest getPullRequest(String upstreamOrganization, String upstreamRepository, int id) {
        return getPullRequest(RepositoryId.create(upstreamOrganization, upstreamRepository), id);
    }

    private PullRequest getPullRequest(IRepositoryIdProvider repository, int id) {
        PullRequest pullRequest = null;
        try {
            pullRequest = pullRequestService.getPullRequest(repository, id);
        } catch (IOException e) {
            System.err.printf("Couldn't retrieve PullRequestId: '" + id + "' from Repository: '" + repository.generateId()
                    + "'");
            e.printStackTrace();
        }
        return pullRequest;
    }

    public List<PullRequest> getPullRequests(String state) {
        List<PullRequest> result;
        try {
            result = pullRequestService.getPullRequests(repository, state);
        } catch (IOException e) {
            System.err.printf("Couldn't get pull requests in state %s of repository %s due to %s.\n", state, repository, e);
            result = new ArrayList<PullRequest>();
        }
        return result;
    }

    public void postGithubStatus(PullRequest pull, String targetUrl, String status) {
        try {
            CommitStatus commitStatus = new CommitStatus();
            commitStatus.setTargetUrl(targetUrl);
            commitStatus.setState(status);
            commitService.createStatus(repository, pull.getHead().getSha(), commitStatus);
        } catch (Exception e) {
            System.err.printf("Problem posting a status build for sha: %s\n", pull.getHead().getSha());
            e.printStackTrace(System.err);
        }
    }

    public void postGithubComment(PullRequest pull, String comment) {
        try {
            issueService.createComment(repository, pull.getNumber(), comment);
        } catch (IOException e) {
            System.err.printf("Problem posting a comment build for pull: %d\n", pull.getNumber());
            e.printStackTrace(System.err);
        }
    }

    private List<Milestone> milestones = null;

    public List<Milestone> getMilestones() {
        if (milestones == null) {
            milestones = new ArrayList<Milestone>();
            try {
                milestones = milestoneService.getMilestones(repository, "open");
                milestones.addAll(milestoneService.getMilestones(repository, "closed"));
            } catch (IOException e) {
                System.err.printf("Problem getting milestones");
                e.printStackTrace(System.err);
            }
        }
        return milestones;
    }

    public Milestone createMilestone(String title) {
        Milestone newMilestone = new Milestone();
        newMilestone.setTitle(title);
        Milestone returnMilestone = null;
        try {
            returnMilestone = milestoneService.createMilestone(repository, newMilestone);
        } catch (IOException e) {
            System.err.printf("Problem creating new milestone. title: " + title);
            e.printStackTrace(System.err);
        }
        return returnMilestone;
    }

    public Issue getIssue(PullRequest pullRequest) {
        int id = getIssueIdFromIssueURL(pullRequest.getIssueUrl());
        Issue issue = null;
        try {
            issue = issueService.getIssue(repository, id);
        } catch (IOException e) {
            System.err.printf("Problem getting issue. id: " + id);
            e.printStackTrace(System.err);
        }
        return issue;
    }

    private int getIssueIdFromIssueURL(String issueURL) {
        return Integer.valueOf(issueURL.substring(issueURL.lastIndexOf("/") + 1));
    }

    public Issue editIssue(Issue issue) {
        Issue returnIssue = null;
        try {
            returnIssue = issueService.editIssue(repository, issue);
        } catch (IOException e) {
            System.err.printf("Problem editing issue. id: " + issue.getId());
            e.printStackTrace(System.err);
        }
        return returnIssue;
    }

    public String getGithubLogin() {
        return GITHUB_LOGIN;
    }

    public boolean isMerged(PullRequest pullRequest) {
        if (pullRequest == null) {
            return false;
        }

        if (!pullRequest.getState().equals("closed")) {
            return false;
        }

        try {
            if (pullRequestService.isMerged(pullRequest.getBase().getRepo(), pullRequest.getNumber())) {
                return true;
            }
        } catch (IOException ignore) {
            System.err.printf("Cannot get Merged information of the pull request %d: %s.\n", pullRequest.getNumber(), ignore);
            ignore.printStackTrace(System.err);
        }

        try {
            final List<Comment> comments = issueService.getComments(pullRequest.getBase().getRepo(), pullRequest.getNumber());
            for (Comment comment : comments) {
                if (comment.getBody().toLowerCase().indexOf("merged") != -1) {
                    return true;
                }
            }
        } catch (IOException ignore) {
            System.err.printf("Cannot get comments of the pull request %d: %s.\n", pullRequest.getNumber(), ignore);
            ignore.printStackTrace(System.err);
        }

        return false;
    }

    public Comment getLastMatchingComment(PullRequest pullRequest, Pattern pattern) {
        Comment lastComment = null;
        List<Comment> comments = getComments(pullRequest);

        for (Comment comment : comments) {
            Matcher matcher = pattern.matcher(comment.getBody());
            if (matcher.find()) {
                lastComment = comment;
            }
        }

        return lastComment;
    }

    public List<Comment> getComments(PullRequest pullRequest) {
        try {
            return issueService.getComments(repository, pullRequest.getNumber());
        } catch (IOException e) {
            System.err.println("Error to get comments for pull request : " + pullRequest.getNumber());
            e.printStackTrace(System.err);
        }

        return new ArrayList<Comment>();
    }

    public List<Label> getLabels(PullRequest pullRequest) {
        Issue issue = getIssue(pullRequest);
        if (issue != null) {
            return issue.getLabels();
        }
        return new ArrayList<Label>();
    }

    public Label getLabel(final String label) {
        if(labelsCache.containsKey(label)) {
            return labelsCache.get(label);
        } else {
            LOG.log(Level.WARNING, "Label '" + label + "' does not exist");
            return null;
        }
    }

    public void addLabel(PullRequest pullRequest, Label label) {

        Issue issue = getIssue(pullRequest);

        List<Label> labels = issue.getLabels();
        labels.add(label);
        issue.setLabels(labels);

        editIssue(issue);

    }

    public void removeLabel(PullRequest pullRequest, Label newLabel) {
        Issue issue = getIssue(pullRequest);

        List<Label> labels = issue.getLabels();
        for( Label label : issue.getLabels() ){
            if( label.getName().equals(newLabel.getName())){
                labels.remove(label);
                break;
            }
        }
        issue.setLabels(labels);

        editIssue(issue);

    }
}
