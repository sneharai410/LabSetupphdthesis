package org.cloudsimplus.testbeds.sla.taskcompletiontime;

    import org.cloudsimplus.testbeds.ExperimentRunner;

/**
 * Runs the {@link CloudletTaskCompletionTimeMinimizationExperiment} the number of
 * times defines by {@link #getSimulationRuns()} and compute statistics.
 *
 * @author raysaoliveira
 */
public final class TestExperiment2Runner extends ExperimentRunner<TestExperiment2> {

    /**
     * Different lengths that will be randomly assigned to created Cloudlets.
     */
    static final long[] CLOUDLET_LENGTHS = {10000, 14000, 20000, 40000,60000,80000};
    static final int[] VM_PES = {2, 4};
    static final int[] CLOUDLET_PES = {2};
    static final int[] MIPS_VM = {1000};
    static final int VMS = 30;
    static final int CLOUDLETS = 40;

    /**
     * Indicates if each experiment will output execution logs or not.
     */
    private final boolean experimentVerbose = true;

    /**
     * Starts the execution of the experiments the number of times defines in
     * {@link #getSimulationRuns()}.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        new TestExperiment2Runner(true, 9075098589732L)
            .setVerbose(true)
            .run();
    }

    private TestExperiment2Runner(final boolean antitheticVariatesTechnique, final long baseSeed) {
        super(baseSeed, 300, 5, antitheticVariatesTechnique);
    }

    @Override
    protected TestExperiment2Runner createExperimentInternal(int index) {
        TestExperiment2Runner exp
            = new TestExperiment2Runner(index, this);

        exp.setAfterExperimentFinish(this::afterExperimentFinish).setVerbose(experimentVerbose);
        return exp;
    }

    /**
     * Method automatically called after every experiment finishes running. It
     * performs some post-processing such as collection of data for statistic
     * analysis.
     *
     * @param exp the finished experiment
     */
    private void afterExperimentFinish(CloudletTaskCompletionTimeMinimizationExperiment exp) {
        //The Task Completion Time average for all the experiments.
        addMetricValue("Cloudlet Task Completion Time", exp.getTaskCompletionTimeAverage());

        // The percentage of cloudlets meeting Task Completion Time average for all the experiments.
        addMetricValue("Percentage Of Cloudlets Meeting the Task Completion Time", exp.getPercentageOfCloudletsMeetingTaskCompletionTime());

        // Amount of cloudlet PE per PE of vm.
        addMetricValue("Average of vPEs/CloudletsPEs", exp.getRatioOfExistingVmPesToRequiredCloudletPes());
    }

    @Override
    protected void printSimulationParameters() {
        System.out.printf("Executing %d experiments. Please wait ... It may take a while.%n", getSimulationRuns());
        System.out.println("Experiments configurations:");
        System.out.printf("\tBase seed: %d | Number of VMs: %d | Number of Cloudlets: %d%n", getBaseSeed(), VMS, CLOUDLETS);
        System.out.printf("\tApply Antithetic Variates Technique: %b%n", isApplyAntitheticVariates());
        if (isApplyBatchMeansMethod()) {
            System.out.println("\tApply Batch Means Method to reduce simulation results correlation: true");
            System.out.printf("\tNumber of Batches for Batch Means Method: %d", getBatchesNumber());
            System.out.printf("\tBatch Size: %d%n", batchSizeCeil());
        }
        System.out.printf("%nSimulated Annealing Parameters%n");
    }
}
