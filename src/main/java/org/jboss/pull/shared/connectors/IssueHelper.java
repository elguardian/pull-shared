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
import java.util.List;
import java.util.Properties;

import org.jboss.pull.shared.connectors.common.Issue;

/**
 * @author navssurtani
 */
public interface IssueHelper<T extends Issue> {
    /**
     * A method to test if underlying implementatio accepts a given issue.n.
     *
     * @param url - the issue URL
     * @return - whether or not the url is accepted by the underlying issue tracking system.
     */
    boolean accepts(URL url);

    /**
     * Finds a given {@link Issue} based on a URL parameter. Any client should make sure that
     * {@link #accepts(java.net.URL)} is called first.
     *
     * @param url - the issue URL
     * @return - the corresponding {@link org.jboss.pull.shared.connectors.common.Issue} or null if no Issue is found
     * @throws java.lang.IllegalArgumentException - if the String is incorrect. This will be if the remote server
     * rejects the request.
     */
    T findIssue(URL url) throws IllegalArgumentException, IssueUnavailableException;

    /**
     * Update the status ofan {@link Issue}e. Before calling this method, {@link #accepts(java.net.URL)} should be called
     * @param url - the issue URL
     * @param status - the status to update to
     * @return - whether or not the status was updated successfully.
     */
    boolean updateStatus(URL url, Enum status);

    /**
     * Chechk whether the description contains a link that this issue tracker can hanler or not.
     * @param description content of the description
     * @return wheter or not there is at least one link to this issue tracker
     */
    boolean hasLinkInDescription(String description);

    /**
     * extracts the url contained in the description
     * @param description description of the issue
     * @return a list of url to this issue tracker
     */
    List<URL> extractURLs(String description);

    /**
     * initialize the issue tracker
     * @param props properties to initilize the issue tracker
     * @throws Exception
     */
    void init(Properties props) throws Exception;
}
