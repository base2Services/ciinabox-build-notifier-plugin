package io.jenkins.plugins.ciinaboxbuildnotifier;

import io.jenkins.plugins.ciinaboxbuildnotifier.models.BuildModel;
import io.jenkins.plugins.ciinaboxbuildnotifier.models.SCMModel;
import io.jenkins.plugins.ciinaboxbuildnotifier.SnsProvidor;
import io.jenkins.plugins.ciinaboxbuildnotifier.SnsPublisher;
import io.jenkins.plugins.ciinaboxbuildnotifier.CiinaboxBuildNotifierConfiguration;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.model.Run;
import hudson.model.Executor;
import hudson.model.Result;
import hudson.model.Job;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;

import hudson.plugins.git.GitSCM;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.git.Revision;
import hudson.plugins.git.Branch;

import java.lang.InterruptedException;
import java.io.IOException;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.regions.Regions;
import com.amazonaws.regions.Region;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;

public class Collector {

    private static final Logger LOGGER = Logger.getLogger(Collector.class.getName());
    private BuildModel buildModel = new BuildModel();
    private final Gson gson = new GsonBuilder().create();

    public void scmCheckoutCollector(Run<?,?> build, SCM scm, FilePath workspace, TaskListener listener, File changelogFile, SCMRevisionState pollingBaseline) {
        SCMModel scmModel = new SCMModel();
        buildModel.setEventType("SCM");
        jobCollector(build);
        awsCollector();

        if (scm instanceof GitSCM) {
            GitSCM gitSCM = (GitSCM) scm;
            scmModel.setUrl(gitSCM.getKey());
            BuildData buildData = gitSCM.getBuildData(build);
            if (buildData != null) {
                Revision rev = buildData.getLastBuiltRevision();
                if (rev != null) {
                    scmModel.setCommit(rev.getSha1String());
                }
            }
        }

        try {
            EnvVars environment = build.getEnvironment(listener);
            if (environment != null) {
                scmModel.setBranch(environment.get("BRANCH_NAME", ""));
            }
        } catch (java.io.IOException e) {
            LOGGER.log(Level.WARNING, "failed to get environment vars: " + e.getMessage());
        } catch (java.lang.InterruptedException e) {
            LOGGER.log(Level.WARNING, "failed to get environment vars: " + e.getMessage());
        }

        buildModel.setSCM(scmModel);
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

        buildModel.setEventType("COMPLETED");

        jobCollector(r);
        awsCollector();

        buildModel.setBuildTime(r.getDuration());
        buildModel.setQueueTime(queueTime);
        buildModel.setStatus(status);
    }

    public void jobCollector(Run r) {
        Job job = r.getParent();
        buildModel.setName(job.getFullName());
        buildModel.setDisplayName(job.getDisplayName());
        buildModel.setJobUrl(job.getUrl());

        byte[] eventdIdBytes = r.getExternalizableId().getBytes(StandardCharsets.UTF_8);
        String eventId = Base64.getEncoder().encodeToString(eventdIdBytes);
        buildModel.setEventId(eventId);

        buildModel.setBuildNumber(r.getNumber());
        buildModel.setBuildUrl(r.getUrl());
    }

    public void awsCollector() {
        Region region = Regions.getCurrentRegion();
        if (region != null) {
            try {
                AWSSecurityTokenService sts = AWSSecurityTokenServiceClientBuilder.standard().withRegion(region.getName()).build();
                GetCallerIdentityResult result = sts.getCallerIdentity(new GetCallerIdentityRequest());
                buildModel.setAwsAccountId(result.getAccount());
                buildModel.setAwsRegion(region.getName());
            } catch(com.amazonaws.SdkClientException e) {
                // do nothing here
            }
        }
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