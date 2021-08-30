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

	private static boolean consoleLog = false; //log of jobs and services printed in the console
	private static boolean advancedResults = false; //advanced results for troubleshooting
	
	private static final double LENGTH_SIM = 10000.0; //length of the simulation
	private static final int RANDOM_FACTORY_SEED = 3; //seed to be used for the random factory
	private static final int NUM_PRIO = 10; //number of priorities: 1 to 10
	private static final int[] arrivalJobs = {10, 30}; //min, max arrival time of jobs
	private static final int[] durationServices = {10, 120}; //min, max duration of service
	private static final int NUM_MACHINES = 3; //number of machines running in parallel
	private static final double meanArrivalFailures = 60.0; //mean time of arrival of failures
	private static final double meanDurationFailures = 10.0; //mean time of duration of failures
	
	static class MM1 extends SimComponentBase {
		
		//statistics to check the generation of random numbers
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
		
		private int countJobsCreated = 0; //counter for created jobs
		private int countJobsCompleted = 0; //counter for completed jobs
		
		/* for each machine there is an Integer to hold their current job
		 * null: waiting, -1: failure, > 0: ID of job 
		 */
		private Integer[] occupiedMachine = new Integer [NUM_MACHINES];
		/* if a failure arrives and the machine is still working, the failure's duration is saved in the arrayList
		 * disruptedMachine. For each machine there is an array. Once the machine finishes it's job the failure is
		 * started
		 */
		@SuppressWarnings("unchecked")
		private ArrayList<Double>[] disruptedMachine = new ArrayList[NUM_MACHINES];

		private IntSequence priorityJob; //Sequence of Integers for the jobs' priorities
		private IntSequence arrivalJob; //Sequence of Integers for the jobs' arrival times
		private IntSequence durationService; //Sequence of Integers for the job's service durations
		private IntSequence machineFailure; //Sequence of Integers to define which machine is to fail
		private DblSequence arrivalFailure; //Sequence of Doubles for the failures' arrival times
		private DblSequence durationFailure; //Sequence of Double for the failures' durations
		
		private Q_Custom q; //queue for the jobs
		
		@Override
		public void init() {
			super.init();
			
			for (int i = 0; i < NUM_MACHINES; i++) {
				occupiedMachine[i] = null; //all machines are set to null: waiting
			}
			
			RandomFactory rf = new RandomFactory(getSim(), RANDOM_FACTORY_SEED); //random factory is initialized
			priorityJob = rf.initRndGen(new IntUniformRange(1, NUM_PRIO), "priorityJob");
			arrivalJob = rf.initRndGen(new IntUniformRange(arrivalJobs[0], arrivalJobs[1]), "arrivalJob");
			durationService = rf.initRndGen(new IntUniformRange(durationServices[0], durationServices[1]), "durationService");
			machineFailure = rf.initRndGen(new IntUniformRange(0, NUM_MACHINES - 1), "machineFailure");
			arrivalFailure = rf.initRndGen(new DblExp(meanArrivalFailures), "arrivalFailure");
			durationFailure = rf.initRndGen(new DblExp(meanDurationFailures), "durationFailure");
			
			q = new Q_Custom(getSim(), NUM_PRIO); //queue is initialized. NUM_PRIO is used for the weighted waiting times
			
			for (int i = 0; i < NUM_MACHINES; i++) {
				disruptedMachine[i] = new ArrayList<Double>(); //each array in the arrayList is initialized
			}
			
			int nextArrivalJob = arrivalJob.nextInt(); //the arrival time for the first job is defined
			double nextArrivalFailure = arrivalFailure.nextDbl(); //the arrival time for the first failure is defined
			countArrivalJob[nextArrivalJob - arrivalJobs[0]]++; //statistics
			meanArrivalFailure = nextArrivalFailure * ++counterArrivalFailure; //statistics
			minArrivalFailure = maxArrivalFailure = nextArrivalFailure; //statistics
			scheduleIn(nextArrivalJob, getSim().currentPrio(), ()->nextJob()); //first job is scheduled
			scheduleIn(nextArrivalFailure, getSim().currentPrio(), ()->machineFailure()); //first failure is scheduled
		}
		
		@Override
		public void produceResults(Map<String, Object> res) {
			super.produceResults(res);
			
			//results are added to the map res to be printed in the console
			res.put("jobs_created", countJobsCreated); //number of created jobs
			res.put("jobs_completed", countJobsCompleted); //number of completed jobs
			res.put("mean queue time", q.getMeanWait()); //mean wait time of jobs in queue
			res.put("mean weighted queue time", q.getMeanWeightedTime()); //mean weighted wait time of jobs in queue
			
			//advanced results for troubleshooting
			if (advancedResults == true) {
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
		}
		
		//prints a String in the console; to save space
		void prtLine(String line) {
			if (consoleLog == true) {
				System.out.println(line);
			}
		}
		
		void nextJob() {
			int jobID = countJobsCreated++; //the job's ID is saved and the counter for created jobs increased
			int jobPrio = priorityJob.nextInt(); //the job's priority is defined
			int nextArrival = arrivalJob.nextInt(); //the next job's arrival time is defined
			prtLine("new job: " + jobID + "; prio: " + jobPrio + "; arrived at " + getSim().simTime());
			prtLine("	next job in " + nextArrival);
			countPriorityJob[jobPrio - 1]++;
			countArrivalJob[nextArrival - arrivalJobs[0]]++;
			q.put(jobID, jobPrio); //job is added to the queue
			checkStartService(); //the service is checked for the machines' availability 
			scheduleIn(nextArrival, getSim().currentPrio(), ()->nextJob()); //the next job is scheduled
		}
		
		void checkStartService() {
			for (int i = 0; i < NUM_MACHINES; i++) {
				if (occupiedMachine[i] == null) { //a waiting machine is searched
					int machineID = i; //the waiting machine's ID is saved
					Integer jobID = q.take(); // a job and it's ID is taken from the queue
					if (jobID != null) { //to avoid errors, the ID must not be null
						int duration = durationService.nextInt(); //the service duration is defined
						prtLine("	machine: " + machineID + "; job: " + jobID + "; finish in " + duration);
						countDurationService[duration - durationServices[0]]++;
						occupiedMachine[machineID] = jobID;
						scheduleIn(duration, getSim().currentPrio(), ()->finishService(machineID)); //the service's finish is scheduled
					} else {
						prtLine("	machine: " + machineID + ", no job found in queue");
					}
					return;
				}
			}
			prtLine("all machines occupied");
		}
		
		void finishService(int machineID) {
			countJobsCompleted++; //the counter for completed jobs is increased
			prtLine("machine: " + machineID + "; job: " + occupiedMachine[machineID] + ", service finished at " + getSim().simTime());
			if (disruptedMachine[machineID].size() == 0) { //in case there are no failures for this machine
				prtLine("	machine available again");
				occupiedMachine[machineID] = null; //machine is waiting again
				checkStartService(); //the service is checked for jobs waiting in the queue
			} else { //in case a failure for this machine arrived in the mean time
				double failureDuration = disruptedMachine[machineID].remove(0); //the failure's duration is taken from the array
				prtLine("	machine failed; finish in: " + failureDuration);
				occupiedMachine[machineID] = -1; //the machine is now in failure
				scheduleIn(failureDuration, getSim().currentPrio(), ()->finishFailure(machineID)); //the failure's end is scheduled
			}
		}
		
		void machineFailure() {
			int machineID = machineFailure.nextInt(); //which machine is to fail is defined
			double nextArrival = arrivalFailure.nextDbl(); //the next failure's arrival time is defined
			double duration = durationFailure.nextDbl(); //the failure's duration is defined
			prtLine("new failure; machine: " + machineID + "; duration: " + duration + "; arrived at " + getSim().simTime());
			prtLine("	next failure in " + nextArrival);
			//advanced statistics
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
			if (occupiedMachine[machineID] == null) { //in case the machine is waiting
				prtLine("	machine idling; failure starting right away");
				occupiedMachine[machineID] = -1; //the failure starts right away
				scheduleIn(duration, getSim().currentPrio(), ()->finishFailure(machineID)); //the failure's end is scheduled
			} else { //in case the machine is working
				prtLine("	machine working; failure postponed");
				disruptedMachine[machineID].add(duration); //the failure's duration is added to the corresponding array
			}
			scheduleIn(nextArrival, getSim().currentPrio(), ()->machineFailure()); //the next failure is scheduled
		}
		
		void finishFailure(int machineID) {
			prtLine("machine: " + machineID + "; failure finished at " + getSim().simTime());
			if (disruptedMachine[machineID].size() == 0) { //in case there are no further waiting failures
				prtLine("	no further failures found");
				occupiedMachine[machineID] = null; //the machine is waiting again
				checkStartService(); //the service is checked for jobs waiting in the queue
			} else { //in case further failures are found
				double duration = disruptedMachine[machineID].remove(0); //the next failure's duration is removed from the array
				prtLine("	waiting failure found; finish in " + duration);
				scheduleIn(duration, getSim().currentPrio(), ()->finishFailure(machineID)); //the failure's end is scheduled
			}
		}
	}
	
	@Test
	public void EventOriented() {
		
		Simulation sim = new Simulation(); //a new simulation is initialized
		sim.setSimulationLength(LENGTH_SIM); //the simulation's length is set
		sim.addComponent(new MM1()); //the class MM1 is added
		Map<String, Object> res = sim.performRun(); //the simulation is run and it's statistics saved in the map res
		
		ConsolePrinter.printResults(null,  res); //the results contained in res are printed in the console
	}
}
