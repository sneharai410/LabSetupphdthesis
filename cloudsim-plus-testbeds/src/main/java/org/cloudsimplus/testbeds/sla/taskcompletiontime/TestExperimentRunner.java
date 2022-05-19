package org.cloudsimplus.testbeds.sla.taskcompletiontime;

    import org.cloudsimplus.testbeds.ExperimentRunner;

/**
 * Runs the {@link CloudletTaskCompletionTimeWithoutMinimizationExperiment} the number of
 * times defined by {@link #getSimulationRuns()} and computes statistics.
 *
 * @author raysaoliveira
 */
public class TestExperimentRunner extends ExperimentRunner<TestExperiment> {

    /**
     * Different lengths that will be randomly assigned to created Cloudlets.
     */
    static final long[] CLOUDLET_LENGTHS = {10000, 14000, 20000, 40000};
    static final int CLOUDLETS = 90;
    public static final int VMS = 30;
    public static final int[] VM_PES = {2, 4};

    /**
     * Indicates if each experiment will output execution logs or not.
     */
    private final boolean experimentVerbose = false;

    /**
     * Starts the execution of the experiments the number of times defines in
     * {@link #getSimulationRuns()}.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        new TestExperimentRunner(true, 1475098589732L)
            .setVerbose(true)
            .run();
    }

    private TestExperimentRunner(final boolean applyAntitheticVariatesTechnique, final long baseSeed) {
        super(baseSeed, 300, 5, applyAntitheticVariatesTechnique);
    }

    @Override
    protected TestExperiment createExperimentInternal(int index) {
        TestExperiment exp
            = new TestExperiment(index, this);
        exp.setAfterExperimentFinish(this::afterExperimentFinish).setVerbose(experimentVerbose);
        return exp;
    }

    /**
     * Method automatically called after every experiment finishes running.
     * It performs some post-processing such as collection of data for statistic analysis.
     *
     * @param experiment the finished experiment
     */
    private void afterExperimentFinish(TestExperiment experiment) {
        //The Task Completion Time average for all the experiments.
        addMetricValue("Cloudlet Task Completion Time", experiment.getTaskCompletionTimeAverage());

        // The percentage of cloudlets meeting Task Completion Time average for all the experiments.
        addMetricValue("Percentage Of Cloudlets Meeting TaskTimesCompletion", experiment.getPercentageOfCloudletsMeetingTaskCompletionTime());

        // Amount of cloudlet PE per PE of vm.
        addMetricValue("Average of vPEs/CloudletsPEs", experiment.getRatioOfExistingVmPesToRequiredCloudletPes());

        // Average of the cost total
        addMetricValue("Average of Total Cost of simulation", experiment.getTotalCostPrice());
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
