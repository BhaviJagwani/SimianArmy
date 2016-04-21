package com.netflix.simianarmy.client.aws.chaos;

import com.amazonaws.services.rds.model.DBInstance;
import com.netflix.simianarmy.GroupType;
import com.netflix.simianarmy.basic.chaos.BasicInstanceGroup;
import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.client.aws.AWSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;

/**
 * The Class RDSChaosCrawler. This will crawl for all available RDS Instances associated with the in the specified region.
 * Created by bjagwani on 4/20/16.
 */
public class RDSChaosCrawler implements ChaosCrawler {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RDSChaosCrawler.class);

    /**
     * The group types Types.
     */
    public enum Types implements GroupType {
        /** crawls RDS instances*/
       DB;
    }

    /** The aws client. */
    private final AWSClient awsClient;

    /**
     * Instantiates a new db chaos crawler.
     *
     * @param awsClient
     *            the aws client
     */
    public RDSChaosCrawler(AWSClient awsClient){
        this.awsClient= awsClient;
    }


    /** {@inheritDoc} */
    @Override
    public EnumSet<?> groupTypes() {
        return EnumSet.allOf(ASGChaosCrawler.Types.class);
    }

    /** {@inheritDoc} */
    @Override
    public List<InstanceGroup> groups() {
        return groups((String[]) null);
    }

    @Override
    public List<InstanceGroup> groups(String... names) {
        return null;
    }


    /**
     * Returns the InstanceGroup. The instances in the group are the DB Instances in the specified region
     * @return InstanceGroup the group-name has format db-instances-<region-name>-region
     *
     */
    public InstanceGroup group(){
        String groupName= "db-instances-" + awsClient.region()+"-region";
        InstanceGroup instanceGroup = new BasicInstanceGroup(groupName, Types.DB, awsClient.region(), null);

        List<DBInstance> instances= awsClient.getDBInstances();
        for(DBInstance instance: instances){
            if(instance.getMultiAZ())
                instanceGroup.addInstance(instance.getDBInstanceIdentifier()+":MULTIAZ");
            else
                instanceGroup.addInstance(instance.getDBInstanceIdentifier());
        }

        return instanceGroup;
    }

}
