package task_31;

import java.util.ArrayList;
import java.util.Map;

import org.junit.Test;

import jasima.core.random.RandomFactory;
import jasima.core.random.continuous.DblExp;
import jasima.core.random.continuous.DblSequence;
import jasima.core.random.discrete.IntSequence;
import jasima.core.random.discrete.IntUniformRange;
import jasima.core.simulation.SimComponentBase;
import jasima.core.simulation.Simulation;
import jasima.core.util.ConsolePrinter;
import misc.Q_Custom;

public class MM1_failure {

	private static boolean consoleLog = false;
	
	private static final double LENGTH_SIM = 10000.0;
	private static final int RANDOM_FACTORY_SEED = 3;
	private static final int NUM_PRIO = 10;
	private static final int[] arrivalJobs = {10, 30};
	private static final int[] durationServices = {10, 120};
	private static final int NUM_MACHINES = 3;
	private static final double meanArrivalFailures = 60.0;
	private static final double meanDurationFailures = 10.0;
	
	static class MM1 extends SimComponentBase {
		
		private int[] countPriorityJob = new int [NUM_PRIO];
		private int[] countArrivalJob = new int [arrivalJobs[1] - arrivalJobs[0] + 1];
		private int[] countDurationService = new int [(durationServices[1] - durationServices[0]) + 1];
		private int[] countMachineFailure = new int [NUM_MACHINES];
		private int counterArrivalFailure = 0;
		private double meanArrivalFailure = 0.0;
		private Double minArrivalFailure = null;
		private Double maxArrivalFailure = null;
		private int counterDurationFailure = 0;
		private double meanDurationFailure = 0.0;
		private Double minDurationFailure = null;
		private Double maxDurationFailure = null;
		
		private int countJobsCreated = 0;
		private int countJobsCompleted = 0;
		
		private Integer[] occupiedMachine = new Integer [NUM_MACHINES];
		@SuppressWarnings("unchecked")
		private ArrayList<Double>[] disruptedMachine = new ArrayList[NUM_MACHINES];

		private IntSequence priorityJob;
		private IntSequence arrivalJob;
		private IntSequence durationService;
		private IntSequence machineFailure;
		private DblSequence arrivalFailure;
		private DblSequence durationFailure;
		
		private Q_Custom q;
		
		@Override
		public void init() {
			super.init();
			
			for (int i = 0; i < NUM_MACHINES; i++) {
				occupiedMachine[i] = null;
			}
			
			RandomFactory rf = new RandomFactory(getSim(), RANDOM_FACTORY_SEED);
			priorityJob = rf.initRndGen(new IntUniformRange(1, NUM_PRIO), "priorityJob");
			arrivalJob = rf.initRndGen(new IntUniformRange(arrivalJobs[0], arrivalJobs[1]), "arrivalJob");
			durationService = rf.initRndGen(new IntUniformRange(durationServices[0], durationServices[1]), "durationService");
			machineFailure = rf.initRndGen(new IntUniformRange(0, NUM_MACHINES - 1), "machineFailure");
			arrivalFailure = rf.initRndGen(new DblExp(meanArrivalFailures), "arrivalFailure");
			durationFailure = rf.initRndGen(new DblExp(meanDurationFailures), "durationFailure");
			
			q = new Q_Custom(getSim(), true, NUM_PRIO);
			
			for (int i = 0; i < NUM_MACHINES; i++) {
				disruptedMachine[i] = new ArrayList<Double>();
			}
			
			int nextArrivalJob = arrivalJob.nextInt();
			double nextArrivalFailure = arrivalFailure.nextDbl();
			countArrivalJob[nextArrivalJob - arrivalJobs[0]]++;
			meanArrivalFailure = nextArrivalFailure * ++counterArrivalFailure;
			minArrivalFailure = maxArrivalFailure = nextArrivalFailure;
			scheduleIn(nextArrivalJob, getSim().currentPrio(), ()->nextJob());
			scheduleIn(nextArrivalFailure, getSim().currentPrio(), ()->machineFailure());
		}
		
		@Override
		public void produceResults(Map<String, Object> res) {
			super.produceResults(res);
			
			res.put("jobs_created", countJobsCreated);
			res.put("jobs_completed", countJobsCompleted);
			res.put("mean queue time", q.getMeanWait());
			res.put("mean weighted queue time", q.getMeanWeightedTime());
			
			for (int i = 0; i < NUM_MACHINES; i++) {
				res.put("failures in queue for machine " + i, disruptedMachine[i]);
			}
			for (int i = 0; i < countPriorityJob.length; i++) {
				res.put("nmr of priority " + (i + 1), countPriorityJob[i]);
			}
			for (int i = 0; i < countArrivalJob.length; i++) {
				res.put("nmr of job arrival time " + (i + arrivalJobs[0]), countArrivalJob[i]);
			}
			for (int i = 0; i < countDurationService.length; i++) {
				res.put("nmr of service duration time " + (i + durationServices[0]), countDurationService[i]);
			}
			res.put("mean failure arrival time", meanArrivalFailure);
			res.put("min failure arrival time", minArrivalFailure);
			res.put("max failure arrival time", maxArrivalFailure);
			res.put("mean failure duration", meanDurationFailure);
			res.put("min failure duration", minDurationFailure);
			res.put("max failure duration", maxDurationFailure);
		}
		
		void prtLine(String line) {
			if (consoleLog == true) {
				System.out.println(line);
			}
		}
		
		void nextJob() {
			int jobID = countJobsCreated++;
			int jobPrio = priorityJob.nextInt();
			int nextArrival = arrivalJob.nextInt();
			prtLine("new job: " + jobID + "; prio: " + jobPrio + "; arrived at " + getSim().simTime());
			prtLine("	next job in " + nextArrival);
			countPriorityJob[jobPrio - 1]++;
			countArrivalJob[nextArrival - arrivalJobs[0]]++;
			q.put(jobID, jobPrio);
			checkStartService();
			scheduleIn(nextArrival, getSim().currentPrio(), ()->nextJob());
		}
		
		void checkStartService() {
			for (int i = 0; i < NUM_MACHINES; i++) {
				if (occupiedMachine[i] == null) {
					int machineID = i;
					Integer jobID = q.take();
					if (jobID != null) {
						int duration = durationService.nextInt();
						prtLine("	machine: " + machineID + "; job: " + jobID + "; finish in " + duration);
						countDurationService[duration - durationServices[0]]++;
						occupiedMachine[machineID] = jobID;
						scheduleIn(duration, getSim().currentPrio(), ()->finishService(machineID));
					} else {
						prtLine("	machine: " + machineID + ", no job found in queue");
					}
					return;
				}
			}
			prtLine("all machines occupied");
		}
		
		void finishService(int machineID) {
			countJobsCompleted++;
			prtLine("machine: " + machineID + "; job: " + occupiedMachine[machineID] + ", service finished at " + getSim().simTime());
			if (disruptedMachine[machineID].size() == 0) {
				prtLine("	machine available again");
				occupiedMachine[machineID] = null;
				checkStartService();
			} else {
				double failureDuration = disruptedMachine[machineID].remove(0);
				prtLine("	machine failed; finish in: " + failureDuration);
				occupiedMachine[machineID] = -1;
				scheduleIn(failureDuration, getSim().currentPrio(), ()->finishFailure(machineID));
			}
		}
		
		void machineFailure() {
			int machineID = machineFailure.nextInt();
			double nextArrival = arrivalFailure.nextDbl();
			double duration = durationFailure.nextDbl();
			prtLine("new failure; machine: " + machineID + "; duration: " + duration + "; arrived at " + getSim().simTime());
			prtLine("	next failure in " + nextArrival);
			countMachineFailure[machineID]++;
			meanArrivalFailure = (meanArrivalFailure * counterArrivalFailure++ + nextArrival) / counterArrivalFailure;
			if (nextArrival < minArrivalFailure) {
				minArrivalFailure = nextArrival;
			} else if (nextArrival > maxArrivalFailure) {
				maxArrivalFailure = nextArrival;
			}
			meanDurationFailure = (meanDurationFailure * counterDurationFailure++ + duration) / counterDurationFailure;
			if (minDurationFailure == null) {
				minDurationFailure = maxDurationFailure = duration;
			} else if (duration < minDurationFailure) {
				minDurationFailure = duration;
			} else if (duration > maxDurationFailure) {
				maxDurationFailure = duration;
			}
			if (occupiedMachine[machineID] == null) {
				prtLine("	machine idling; failure starting right away");
				occupiedMachine[machineID] = -1;
				scheduleIn(duration, getSim().currentPrio(), ()->finishFailure(machineID));
			} else {
				prtLine("	machine working; failure postponed");
				disruptedMachine[machineID].add(duration);
			}
			scheduleIn(nextArrival, getSim().currentPrio(), ()->machineFailure());
		}
		
		void finishFailure(int machineID) {
			prtLine("machine: " + machineID + "; failure finished at " + getSim().simTime());
			if (disruptedMachine[machineID].size() == 0) {
				prtLine("	no further failures found");
				occupiedMachine[machineID] = null;
				checkStartService();
			} else {
				double duration = disruptedMachine[machineID].remove(0);
				prtLine("	waiting failure found; finish in " + duration);
				scheduleIn(duration, getSim().currentPrio(), ()->finishFailure(machineID));
			}
		}
	}
	
	@Test
	public void EventOriented() {
		
		Simulation sim = new Simulation();
		sim.setSimulationLength(LENGTH_SIM);
		sim.addComponent(new MM1());
		Map<String, Object> res = sim.performRun();
		
		ConsolePrinter.printResults(null,  res);
	}
}
