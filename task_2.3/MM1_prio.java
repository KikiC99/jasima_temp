package task_23;

//task 2.3 queue with priority based on TestShadesOfMM1, class MM1Model

import java.util.Map;

import org.junit.Test;

import jasima.core.random.RandomFactory;
import jasima.core.random.continuous.DblSequence;
import jasima.core.random.discrete.IntEmpirical;
import jasima.core.random.discrete.IntSequence;
import jasima.core.random.discrete.IntUniformRange;
import jasima.core.simulation.SimComponentBase;
import jasima.core.simulation.Simulation;
import jasima.core.util.ConsolePrinter;
import misc.Q_Custom;

public class MM1_prio {
	private static boolean consoleLog = false; //log printed in console for troubleshooting

	private static final double LENGTH_SIM = 20.0; //duration of the simulation in minutes
	private static final int NUM_PRIO = 2; //number of priorities
	private static final double SERVICE_TIME = 4.0; //time in minutes for the duration of services
	private static final int RANDOM_FACTORY_SEED = 41; //seed used for the simulation's random factory
	
	/* The array arrivalDistrib holds the chances in percent for the arrival times of new jobs. The array
	 * arrivalTime holds the corresponding arrival times in minutes which are return by the random double
	 * sequence meanArrival.
	 */
	static double arrivalDistrib[] = { 0.20, 0.40, 0.20, 0.10, 0.10 };
	static int arrivalTime[] = { 1, 2, 3, 4, 5 };
	
	static class MM1 extends SimComponentBase {

		private int numCreated = 0; //keeps count of the number of jobs created
		private int numServed = 0; //keeps count of the number of jobs successfully served
		private DblSequence meanArrival; //random sequence of doubles for the arrival times of jobs
		private IntSequence prioArrival; //random sequence of integers for the job's priority
		private Q_Custom q; //custom queue which takes into account the jobs' priorities
		private Integer currentJob; //integer to define whether the server is currently working
		private double lastServicePiT; //"Point in Time" used to calculate the average working time
		private double serviceTimeWorking; //double which holds the total server's working time
		
		@Override
		public void init() {
			super.init();
			
			//a Random factory is created and used for the arrival time and priority of jobs
			RandomFactory rf = new RandomFactory(getSim(), RANDOM_FACTORY_SEED);
			meanArrival = rf.initRndGen(new IntEmpirical("timeArrivals", arrivalDistrib, arrivalTime));
			prioArrival = rf.initRndGen(new IntUniformRange(1, NUM_PRIO), "prioArrivals");
			
			q = new Q_Custom(getSim()); //The queue is initialized
			
			//the first job is scheduled
			scheduleIn(meanArrival.nextDbl(), getSim().currentPrio(), this::nextJob);
		}
		
		@Override
		public void produceResults(Map<String, Object> res) {
			super.produceResults(res);
			
			//all the necessary statistics are added to the map 'res' to later be printed in the console
			res.put("numCreated", numCreated);
			res.put("numServed", numServed);
			res.put("qMax", q.getMax());
			//in case the server is still working at the end of the simulation the working time is added
			if (currentJob != null) {
				serviceTimeWorking += LENGTH_SIM - lastServicePiT;
			}
			//the server's total working percentage is calculated and added to 'res'
			res.put("relTimeWorking", serviceTimeWorking * 100 / LENGTH_SIM);
		}

		//method to simplify the printing of text in the console
		void prtLine(String line) {
			if (consoleLog == true) {
				System.out.println(line);
			}
		}
		
		void nextJob() {
			int n = numCreated++; //the new job's ID is saved and the job counter increased by one
			int prio = prioArrival.nextInt(); //the job's priority is randomly defined
			prtLine("new " + n + " prio " + prio + " time " + getSim().simTime());
			q.put(n, prio); //the job is added to the queue
			checkStartService(); //it is checked whether the service can immediately begin
			scheduleIn(meanArrival.nextDbl(), getSim().currentPrio(), this::nextJob); //the next job is scheduled
		}

		void checkStartService() {
			if (q.numItems() == 0 || currentJob != null) { //the queue is empty or the server is occupied
				prtLine("	machine occupied; time " + getSim().simTime());
				return; //the service cannot be started and checkStartService is finished
			}
			currentJob = q.take(); //the next job is taken from the queue respecting it's priotity

			prtLine("	job " + currentJob + " started; time " + getSim().simTime());
			lastServicePiT = getSim().simTime(); //the working start point is saved for later calculation
			scheduleIn(SERVICE_TIME, getSim().currentPrio(), this::finishedService); //the service's finish is scheduled
		}
		
		void finishedService() {
			prtLine("	job finished; time " + getSim().simTime());
			//the server's work is finished and so the working time is calculated and added to the total
			serviceTimeWorking += getSim().simTime() - lastServicePiT;
			currentJob = null; //the currentJob is deactivate to symbolize that the server is not working
			numServed++; //the counter for successfully served jobs is increased by one
			checkStartService(); //it is checked whether another job is waiting in the queue
		}
		
	}
	
	@Test
	public void testEventOriented() {
		
		Simulation sim = new Simulation(); //new simulation is created
		sim.setSimulationLength(LENGTH_SIM); //the simulation's length is set
		sim.addComponent(new MM1());
		/* All the statistics produced and at the end returned by the simulation and saved in the map res to later
		 * be printed in the console
		 */
		Map<String, Object> res = sim.performRun(); //the simulation is run and the returned statistics saved in res

		ConsolePrinter.printResults(null, res); //the statistics saved in res are printed in the console
	}
}
