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

package org.jboss.pull.shared.connectors.bugzilla;


import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.jboss.pull.shared.Constants;
import org.jboss.pull.shared.Util;
import org.jboss.pull.shared.connectors.IssueHelper;
import org.jboss.pull.shared.connectors.IssueUnavailableException;
import org.jboss.pull.shared.connectors.common.AbstractCommonIssueHelper;

public class BZHelper extends AbstractCommonIssueHelper implements IssueHelper<Bug> {

    private Bugzilla bugzillaClient;

    @Override
    public void init() throws Exception {
         String login = Util.require(getProperties(), "bugzilla.login");
         String password = Util.require(getProperties(), "bugzilla.password");

         // initialize bugzilla client
         bugzillaClient = new Bugzilla(Constants.BUGZILLA_BASE, login, password);
    }


    @Override
    public Bug findIssue(URL url) throws IssueUnavailableException {
        try {
           Bug bug = bugzillaClient.getBug(cutIdFromURL(url));
           if(bug == null) {
              throw new IssueUnavailableException("Failed to locate " + url);
           } else {
              return bug;
           }
        } catch (Exception e) {
           throw new IssueUnavailableException("Failed to locate " + url, e);
        }
    }

    @Override
    public boolean accepts(URL url) {
        return url.getHost().equalsIgnoreCase(Constants.BUGZILLA_HOST);
    }

    // FIXME: This has to be implemented properly.
    @Override
    public boolean updateStatus(URL url, Enum status) {
        throw new UnsupportedOperationException("This feature is not implemented yet.");
    }

    /**
     * Returns true if BZ link is in the PR description. Does not verify BZ exists.
     *
     * @return
     */
    @Override
    public boolean hasLinkInDescription(String description) {
        return extractURLs(description).size() > 0;
    }

    @Override
    public List<URL> extractURLs(String description) {
        return extractURLs(Constants.BUGZILLA_BASE_ID, Constants.BUGZILLA_ID_PATTERN, description);
    }

    private int cutIdFromURL(URL url) {
        String urlStr = url.toString().trim().toLowerCase();
        int index = urlStr.indexOf("id=");
        return Integer.parseInt(urlStr.substring(index + 3));
    }

    public SortedSet<Comment> loadCommentsFor(Bug bug) throws IllegalArgumentException {
        return bugzillaClient.commentsFor(bug);
    }

    public Map<String, SortedSet<Comment>> loadCommentsFor(Collection<String> bugIds) throws IllegalArgumentException {
        return bugzillaClient.commentsFor(bugIds);
    }

    public Map<String, Bug> loadIssues(Set<String> bugIds) throws IllegalArgumentException {
        return bugzillaClient.getBugs(bugIds);
    }

    public boolean addComment(final int id, final String text, CommentVisibility visibility, double worktime) {
        return bugzillaClient.addComment(id, text, visibility, worktime);
    }

    public boolean updateEstimate(final int id, final double worktime) {
        return bugzillaClient.updateEstimate(id, worktime);
    }

}