package io.jenkins.plugins.ciinaboxbuildnotifier;

import io.jenkins.plugins.ciinaboxbuildnotifier.Collector;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

@Extension
public class CiinaboxRunListener extends RunListener<Run> {

    private final Collector collector = new Collector();

    public CiinaboxRunListener() {
        super(Run.class);
    }

    @Override
    public void onCompleted(Run r, TaskListener listener) {
        collector.buildCollector(r, listener);
        collector.sendEvent();
    }

}