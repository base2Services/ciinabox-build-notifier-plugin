package io.jenkins.plugins.ciinaboxbuildnotifier;

import io.jenkins.plugins.ciinaboxbuildnotifier.Runner;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

@Extension
public class CiinaboxRunListener extends RunListener<Run> {

    private final Runner runner = new Runner();

    public CiinaboxRunListener() {
        super(Run.class);
    }

    @Override
    public void onCompleted(Run r, TaskListener listener) {
       runner.buildCollector(r, listener);
       runner.sendEvent();
    }

}