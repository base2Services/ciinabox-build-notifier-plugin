package io.jenkins.plugins.ciinaboxbuildnotifier.models;

import java.util.List;

public class SCMModel {

    private String commit;
    private String branch;
    private String url;

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}