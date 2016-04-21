package com.netflix.simianarmy.basic;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.basic.chaos.BasicChaosEmailNotifier;
import com.netflix.simianarmy.basic.chaos.BasicChaosInstanceSelector;
import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.chaos.ChaosEmailNotifier;
import com.netflix.simianarmy.chaos.ChaosInstanceSelector;
import com.netflix.simianarmy.chaos.ChaosMonkey;
import com.netflix.simianarmy.client.aws.chaos.ASGChaosCrawler;
import com.netflix.simianarmy.client.aws.chaos.DBChaosCrawler;
import com.netflix.simianarmy.client.aws.chaos.FilteringChaosCrawler;
import com.netflix.simianarmy.client.aws.chaos.TagPredicate;

/**
 * The Class DBChaosMonkeyContext. This provides the context required the DBChaosMonkey to run.
 * It will configure the Chaos Monkey based on a simianarmy.properties file and dbchaos.properties file.
 * The properties file can be overridden with -Dsimianarmy.properties=/path/to/my.properties
 * Created by bjagwani on 4/20/16.
 */
public class DBChaosMonkeyContext extends BasicSimianArmyContext implements ChaosMonkey.Context{

    /** The RDS Instance crawler */
    private DBChaosCrawler dbChaosCrawler;

    /** The selector. */
    private ChaosInstanceSelector selector;

    /**
     * Instantiates a new extreme context.
     */
    public DBChaosMonkeyContext(){
        super("simianarmy.properties", "client.properties", "dbchaos.properties");
        DBChaosCrawler chaosCrawler= new DBChaosCrawler(awsClient());
        this.dbChaosCrawler= chaosCrawler;

        setChaosInstanceSelector(new BasicChaosInstanceSelector());

    }

    /**
     * @return DBChaosCrawler the chaos crawler
     * */
    public ChaosCrawler chaosCrawler(){
       return dbChaosCrawler;
   }

    /**
     * Sets the chaos instance selector.
     *
     * @param chaosInstanceSelector
     *            the new chaos instance selector
     */
    protected void setChaosInstanceSelector(ChaosInstanceSelector chaosInstanceSelector) {
        this.selector = chaosInstanceSelector;
    }

    @Override
    public ChaosInstanceSelector chaosInstanceSelector() {
        return selector;
    }

    @Override
    public ChaosEmailNotifier chaosEmailNotifier() {
        return null;
    }

}
