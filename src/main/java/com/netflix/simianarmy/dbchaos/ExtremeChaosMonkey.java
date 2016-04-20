package com.netflix.simianarmy.dbchaos;

import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.basic.ExtremeChaosMonkeyContext;
import com.netflix.simianarmy.basic.chaos.BasicChaosMonkey;
import com.netflix.simianarmy.chaos.*;
import com.netflix.simianarmy.client.aws.chaos.DBChaosCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.TimeUnit;


/**
 * The Class ExtremeChaosMonkey. It creates a DB Chaos and then does the task of BasicChaosMonkey
 * Created by bjagwani on 4/19/16.
 */
public class ExtremeChaosMonkey extends BasicChaosMonkey{

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicChaosMonkey.class);

    /** The cfg. */
    private final MonkeyConfiguration cfg;

    /** The Constant NS. */
    private static final String NS = "simianarmy.chaos.";

    /** The runs per day. */
    private final long runsPerDay;

    private final MonkeyCalendar monkeyCalendar;

    /**
     * Instantiates a new Extreme chaos monkey.
     *
     * @param ctx the ctx
     */
    public ExtremeChaosMonkey(ChaosMonkey.Context ctx) {
        super(ctx);
        this.cfg = ctx.configuration();

        this.monkeyCalendar = ctx.calendar();

        Calendar open = monkeyCalendar.now();
        Calendar close = monkeyCalendar.now();
        open.set(Calendar.HOUR, monkeyCalendar.openHour());
        close.set(Calendar.HOUR, monkeyCalendar.closeHour());

        TimeUnit freqUnit = ctx.scheduler().frequencyUnit();
        if (TimeUnit.DAYS == freqUnit) {
            runsPerDay = ctx.scheduler().frequency();
        } else {
            long units = freqUnit.convert(close.getTimeInMillis() - open.getTimeInMillis(), TimeUnit.MILLISECONDS);
            runsPerDay = units / ctx.scheduler().frequency();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void doMonkeyBusiness(){
        context().resetEventReport();
        cfg.reload();
        LOGGER.info("Running extreme chaos monkey");
        if (!isChaosMonkeyEnabled()) {
            LOGGER.info("Chaos monkey disabled");
            return;
        }

        ExtremeChaosMonkeyContext xctx= (ExtremeChaosMonkeyContext) context();

        DBChaosCrawler extremeChaosCrawler = (DBChaosCrawler) xctx.xchaosCrawler();
        ChaosCrawler.InstanceGroup instanceGroup = extremeChaosCrawler.group();
        if (isGroupEnabled(instanceGroup)) {
            if (!isMaxTerminationCountExceeded(instanceGroup)) {
                double prob = getEffectiveProbability(instanceGroup);
                Collection<String> instances = context().chaosInstanceSelector().select(instanceGroup, prob / runsPerDay);
                for (String inst : instances) {
                    if (isMaxTerminationCountExceeded(instanceGroup)) {
                        break;
                    }
                    SshConfig sshConfig = new SshConfig(cfg);
                    ChaosInstance chaosInstance = new ChaosInstance(context().cloudClient(), inst, sshConfig);
                    DBRebootChaosType chaosType= new DBRebootChaosType(cfg);

                    chaosType.apply(chaosInstance);
                }
            }
        }

        //super.doMonkeyBusiness();
    }

    private boolean isChaosMonkeyEnabled() {
        String prop = NS + "enabled";
        if (cfg.getBoolOrElse(prop, true)) {
            return true;
        }
        LOGGER.info("ChaosMonkey disabled, set {}=true", prop);
        return false;
    }

}
