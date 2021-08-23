package task_22;

/* task 2.2 FIFO vs LIFO based on TestShadesOfMM1, class MM1Model
 * 
 */

import java.util.Map;

import org.junit.Test;

import jasima.core.random.RandomFactory;
import jasima.core.random.continuous.DblExp;
import jasima.core.random.continuous.DblSequence;
import jasima.core.random.discrete.IntSequence;
import jasima.core.random.discrete.IntUniformRange;
import jasima.core.simulation.SimComponentBase;
import jasima.core.simulation.Simulation;
import jasima.core.simulation.generic.Q;
import jasima.core.util.ConsolePrinter;

public class MM1_FIFO {
	private static boolean consoleLog = true;
	
	private static final double LENGTH_SIM = 100.0; //duration of the simulation in minutes
	private static final int NUM_JOB_TYPES = 10; //number of different types of jobs
	private static final double MEAN_TIME = 10.0; //average time in minutes for both jobs and services
	private static final int RANDOM_FACTORY_SEED = 23; //seed used for the simulation's random factory
	
	static class MM1 extends SimComponentBase {

		private int countJob = 0; //keeps count of the number of jobs created
		private int countService = 0; //keeps count of the number of services created
		private int countServicedJobs = 0; //keeps count of the number of jobs successfully serviced
		
		/* Jobs and services are created randomly and independently from each other and assigned a random type.
		 * Once a service finds a job of the same time it immediately services it, without any processing time.
		 * If a service does not find a matching job upon arrival it waits for one to be created.
		 * */
		private DblSequence meanArrival; //random sequence of doubles for the arrival time of jobs
		private DblSequence meanService; //random sequence of doubles for the arrival time of services
		private IntSequence typeArrival; //random sequence of integers for the type of job
		private IntSequence typeService; //random sequence of integers for the type of service
		
		/* There are two queue arrays, each with, in this case, 10 queues. 
		 * For every type of job there is a queue, so that a new service can check the availability of jobs
		 * according to the service's individual type. For all job queues there is an analog service queue.
		 * In case a new service does not find an available job in the specific queue, it adds itself to the
		 * analog service queue and waits for a job of matching type. Once such a job arrives and finds a waiting
		 * service of the same type, it immediately initializes the servicing process.
		 * */
		@SuppressWarnings("unchecked")
		private Q<Integer>[] qJob = new Q[NUM_JOB_TYPES]; //queue for jobs
		@SuppressWarnings("unchecked")
		private Q<Integer>[] qService = new Q[NUM_JOB_TYPES]; //queue for services
		
		@Override
		public void init() {
			super.init();
			meanArrival = initRndGen(new DblExp(MEAN_TIME), "arrivals"); //random sequence for jobs is initialized
			meanService = initRndGen(new DblExp(MEAN_TIME), "services"); //random sequence for services is initialized
			typeArrival = initRndGen(new IntUniformRange(0, NUM_JOB_TYPES - 1), "typeArrivals");
			typeService = initRndGen(new IntUniformRange(0, NUM_JOB_TYPES - 1), "typeServices");
					
			for(int i = 0; i < NUM_JOB_TYPES; i++) { //both queues are initialized
				qJob[i] = new Q<>();
				qService[i] = new Q<>();
			}
			
			scheduleIn(meanArrival.nextDbl(), getSim().currentPrio(), this::nextJob); //first job is scheduled
			scheduleIn(meanService.nextDbl(), getSim().currentPrio(), this::nextService); //first service is scheduled
		}
		
		@Override
		public void produceResults(Map<String, Object> res) {
			super.produceResults(res);
			res.put("numJobs", countJob);
			res.put("numServices", countService);
			res.put("numServicedJobs", countServicedJobs);
		}
		
		void prtLine(String line) {
			if (consoleLog == true) {
				System.out.println(line);
			}
		}
		
		void nextJob() {
			int qNmr = typeArrival.nextInt();
			int jobID = countJob++;
			prtLine("new job     " + jobID + " type " + qNmr + " at " + getSim().simTime());
			qJob[qNmr].tryPut(jobID);
			if (qService[qNmr].numItems() > 0) {
				int service = qService[qNmr].tryTake();
				prtLine("			waiting service " + service + " found");
				serviceProcess(qNmr);
			} else {
				prtLine("			no waiting service found");
			}
			scheduleIn(meanArrival.nextDbl(), getSim().currentPrio(), this::nextJob);
		}

		void nextService() {
			int qNmr = typeService.nextInt();
			int serviceID = countService++;
			prtLine("new service " + serviceID + " type " + qNmr + " at " + getSim().simTime());
			if (qJob[qNmr].numItems() > 0) {
				prtLine("			waiting job found");
				serviceProcess(qNmr);
			} else {
				prtLine("			service added to queue");
				qService[qNmr].tryPut(countService);
			}
			scheduleIn(meanService.nextDbl(), getSim().currentPrio(), this::nextService); //()->nextService(xyz))
		}
		
		void serviceProcess(int qNmr) {
			int job = qJob[qNmr].tryTake();
			prtLine("			job " + job + " processed");
			countServicedJobs++;
		}
	}
	
	@Test
	public void testEventOriented() {
		
		Simulation sim = new Simulation();
		sim.setSimulationLength(LENGTH_SIM);
		RandomFactory rf = new RandomFactory(sim, RANDOM_FACTORY_SEED);
		sim.setRndStreamFactory(rf);
		sim.addComponent(new MM1());
		Map<String, Object> res = sim.performRun();

		ConsolePrinter.printResults(null, res);
	}
}
