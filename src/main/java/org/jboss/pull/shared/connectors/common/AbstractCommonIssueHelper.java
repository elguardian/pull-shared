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

package org.jboss.pull.shared.connectors.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author navssurtani
 */

public abstract class AbstractCommonIssueHelper {

    private Properties properties;


    public void init(Properties properties) throws Exception {
        this.properties = properties;
        init();
    }

    public abstract void init() throws Exception;


    public final Properties getProperties() {
        return properties;
    }

    protected final List<URL> extractURLs(String urlBase, Pattern toMatch, String content) {
        final List<URL> urls = new ArrayList<URL>();
        final Matcher matcher = toMatch.matcher(content);
        while (matcher.find()) {
            try {
                urls.add(new URL(urlBase + matcher.group(1)));
            } catch (NumberFormatException ignore) {
                System.err.printf("Invalid bug number: %s.\n", ignore);
            } catch (MalformedURLException malformed) {
                System.err.printf("Invalid URL formed: %s. \n", malformed);
            }
        }
        return urls;
    }

}
