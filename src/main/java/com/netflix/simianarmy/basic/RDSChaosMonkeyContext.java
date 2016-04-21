package com.netflix.simianarmy.basic;

import com.netflix.simianarmy.basic.chaos.BasicChaosInstanceSelector;
import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.chaos.ChaosEmailNotifier;
import com.netflix.simianarmy.chaos.ChaosInstanceSelector;
import com.netflix.simianarmy.chaos.ChaosMonkey;
import com.netflix.simianarmy.client.aws.chaos.RDSChaosCrawler;

/**
 * The Class RDSChaosMonkeyContext. This provides the context required the RDSChaosMonkey to run.
 * It will configure the Chaos Monkey based on a simianarmy.properties file and rdschaos.properties file.
 * The properties file can be overridden with -Dsimianarmy.properties=/path/to/my.properties
 * Created by bjagwani on 4/20/16.
 */
public class RDSChaosMonkeyContext extends BasicSimianArmyContext implements ChaosMonkey.Context{

    /** The RDS Instance crawler */
    private RDSChaosCrawler dbChaosCrawler;

    /** The selector. */
    private ChaosInstanceSelector selector;

    /**
     * Instantiates a new extreme context.
     */
    public RDSChaosMonkeyContext(){
        super("simianarmy.properties", "client.properties", "rdschaos.properties");
        RDSChaosCrawler chaosCrawler= new RDSChaosCrawler(awsClient());
        this.dbChaosCrawler= chaosCrawler;

        setChaosInstanceSelector(new BasicChaosInstanceSelector());

    }

    /**
     * @return RDSChaosCrawler the chaos crawler
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
