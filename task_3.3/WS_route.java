package task_33;

import java.util.HashMap;

import jasima.core.random.RandomFactory;
import jasima.core.random.continuous.DblSequence;
import jasima.core.random.discrete.IntEmpirical;
import jasima.core.simulation.Simulation;
import jasima.core.statistics.SummaryStat;
import jasima.core.util.Util;
import jasima.shopSim.core.Job;
import jasima.shopSim.core.JobSource;
import jasima.shopSim.core.Operation;
import jasima.shopSim.core.PrioRuleTarget;
import jasima.shopSim.core.Shop;
import jasima.shopSim.core.WorkStation;
import jasima.shopSim.core.WorkStationListener;
import jasima.shopSim.util.MachineStatCollector;

public class WS_route extends Shop {
	
	private static double LENGTH_SIM = 1000.0;
	
	private static final int RANDOM_FACTORY_SEED = 28;
	double probDistribJobType[] = { 0.3, 0.1, 0.2, 0.1, 0.3 };
	int listJobType[] = { 0, 1, 2, 3, 4 };
	
	double probDistribArrivalTime[] = { 0.25, 0.6, 0.15 };
	int listArrivalTime[] = { 5, 10, 15 };
	
	float serviceTime[][] = { { 10.0f, 12.0f, 25.0f, 10.0f},
			{ 15.0f, 10.0f, 12.0f, 20.0f},
			{ 10.0f, 10.0f, 10.0f, 10.0f},
			{ 20.0f, 35.0f, 5.0f },
			{ 12.0f, 5.0f, 15.0f}};
	
	WorkStation route[][];
	
	final int NUM_JOB_TYPES = serviceTime.length;
	final static int NUM_MACHINES = 11;
	
	SummaryStat[] jobTypeDelay;
	
	private DblSequence arrivalTime;
	private DblSequence jobType;
	
	private int nmrTotalJobsCreated;
	private int[] nmrTypeJobsCompleted = new int[NUM_JOB_TYPES];
	private int nmrTotalJobsCompleted;
	
	public static void main(String args[]) throws Exception {
		Simulation sim = new Simulation();

		WS_route ws_r = new WS_route();
		sim.addComponent(ws_r);

		sim.setSimulationLength(LENGTH_SIM);
		
		for(int i = 0; i < NUM_MACHINES; i++) {
			ws_r.addMachine(new WorkStation(1));
		}

		WorkStation[] ws = ws_r.getMachines();
		ws_r.route = new WorkStation[][] { { ws[0], ws[4], ws[6], ws[10] },
			{ ws[1], ws[4], ws[7], ws[9] },
			{ ws[2], ws[5], ws[7], ws[7] },
			{ ws[2], ws[5], ws[10] },
			{ ws[3], ws[8], ws[10] } };

		sim.init();
		sim.beforeRun();
		sim.run();
		sim.afterRun();
		sim.done();
		ws_r.report();
	}
	
	@Override
	public void init() {
		installMachineListener(new WorkStationListener() {

			@Override
			public void operationStarted(WorkStation m, PrioRuleTarget b, int oldSetupState, int newSetupState, double setupTime) {
				assert b.numJobsInBatch() == 1;
				Job job = b.job(0);

				jobTypeDelay[job.getJobType()].value(simTime() - job.getArriveTime());
			}
		}, false);
		installMachineListener(new MachineStatCollector(), true);

		super.init();
		
		RandomFactory rf = new RandomFactory(getSim(), RANDOM_FACTORY_SEED);
		arrivalTime = rf.initRndGen(new IntEmpirical("arrivalTimes", probDistribArrivalTime, listArrivalTime));
		jobType = rf.initRndGen(new IntEmpirical("jobTypes", probDistribJobType, listJobType));

		addJobSource(new JobSource() {
			@Override
			public Job createNextJob() {
				Job job = new Job(WS_route.this);
				
				nmrTotalJobsCreated++;
				
				job.setRelDate(simTime() + arrivalTime.nextDbl());
				//System.out.println("new job arrival time " + job.getRelDate());

				job.setJobType((int)jobType.nextDbl());
				//System.out.println("new job type " + job.getJobType());
				job.setTaskNumber(0);

				WorkStation[] ms = route[job.getJobType()];

				job.setOps(Util.initializedArray(ms.length, Operation.class));
				for (int i = 0; i < ms.length; i++) {
					job.getOps()[i].setMachine(ms[i]);
					//System.out.println("	machine " + ms[i] + 1);
				}

				for (int i = 0; i < ms.length; i++) {
					job.getOps()[i].setProcTime(serviceTime[job.getJobType()][i]);
					//System.out.println("	service time " + serviceTime[job.getJobType()]);
				}

				return job;
			}
		});

		jobTypeDelay = Util.initializedArray(NUM_JOB_TYPES, SummaryStat.class);
	}
	
	@Override
	public void jobFinished(Job j) {
		super.jobFinished(j);
		nmrTotalJobsCompleted++;
		nmrTypeJobsCompleted[j.getJobType()]++;
	}

	public void addRecord(String s) {
		System.out.println(s);
	}

	public void report() {
		addRecord("nmrTotalJobsCreated " + nmrTotalJobsCreated);
		for (int i = 0; i < NUM_JOB_TYPES; i++) {
			addRecord("type " + i + " nmrJobsCompleted " + nmrTypeJobsCompleted[i]);
		}
		addRecord("nmrTotalJobsCompleted " + nmrTotalJobsCompleted);
		addRecord("");
		
		addRecord("Average total delay in queue");
		double oajtd = (double) 0.0; // Overall average job total delay
		double ajtd; // Average job total delay
		double sumProbs = (double) 0.0;
		for (int i = 0; i < NUM_JOB_TYPES; i++) {
			/*
			 * average job total delay = average delay for job type for each
			 * task times the number of tasks
			 */
			ajtd = (jobTypeDelay[i].mean() * route[i].length);
			addRecord(String.valueOf(ajtd));
			/*
			 * oajtd is a weighted average of the total time a job waits in
			 * queue. Total waits (ojtd) are multiplied by the probability job
			 * being of a particular type. Oajtd would be the typical total wait
			 */
			oajtd += (probDistribJobType[i] - sumProbs) * ajtd;
			sumProbs = probDistribJobType[i];
		}
		addRecord("Overall average job total delay: " + String.valueOf(oajtd));
		/*
		 * Compute the average number in queue, the average utilization, and the
		 * average delay in queue for each station
		 */
		addRecord("\n\nWork     Average Number     Average       Average Delay");
		addRecord("Station    in Queue        Utilization       in queue ");

		HashMap<String, Object> res = new HashMap<String, Object>();
		produceResults(res);
		int i = 0;
		for (WorkStation m : getMachines()) {
			SummaryStat aniq = (SummaryStat) res.get(m.getName() + ".qLen");
			SummaryStat aveMachinesBusy = (SummaryStat) res.get(m.getName() + ".util");
			SummaryStat stationDelay = (SummaryStat) res.get(m.getName() + ".qWait");
			addRecord(String.valueOf(i) + "        " + String.valueOf(aniq.mean()) + "        "
					+ String.valueOf(aveMachinesBusy.mean() / m.numInGroup()) + "        "
					+ String.valueOf(stationDelay.mean()));
			i++;
		}
	}
}
