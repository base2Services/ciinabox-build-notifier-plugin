package io.jenkins.plugins.ciinaboxbuildnotifier.models;

import io.jenkins.plugins.ciinaboxbuildnotifier.models.ChangelogModel;

import java.util.List;

public class SCMModel {

    private String commit;
    private String branch;
    private String url;
    private List<ChangelogModel> changelog;

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

    public List<ChangelogModel> getChangelog() {
        return changelog;
    }

    public void setChangelog(List<ChangelogModel> changelog) {
        this.changelog = changelog;
    }

}