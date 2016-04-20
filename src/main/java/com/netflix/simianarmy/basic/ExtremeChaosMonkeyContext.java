package com.netflix.simianarmy.basic;

import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.client.aws.chaos.DBChaosCrawler;

/**
 * The Class ExtremeChaosMonkeyContext. This provides the context required the ExtremeChaosMonkey to run.
 * It will configure the Chaos Monkey based on a simianarmy.properties file and chaos.properties file.
 * The properties file can be overridden with -Dsimianarmy.properties=/path/to/my.properties
 * Created by bjagwani on 4/20/16.
 */
public class ExtremeChaosMonkeyContext extends BasicChaosMonkeyContext{

    /** The RDS Instance crawler */
    private DBChaosCrawler dbChaosCrawler;

    /**
     * Instantiates a new extreme context.
     */
    public ExtremeChaosMonkeyContext(){
        super();
        MonkeyConfiguration cfg = configuration();
        DBChaosCrawler chaosCrawler= new DBChaosCrawler(awsClient());
        this.dbChaosCrawler= chaosCrawler;
    }

    /**
     * @return DBChaosCrawler the chaos crawler
     * */
    public ChaosCrawler xchaosCrawler(){
       return dbChaosCrawler;
   }

}
