package io.jenkins.plugins.ciinaboxbuildnotifier;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;

@Extension
public class CiinaboxBuildNotifierConfiguration extends GlobalConfiguration {

    private String snsTopicArn;

    /** @return the singleton instance */
    public static CiinaboxBuildNotifierConfiguration get() {
        return GlobalConfiguration.all().get(CiinaboxBuildNotifierConfiguration.class);
    }

    public CiinaboxBuildNotifierConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    /** @return the currently configured snsTopicArn, if any */
    public String getSnsTopicArn() {
        return snsTopicArn;
    }

    /**
     * Together with {@link #getSnsTopicArn}, binds to entry in {@code config.jelly}.
     * @param snsTopicArn the new value of this field
     */
    public void setSnsTopicArn(String snsTopicArn) {
        this.snsTopicArn = snsTopicArn;
        save();
    }

    /** @return the AWS region from the snsTopicArn */
    public String getRegionFromSnsTopicArn() {
        if (this.snsTopicArn == null) {
            return null;
        }

        String[] parts = this.snsTopicArn.split(":");
        
        if (parts.length < 6) {
            return null;
        }

        return parts[3];
    }

}