package com.netflix.simianarmy.rdschaos;

import com.google.common.collect.Lists;
import com.netflix.simianarmy.*;
import com.netflix.simianarmy.basic.chaos.BasicChaosMonkey;
import com.netflix.simianarmy.chaos.*;
import com.netflix.simianarmy.client.aws.chaos.RDSChaosCrawler;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * The Class RDSChaosMonkey. It creates a RDS Chaos and then does the task of BasicChaosMonkey
 * Created by bjagwani on 4/19/16.
 */
public class RDSChaosMonkey extends ChaosMonkey{

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicChaosMonkey.class);

    /** The cfg. */
    private final MonkeyConfiguration cfg;

    /** The Constant NS. */
    private static final String NS = "simianarmy.rdschaos.";

    /** The runs per day. */
    private final long runsPerDay;

    private final MonkeyCalendar monkeyCalendar;

    private final List<ChaosType> allChaosTypes;

    /** The minimum value of the maxTerminationCountPerday property to be considered non-zero. **/
    private static final double MIN_MAX_TERMINATION_COUNT_PER_DAY = 0.001;

    // When a mandatory termination is triggered due to the minimum termination limit is breached,
    // the value below is used as the termination probability.
    private static final double DEFAULT_MANDATORY_TERMINATION_PROBABILITY = 0.5;

    /**
     * Instantiates a new RDS chaos monkey.
     *
     * @param ctx the ctx
     */
    public RDSChaosMonkey(ChaosMonkey.Context ctx) {
        super(ctx);
        this.cfg = ctx.configuration();

        this.monkeyCalendar = ctx.calendar();

        Calendar open = monkeyCalendar.now();
        Calendar close = monkeyCalendar.now();
        open.set(Calendar.HOUR, monkeyCalendar.openHour());
        close.set(Calendar.HOUR, monkeyCalendar.closeHour());

        allChaosTypes = Lists.newArrayList();
        allChaosTypes.add(new DBRebootChaosType(cfg));

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
        LOGGER.info("Running RDS chaos monkey");
        if (!isChaosMonkeyEnabled()) {
            LOGGER.info("Chaos monkey disabled");
            return;
        }

        RDSChaosCrawler extremeChaosCrawler = (RDSChaosCrawler) context().chaosCrawler();
        ChaosCrawler.InstanceGroup instanceGroup = extremeChaosCrawler.group();
        if (!this.isMaxTerminationCountExceeded(instanceGroup)) {
            double prob = getEffectiveProbabilityFromCfg(instanceGroup);
            Collection<String> instances = context().chaosInstanceSelector().select(instanceGroup, prob / runsPerDay);
            for (String inst : instances) {
                if (this.isMaxTerminationCountExceeded(instanceGroup)) {
                    break;
                }

                rebootInstance(instanceGroup, inst, allChaosTypes.get(0));
            }
        }

    }

    private MonkeyRecorder.Event rebootInstance(ChaosCrawler.InstanceGroup group, String inst, ChaosType chaosType) {
        Validate.notNull(group);
        Validate.notEmpty(inst);
        String prop = NS + "leashed";
        if (cfg.getBoolOrElse(prop, true)) {
            LOGGER.info("leashed ChaosMonkey prevented from killing {} from group {} [{}], set {}=false",
                    new Object[]{inst, group.name(), group.type(), prop});
            reportEventForSummary(EventTypes.CHAOS_REBOOT_DB_SKIPPED, group, inst);
            return null;
        } else {
            try {
                MonkeyRecorder.Event evt = recordTermination(group, inst, chaosType);
                sendTerminationNotification(group, inst, chaosType);
                SshConfig sshConfig = new SshConfig(cfg);
                ChaosInstance chaosInstance = new ChaosInstance(context().cloudClient(), inst, sshConfig);
                chaosType.apply(chaosInstance);
                LOGGER.info("Rebooting {} from group {} [{}] with {}",
                        new Object[]{inst, group.name(), group.type(), chaosType.getKey() });
                reportEventForSummary(EventTypes.CHAOS_REBOOT_DB, group, inst);
                return evt;
            } catch (NotFoundException e) {
                LOGGER.warn("Failed to reboot " + inst + ", it does not exist. Perhaps it was already rebooting");
                reportEventForSummary(EventTypes.CHAOS_REBOOT_DB_SKIPPED, group, inst);
                return null;
            } catch (Exception e) {
                handleTerminationError(inst, e);
                reportEventForSummary(EventTypes.CHAOS_REBOOT_DB_SKIPPED, group, inst);
                return null;
            }
        }
    }

    private void reportEventForSummary(EventTypes eventType, ChaosCrawler.InstanceGroup group, String instanceId) {
        context().reportEvent(createEvent(eventType, group, instanceId));
    }

    private MonkeyRecorder.Event createEvent(EventTypes chaosTermination, ChaosCrawler.InstanceGroup group, String instance) {
        MonkeyRecorder.Event evt = context().recorder().newEvent(Type.DBCHAOS, chaosTermination, group.region(), instance);
        evt.addField("groupName", group.name());
        return evt;
    }

    /**
     * Handle reboot error. This has been abstracted so subclasses can decide to continue causing chaos if desired.
     *
     * @param instance
     *            the instance
     * @param e
     *            the exception
     */
    protected void handleTerminationError(String instance, Throwable e) {
        LOGGER.error("failed to reboot instance " + instance, e);
        throw new RuntimeException("failed to reboot instance " + instance, e);
    }

    /** {@inheritDoc} */
    @Override
    public int getPreviousTerminationCount(ChaosCrawler.InstanceGroup group, Date after) {
        Map<String, String> query = new HashMap<String, String>();
        query.put("groupName", group.name());
        List<MonkeyRecorder.Event> evts = context().recorder().findEvents(Type.DBCHAOS, EventTypes.CHAOS_REBOOT_DB, query, after);
        return evts.size();
    }

    @Override
    public MonkeyRecorder.Event recordTermination(ChaosCrawler.InstanceGroup group, String instance, ChaosType chaosType) {
        MonkeyRecorder.Event evt = context().recorder().newEvent(Type.DBCHAOS, EventTypes.CHAOS_REBOOT_DB, group.region(), instance);
        evt.addField("groupName", group.name());
        evt.addField("chaosType", chaosType.getKey());
        context().recorder().recordEvent(evt);
        return evt;
    }

    @Override
    public MonkeyRecorder.Event terminateNow(String type, String name, ChaosType chaosType) throws FeatureNotEnabledException, InstanceGroupNotFoundException {
        return null;
    }

    @Override
    public void sendTerminationNotification(ChaosCrawler.InstanceGroup group, String instance, ChaosType chaosType) {
        // TODO
    }

    @Override
    public List<ChaosType> getChaosTypes() {
        return Lists.newArrayList(allChaosTypes);
    }

    private boolean isChaosMonkeyEnabled() {
        String prop = NS + "enabled";
        if (cfg.getBoolOrElse(prop, true)) {
            return true;
        }
        LOGGER.info("ChaosMonkey disabled, set {}=true", prop);
        return false;
    }

    protected boolean isMaxTerminationCountExceeded(ChaosCrawler.InstanceGroup group){
        Validate.notNull(group);
        String propName = "maxTerminationsPerDay";
        double maxTerminationsPerDay = this.getNumFromCfgOrDefault(group, propName, 1.0);
        if (maxTerminationsPerDay <= MIN_MAX_TERMINATION_COUNT_PER_DAY) {
            String prop = String.format("%s%s.%s.%s", NS, propName);
            LOGGER.info("ChaosMonkey is configured to not allow any killing from group {} [{}] "
                    + "with max daily count set as {}", new Object[]{group.name(), group.type(), prop});
            return true;
        } else {
            int daysBack = 1;
            int maxCount = (int) maxTerminationsPerDay;
            if (maxTerminationsPerDay < 1.0) {
                daysBack = (int) Math.ceil(1 / maxTerminationsPerDay);
                maxCount = 1;
            }
            Calendar after = monkeyCalendar.now();
            after.add(Calendar.DATE, -1 * daysBack);
            // Check if the group has exceeded the maximum terminations for the last period
            int terminationCount = getPreviousTerminationCount(group, after.getTime());
            if (terminationCount >= maxCount) {
                LOGGER.info("The count of terminations for group {} [{}] in the last {} days is {},"
                                + " equal or greater than the max count threshold {}",
                        new Object[]{group.name(), group.type(), daysBack, terminationCount, maxCount});
                return true;
            }
        }
        return false;
    }

    protected double getNumFromCfgOrDefault(ChaosCrawler.InstanceGroup group, String propName, double defaultValue) {
        String defaultProp = String.format("%s%s", NS, propName);
        return cfg.getNumOrElse(defaultProp, defaultValue);
    }

    /**
     * Gets the effective probability value when the monkey processes an instance group, it uses the following
     * logic in the order as listed below.
     *
     * 1) When minimum mandatory termination is enabled, a default non-zero probability is used for opted-in
     * groups, if a) the application has been opted in for the last mandatory termination window
     *        and b) there was no terminations in the last mandatory termination window
     * 2) Use the probability configured for the group type and name
     * 3) Use the probability configured for the group
     * 4) Use 1.0
     * @param group
     * @return double
     */
    protected double getEffectiveProbabilityFromCfg(ChaosCrawler.InstanceGroup group) {
        String propName;
        if (cfg.getBool(NS + "mandatoryTermination.enabled")) {
            String mtwProp = NS + "mandatoryTermination.windowInDays";
            int mandatoryTerminationWindowInDays = (int) cfg.getNumOrElse(mtwProp, 0);
            if (mandatoryTerminationWindowInDays > 0
                    && noTerminationInLastWindow(group, mandatoryTerminationWindowInDays)) {
                double mandatoryProb = cfg.getNumOrElse(NS + "mandatoryTermination.defaultProbability",
                        DEFAULT_MANDATORY_TERMINATION_PROBABILITY);
                LOGGER.info("There has been no terminations for group {} [type {}] in the last {} days,"
                                + "setting the probability to {} for mandatory termination.",
                        new Object[]{group.name(), group.type(), mandatoryTerminationWindowInDays, mandatoryProb});
                return mandatoryProb;
            }
        }
        propName = "probability";
        double prob = getNumFromCfgOrDefault(group, propName, 1.0);
        LOGGER.info("Group {} [type {}] enabled [prob {}]", new Object[]{group.name(), group.type(), prob});
        return prob;
    }

    /**
     * Returns lastOptInTimeInMilliseconds from the .properties file.
     *
     * @param group
     * @return long
     */
    protected long getLastOptInMilliseconds(ChaosCrawler.InstanceGroup group) {
        String prop = NS + ".lastOptInTimeInMilliseconds";
        long lastOptInTimeInMilliseconds = (long) cfg.getNumOrElse(prop, -1);
        return lastOptInTimeInMilliseconds;
    }

    private boolean noTerminationInLastWindow(ChaosCrawler.InstanceGroup group, int mandatoryTerminationWindowInDays) {
        long lastOptInTimeInMilliseconds = getLastOptInMilliseconds(group);
        if (lastOptInTimeInMilliseconds < 0) {
            return false;
        }

        Calendar windowStart = monkeyCalendar.now();
        windowStart.add(Calendar.DATE, -1 * mandatoryTerminationWindowInDays);

        // return true if the window start is after the last opt-in time and
        // there has been no termination since the window start
        if (windowStart.getTimeInMillis() > lastOptInTimeInMilliseconds
                && getPreviousTerminationCount(group, windowStart.getTime()) <= 0) {
            return true;
        }

        return false;
    }

}
