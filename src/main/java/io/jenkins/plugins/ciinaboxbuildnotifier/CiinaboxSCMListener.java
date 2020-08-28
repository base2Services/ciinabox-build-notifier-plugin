package io.jenkins.plugins.ciinaboxbuildnotifier;

import io.jenkins.plugins.ciinaboxbuildnotifier.Collector;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class CiinaboxSCMListener extends SCMListener {
    
    private final Collector collector = new Collector();
    private static final Logger LOGGER = Logger.getLogger(CiinaboxSCMListener.class.getName());
    
    @Override
    public void onCheckout(Run<?,?> build, SCM scm, FilePath workspace, TaskListener listener, File changelogFile, SCMRevisionState pollingBaseline) {
        collector.scmCheckoutCollector(build, scm, workspace, listener, changelogFile, pollingBaseline);
        collector.sendEvent();;
    }
}