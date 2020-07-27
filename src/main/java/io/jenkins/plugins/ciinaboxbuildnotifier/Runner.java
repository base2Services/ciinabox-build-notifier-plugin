package io.jenkins.plugins.ciinaboxbuildnotifier;

import io.jenkins.plugins.ciinaboxbuildnotifier.models.BuildModel;
import io.jenkins.plugins.ciinaboxbuildnotifier.models.SCMModel;
import io.jenkins.plugins.ciinaboxbuildnotifier.models.ChangelogModel;
import io.jenkins.plugins.ciinaboxbuildnotifier.SnsProvidor;
import io.jenkins.plugins.ciinaboxbuildnotifier.SnsPublisher;
import io.jenkins.plugins.ciinaboxbuildnotifier.CiinaboxBuildNotifierConfiguration;

import jenkins.model.Jenkins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Run;
import hudson.model.Executor;
import hudson.model.Result;
import hudson.model.Job;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;

import hudson.plugins.git.GitSCM;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.git.Revision;
import hudson.plugins.git.Branch;

import java.lang.InterruptedException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.amazonaws.services.sns.AmazonSNS;

public class Runner {

    private static final Logger LOGGER = Logger.getLogger(Runner.class.getName());
    private BuildModel buildModel = new BuildModel();
    private final Gson gson = new GsonBuilder().create();

    public void scmCollector(Run<?,?> r, SCM scm, TaskListener listener, ChangeLogSet<?> changelog) {
        SCMModel scmModel = new SCMModel();
        buildModel.setEventType("SCM");
        jobCollector(r);

        List<ChangelogModel> changes = new ArrayList<>();
        for (ChangeLogSet.Entry entry : changelog) {
            ChangelogModel clm = new ChangelogModel();
            clm.setTimeStamp(entry.getTimestamp());
            clm.setCommit(entry.getCommitId());
            changes.add(clm);
        }

        scmModel.setChangelog(changes);

        if (scm instanceof GitSCM) {
            GitSCM gitSCM = (GitSCM) scm;
            scmModel.setUrl(gitSCM.getKey());
            BuildData buildData = gitSCM.getBuildData(r);
            if (buildData != null) {
                Revision rev = buildData.getLastBuiltRevision();
                if (rev != null) {
                    scmModel.setCommit(rev.getSha1String());
                }
            }
        }

        try {
            EnvVars environment = r.getEnvironment(listener);
            if (environment != null) {
                scmModel.setBranch(environment.get("BRANCH_NAME", ""));
            }
        } catch (java.io.IOException e) {
            LOGGER.log(Level.WARNING, "failed to get environment vars: " + e.getMessage());
        } catch (java.lang.InterruptedException e) {
            LOGGER.log(Level.WARNING, "failed to get environment vars: " + e.getMessage());
        }

        buildModel.setSCM(scmModel);

        String message = gson.toJson(buildModel);
        LOGGER.log(Level.INFO, "SCM: " + message);
    }

    public void buildCollector(Run r, TaskListener listener) {     
        String status = "";
        Result result = r.getResult();
        if (result != null) {
            status = result.toString();
        }

        long queueTime = 0L;
        Executor executor = r.getExecutor();
        if (executor != null) {
            queueTime = executor.getTimeSpentInQueue();
        }

        buildModel.setEventType("COMPLETE");

        jobCollector(r);

        buildModel.setBuildTime(r.getDuration());
        buildModel.setQueueTime(queueTime);
        buildModel.setStatus(status);
    }

    public void jobCollector(Run r) {
        Jenkins jenkins = Jenkins.getInstance();
        buildModel.setJenkinsUrl(jenkins.getRootUrl());

        Job job = r.getParent();
        buildModel.setName(job.getFullName());
        buildModel.setDisplayName(job.getDisplayName());
        buildModel.setJobUrl(job.getUrl());

        byte[] buildIdBytes = r.getExternalizableId().getBytes(StandardCharsets.UTF_8);
        String buildId = Base64.getEncoder().encodeToString(buildIdBytes);
        buildModel.setBuildId(buildId);

        buildModel.setBuildNumber(r.getNumber());
        buildModel.setBuildUrl(r.getUrl());
    }

    public void sendEvent() {
        String message = gson.toJson(buildModel);
        
        CiinaboxBuildNotifierConfiguration config = CiinaboxBuildNotifierConfiguration.get();
        String topicArn = config.getSnsTopicArn();
        String region = config.getRegionFromSnsTopicArn();

        if (topicArn == null || region == null) {
            LOGGER.log(Level.WARNING, "Unable to retrive SNS topic arn or extract region from arn, check the SNS topic arn matches the correct format.");
            return;
        }

        SnsProvidor snsProvidor = new SnsProvidor(region);
        AmazonSNS client = snsProvidor.createSnsClient();

        if (client == null) {
            LOGGER.log(Level.WARNING, "Unable to create the SNS client, are the correct policies applied to the role?");
            return;
        }

        SnsPublisher sns = new SnsPublisher(client, topicArn);

        try {
            sns.publish(message);
        } catch(com.amazonaws.services.sns.model.NotFoundException e) {
            LOGGER.log(Level.WARNING, "Failed to send SNS message: " + e.getMessage());
        }   
    }
}