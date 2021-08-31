package task_24;

import java.util.ArrayList;
import java.util.Map;

import org.junit.Test;

import jasima.core.random.RandomFactory;
import jasima.core.random.discrete.IntSequence;
import jasima.core.random.discrete.IntUniformRange;
import jasima.core.simulation.SimComponentBase;
import jasima.core.simulation.Simulation;
import jasima.core.util.ConsolePrinter;
import misc.Q_Custom;

public class MM1_pallet {
	
	private static boolean consoleLog = true;
	
	private static final double LENGTH_SIM = 200.0;
	private static final int RANDOM_FACTORY_SEED = 76;
	private static final double JOB_INTERVAL = 60.0;
	private static final int JOB_LIMIT = 10;
	private static final int[] JOB_LOAD = {20, 50};
	private static final int TRUCK_CAPACITY = 12;
	private static final double PALLET_LOADING_TIME = 1.0;
	private static final double TRUCK_PREPARATION = 10.0;
	private static final double TRUCK_SECURING = 20.0;
	
	static class MM1 extends SimComponentBase {
		
		private ArrayList<Double> loadingTimes = new ArrayList<Double>(); //array to save all loading times
		private Q_Custom q;
		private int jobsCreated = 0;
		private int jobsFinished = 0;
		private Integer loaderOccupation = null; //null: waiting; >= 0: job id
		private IntSequence jobLoad; //returns random number of pallets to be loaded
		
		@Override
		public void init() {
			super.init();
			
			RandomFactory rf = new RandomFactory(getSim(), RANDOM_FACTORY_SEED);
			jobLoad = rf.initRndGen(new IntUniformRange(JOB_LOAD[0], JOB_LOAD[1]), "jobLoad");
			
			q = new Q_Custom(getSim());
			
			scheduleIn(getSim().simTime(), getSim().currentPrio(), ()->nextJob()); //first job is scheduled
		}
		
		void nextJob() {
			int jobID = jobsCreated++; //job id is saved
			prtLine("next job: " + jobID + " at: " + getSim().simTime());
			q.put(jobID); //job is added to queue
			startLoading(); //start loading trucks if possible
			if (jobsCreated <= JOB_LIMIT) { //only if the job limit has not been passed
				scheduleIn(JOB_INTERVAL, getSim().currentPrio(), ()->nextJob()); //next job is scheduled
			}
		}
		
		void startLoading() {
			if (loaderOccupation != null) { //if loader is occupied loading cannot start
				prtLine("	loader occupied");
				return;
			}
			int jobID = q.take(); //job is removed from queue and it's id saved
			loaderOccupation = jobID; //the loader's occupation is changed to symbol it is working
			int load = jobLoad.nextInt(); //the number of pallets to be loaded is defined
			//TRUCK_CAPACITY + 1 in case load = 33
			int numTrucks = (int)(load / (TRUCK_CAPACITY + 1)) + 1; //number of necessary trucks is defined
			//loading duration is calculated taking into account the number of pallets and number of trucks
			double duration = load * PALLET_LOADING_TIME + (TRUCK_PREPARATION + TRUCK_SECURING) * numTrucks;
			prtLine("	start loading job: " + jobID + " load: " + load + " duration: " + duration + " trucks: " + numTrucks);
			loadingTimes.add(duration); //loading time is added to the array for later
			scheduleIn(duration, getSim().currentPrio(), ()->finishLoading()); //loading finish is calculated
		}
		
		void finishLoading() {
			jobsFinished++;
			loaderOccupation = null; //loader is put to waiting again
			prtLine("finish loading at: " + getSim().simTime());
			if (q.numItems() > 0) { //if another job is waiting in the queue start the next loading
				//only call startLoading() if you are sure there is a job in the queue, otherwise q.take() returns null
				startLoading();
			}
		}
		
		void prtLine(String line) {
			if (consoleLog == true) {
				System.out.println(line);
			}
		}
		
		@Override
		public void produceResults(Map<String, Object> res) {
			super.produceResults(res);
			
			res.put("trucks arrived", jobsCreated);
			res.put("trucks loaded", jobsFinished);
			res.put("mean wait time", q.getMeanWait());
			
			//the mean loading time is only calculated at the end with the array loadingTimes
			double totalLoadingTimes = 0.0;
			int numLoadingTimes = loadingTimes.size();
			for (int i = 0; i < numLoadingTimes; i++) {
				totalLoadingTimes += loadingTimes.get(i);
			}
			double meanLoadingTime = totalLoadingTimes / numLoadingTimes;
			res.put("mean loading time", meanLoadingTime);
			
			boolean overTime = false;
			if (loaderOccupation != null) {
				overTime = true;
			}
			res.put("overtime: ", overTime);
		}
	}
	
	@Test
	public void eventOriented() {
		
		Simulation sim = new Simulation();
		sim.setSimulationLength(LENGTH_SIM);
		sim.addComponent(new MM1());
		Map<String, Object> res = sim.performRun();
		ConsolePrinter.printResults(null,  res);
	}
}
