package com.netflix.simianarmy.chaos;

import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Created by bjagwani on 4/18/16.
 */
public class DBRebootChaosType extends ChaosType{

    /**
     * Protected constructor (abstract class).
     *
     * @param config Configuration to use
     */
    public DBRebootChaosType(MonkeyConfiguration config) {
        super(config, "RebootRDS");
    }

    @Override
    public void apply(ChaosInstance instance) {
        CloudClient cloudClient = instance.getCloudClient();
        System.out.println(cloudClient.getClass().getCanonicalName());
        String instanceId = instance.getInstanceId();

        cloudClient.rebootDBInstance(instanceId);
    }
}
