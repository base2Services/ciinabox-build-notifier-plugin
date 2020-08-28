package io.jenkins.plugins.ciinaboxbuildnotifier;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;

public class SnsPublisher {
    
    private static final Logger LOGGER = Logger.getLogger(SnsPublisher.class.getName());
    private AmazonSNS client;
    private String topicArn;

    public SnsPublisher(AmazonSNS client, String topicArn) {
        this.client = client;
        this.topicArn = topicArn;
    }

    public void publish(String message) {
        if (client == null) {
            LOGGER.log(Level.WARNING, "no sns client setup to send build event");
            return;
        }

        try {
            PublishRequest request = new PublishRequest()
                .withTopicArn(topicArn)
                .withSubject("JenkinsBuildEvent")
                .withMessage(message);
            client.publish(request);
            LOGGER.log(Level.INFO, "published build event sns message");
        } catch (com.amazonaws.SdkClientException e) {
            LOGGER.log(Level.WARNING, "failed to publish build even to sns message: " + e.getMessage());
        }
    }
}