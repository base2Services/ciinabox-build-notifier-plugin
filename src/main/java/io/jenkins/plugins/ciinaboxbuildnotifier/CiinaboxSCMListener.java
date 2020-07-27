package io.jenkins.plugins.ciinaboxbuildnotifier;

import hudson.Extension;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.SCMListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;

import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class CiinaboxSCMListener extends SCMListener {
    
    private final Runner runner = new Runner();
    private static final Logger LOGGER = Logger.getLogger(CiinaboxSCMListener.class.getName());
    
    @Override
    public void onChangeLogParsed(Run<?,?> build, SCM scm, TaskListener listener, ChangeLogSet<?> changelog) {
        runner.scmCollector(build, scm, listener, changelog);
        runner.sendEvent();
    }
}