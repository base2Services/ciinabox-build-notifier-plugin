package io.jenkins.plugins.ciinaboxbuildnotifier.models;

public class ChangelogModel {
    
    private long timeStamp;
    private String commit;

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }
}