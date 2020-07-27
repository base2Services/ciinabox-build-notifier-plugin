package io.jenkins.plugins.ciinaboxbuildnotifier;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;

public class SnsProvidor {
    
    private final String region;

    public SnsProvidor(String region) {
        this.region = region;
    }

    public AmazonSNS createSnsClient() {
        try {
            return AmazonSNSClientBuilder.standard()
            .withRegion(region)
            .build();
        } catch(com.amazonaws.SdkClientException e) {
            return null;
        }
    }

}