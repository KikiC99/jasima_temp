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
	private static boolean consoleLog = true; //log printed in console for troubleshooting
	
	private static boolean FIFO = true; //if false, LIFO
	private static final double LENGTH_SIM = 100.0; //duration of the simulation in minutes
	private static final int NUM_JOB_TYPES = 10; //number of different types of jobs
	private static final double MEAN_TIME = 10.0; //average time in minutes for both jobs and services
	private static final int RANDOM_FACTORY_SEED = 28; //seed used for the simulation's random factory
	
	static class MM1 extends SimComponentBase {

		private int countJob = 0; //keeps count of the number of jobs created
		private int countService = 0; //keeps count of the number of services created
		private int countServicedJobs = 0; //keeps count of the number of jobs successfully serviced
		
		/* Jobs and services are created randomly and independently from each other and assigned a random type.
		 * Once a service finds a job of the same time it immediately services it, without any processing time.
		 * If a service does not find a matching job upon arrival it waits for one to be created.
		 * */
		private DblSequence arrivalJob; //random sequence of doubles for the arrival time of jobs
		private DblSequence arrivalService; //random sequence of doubles for the arrival time of services
		private IntSequence typeJob; //random sequence of integers for the type of job
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
			
			//random sequences for types and arrival times are initialized
			arrivalJob = initRndGen(new DblExp(MEAN_TIME), "jobs");
			arrivalService = initRndGen(new DblExp(MEAN_TIME), "services");
			typeJob = initRndGen(new IntUniformRange(0, NUM_JOB_TYPES - 1), "typeJobs");
			typeService = initRndGen(new IntUniformRange(0, NUM_JOB_TYPES - 1), "typeServices");
			
			 //both queues are initialized
			for(int i = 0; i < NUM_JOB_TYPES; i++) {
				qJob[i] = new Q<>();
				qService[i] = new Q<>();
			}
			
			//the first job and first service are scheduled
			scheduleIn(arrivalJob.nextDbl(), getSim().currentPrio(), this::nextJob);
			scheduleIn(arrivalService.nextDbl(), getSim().currentPrio(), this::nextService);
		}
		
		//all collected statistics are added to the map res to later be printed
		@Override
		public void produceResults(Map<String, Object> res) {
			super.produceResults(res);
			res.put("numJobs", countJob);
			res.put("numServices", countService);
			res.put("numServicedJobs", countServicedJobs);
		}
		
		//method to simplify the printing of text in the console
		void prtLine(String line) {
			if (consoleLog == true) {
				System.out.println(line);
			}
		}
		
		void nextJob() {
			int qNmr = typeJob.nextInt(); //the new job's type is randomly defined
			int jobID = countJob++; //the job's ID is saved and the job counter increased by one
			prtLine("new job     " + jobID + " type " + qNmr + " at " + getSim().simTime());
			qJob[qNmr].tryPut(jobID); //the job is added to the corresponding queue
			/* For every job queue there is a corresponding service queue. Every new job looks in it's service queue
			 * for a waiting service, based on the job's type. If it finds a service, the service is removed from the
			 * queue and the servicing process is immediately started. If the job does not find a waiting service
			 * nothing further needs to be done. Finally the next job is scheduled at a random time.
			 */
			if (qService[qNmr].numItems() > 0) { //waiting service is found in corresponding queue
				int service = qService[qNmr].tryTake(); //service is removed from queue
				prtLine("			waiting service " + service + " found");
				serviceProcess(qNmr); //the job's servicing process is started
			} else {
				prtLine("			no waiting service found");
			}
			scheduleIn(arrivalJob.nextDbl(), getSim().currentPrio(), this::nextJob); //the next job is scheduled
		}

		void nextService() {
			int qNmr = typeService.nextInt(); //the new service's type is randomly defined
			int serviceID = countService++; //the service's ID is saved and the service counter increased by one
			prtLine("new service " + serviceID + " type " + qNmr + " at " + getSim().simTime());
			/* Every new service looks in the queue corresponding to it's type for a waiting job. If it finds one the
			 * servicing process is immediately initialized. If the service does not find a waiting queue then it adds
			 * itself to the correct service queue and waits to be called up by a future job. Finally the next service
			 * is scheduled at a random time. 
			 */
			if (qJob[qNmr].numItems() > 0) { //waiting job is found in corresponding queue
				prtLine("			waiting job found");
				serviceProcess(qNmr); //the job's servicing process is started
			} else {
				prtLine("			service added to queue");
				qService[qNmr].tryPut(countService); //the service is added to the corresponding queue
			}
			scheduleIn(arrivalService.nextDbl(), getSim().currentPrio(), this::nextService); //the next service is scheduled
		}
		
		void serviceProcess(int qNmr) {
			int job;
			//depending if FIFO or LIFO is active the right take call is used and the job removed from the queue 
			if (FIFO == true) {
				job = qJob[qNmr].tryTake();
			} else {
				job = qJob[qNmr].tryTakeLast();
			}
			prtLine("			job " + job + " processed");
			countServicedJobs++; //the counter for serviced jobs is increased by one
		}
	}
	
	@Test
	public void testEventOriented() {
		
		Simulation sim = new Simulation(); //new simulation is created
		sim.setSimulationLength(LENGTH_SIM); //the simulation's length is set
		/* A random factory is needed to create the random number sequences for the types and arrival times of jobs
		 * and services. The previously defined random seed is used.
		 */
		RandomFactory rf = new RandomFactory(sim, RANDOM_FACTORY_SEED);
		sim.setRndStreamFactory(rf);
		sim.addComponent(new MM1());
		/* All the statistics produced and at the end returned by the simulation and saved in the map res to later
		 * be printed in the console
		 */
		Map<String, Object> res = sim.performRun(); //the simulation is run and the returned statistics saved in res

		ConsolePrinter.printResults(null, res); //the statistics saved in res are printed in the console
	}
}
