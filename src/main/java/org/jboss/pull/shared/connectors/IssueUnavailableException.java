package org.jboss.pull.shared.connectors;

public class IssueUnavailableException extends Exception {

    private static final long serialVersionUID = 1L;

    public IssueUnavailableException(String msg) {
       super(msg);
    }

    public IssueUnavailableException(String msg, Throwable th) {
        super(msg, th);
    }

}
