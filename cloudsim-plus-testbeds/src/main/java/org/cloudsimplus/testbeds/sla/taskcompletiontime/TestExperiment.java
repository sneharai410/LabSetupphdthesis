package org.cloudsimplus.testbeds.sla.taskcompletiontime;

    import org.cloudbus.cloudsim.brokers.DatacenterBroker;
    import org.cloudbus.cloudsim.cloudlets.Cloudlet;
    import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
    import org.cloudbus.cloudsim.datacenters.Datacenter;
    import org.cloudbus.cloudsim.distributions.ContinuousDistribution;
    import org.cloudbus.cloudsim.distributions.UniformDistr;
    import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
    import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
    import org.cloudbus.cloudsim.vms.Vm;
    import org.cloudbus.cloudsim.vms.VmCost;
    import org.cloudbus.cloudsim.vms.VmSimple;
    import org.cloudsimplus.listeners.EventInfo;
    import org.cloudsimplus.slametrics.SlaContract;
    import org.cloudsimplus.testbeds.ExperimentRunner;

    import java.util.ArrayList;
    import java.util.Comparator;
    import java.util.List;

    import static org.cloudsimplus.testbeds.sla.taskcompletiontime.CloudletTaskCompletionTimeWithoutMinimizationRunner.*;

/**
 *
 * @author raysaoliveira
 */
class TestExperiment extends AbstractCloudletTaskCompletionTimeExperiment {
    private static final int SCHEDULING_INTERVAL = 5;

    private final SlaContract contract;

    private final ContinuousDistribution randCloudlet, randVm;

    /**
     * The file containing the SLA Contract in JSON format.
     */
    private static final String METRICS_FILE = "SlaMetrics.json";

    private TestExperiment(final long seed) {
        this(0, null, seed);
    }

    TestExperiment(final int index, final ExperimentRunner runner) {
        this(index, runner, -1);
    }

    private TestExperiment(final int index, final ExperimentRunner runner, final long seed) {
        super(index, runner, seed);
        randCloudlet = new UniformDistr(getSeed());
        randVm = new UniformDistr(getSeed()+1);
        this.contract = SlaContract.getInstance(METRICS_FILE);
        getSimulation().addOnClockTickListener(this::printVmsCpuUsage);
    }

    @Override
    public void printResults() {
        printBrokerFinishedCloudlets(getFirstBroker());
    }

    private void printVmsCpuUsage(EventInfo eventInfo) {
        DatacenterBroker broker0 = getFirstBroker();
        broker0.getVmExecList().sort(Comparator.comparingLong(Vm::getId));

        broker0.getVmExecList().forEach(vm
            -> System.out.printf("#### Time %.0f: Vm %d CPU usage: %.2f. SLA: %.2f.%n",
            eventInfo.getTime(), vm.getId(),
            vm.getCpuPercentUtilization(), getCustomerMaxCpuUtilization())
        );
    }

    private double getCustomerMaxCpuUtilization() {
        return contract.getCpuUtilizationMetric().getMaxDimension().getValue();
    }

    @Override
    protected Vm createVm(final DatacenterBroker broker, final int id) {
        final int pesId = (int) (randVm.sample() * VM_PES.length);
        final int pes = VM_PES[pesId];

        final Vm vm = new VmSimple(id, 1000, pes)
            .setRam(512).setBw(1000).setSize(10000)
            .setCloudletScheduler(new CloudletSchedulerTimeShared());
        return vm;
    }

    @Override
    protected List<Cloudlet> createCloudlets(final DatacenterBroker broker) {
        final List<Cloudlet> cloudletList = new ArrayList<>(CLOUDLETS);
        for (int id = cloudletList.size(); id < cloudletList.size() + CLOUDLETS; id++) {
            cloudletList.add(createCloudlet(broker));
        }

        return cloudletList;
    }

    @Override
    protected Cloudlet createCloudlet(final DatacenterBroker broker) {
        final int i = (int) (randCloudlet.sample() * CLOUDLET_LENGTHS.length);
        final long length = CLOUDLET_LENGTHS[i];

        return new CloudletSimple(nextCloudletId(), length, 2)
            .setFileSize(1024)
            .setOutputSize(1024)
            .setUtilizationModel(new UtilizationModelFull());
    }

    @Override
    protected Datacenter createDatacenter(final int index) {
        final Datacenter dc = super.createDatacenter(index);
        dc.getCharacteristics()
            .setCostPerSecond(3.0)
            .setCostPerMem(0.05)
            .setCostPerStorage(0.001)
            .setCostPerBw(0.0);
        dc.setSchedulingInterval(SCHEDULING_INTERVAL);
        return dc;
    }


    @Override
    protected double getTaskCompletionTimeAverage() {
        final double mean = super.getTaskCompletionTimeAverage();

        System.out.printf(
            "\t\t%nTaskCompletionTime simulation: %.2f%n SLA's Task Completion Time: %.2f%n",
            mean, getSlaMaxTaskCompletionTime());
        return mean;
    }

    private double getSlaMaxTaskCompletionTime() {
        return contract.getTaskCompletionTimeMetric().getMaxDimension().getValue();
    }

    double getPercentageOfCloudletsMeetingTaskCompletionTime() {
        DatacenterBroker broker = getBrokerList().stream()
            .findFirst()
            .orElse(DatacenterBroker.NULL);

        double totalOfcloudletSlaSatisfied = broker.getCloudletFinishedList().stream()
            .map(c -> c.getFinishTime() - c.getLastDatacenterArrivalTime())
            .filter(rt -> rt <= getSlaMaxTaskCompletionTime())
            .count();

        System.out.printf(
            "%n ** Percentage of cloudlets that complied with the SLA Agreement:  %.2f%%",
            ((totalOfcloudletSlaSatisfied * 100) / broker.getCloudletFinishedList().size()));
        System.out.printf("%nTotal of cloudlets SLA satisfied: %.0f de %d", totalOfcloudletSlaSatisfied, broker.getCloudletFinishedList().size());
        return (totalOfcloudletSlaSatisfied * 100) / broker.getCloudletFinishedList().size();
    }

    /**
     * Gets the ratio of existing vPEs (VM PEs) divided by the number of
     * required PEs of all Cloudlets, which indicates the mean number of vPEs
     * that are available for each PE required by a Cloudlet, considering all
     * the existing Cloudlets. For instance, if the ratio is 0.5, in average,
     * two Cloudlets requiring one vPE will share that same vPE.
     *
     * @return the average of vPEs/CloudletsPEs ratio
     */
    double getRatioOfExistingVmPesToRequiredCloudletPes() {
        double sumPesVms = getSumPesVms();
        double sumPesCloudlets = getSumPesCloudlets();

        return sumPesVms / sumPesCloudlets;
    }

    /**
     * Calculates the cost price of resources (processing, bw, memory, storage)
     * of each or all of the Datacenter VMs()
     *
     */
    double getTotalCostPrice() {
        VmCost vmCost;
        double totalCost = 0.0;
        for (final Vm vm : getVmList()) {
            if (vm.getCloudletScheduler().hasFinishedCloudlets()) {
                vmCost = new VmCost(vm);
                totalCost += vmCost.getTotalCost();
            } else System.out.printf("\t%s didn't execute any Cloudlet.%n", vm);
        }

        return totalCost;
    }

    /**
     * A main method just for test purposes.
     *
     * @param args
     */
    public static void main(String[] args) {
        TestExperiment exp
            = new TestExperiment(1);
        exp.setVerbose(true);
//        exp.run();
        exp.getTaskCompletionTimeAverage();
        exp.getPercentageOfCloudletsMeetingTaskCompletionTime();
    }
}
