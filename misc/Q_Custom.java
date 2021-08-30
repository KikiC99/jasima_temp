package misc;

import java.util.ArrayList;

import jasima.core.simulation.Simulation;

public class Q_Custom {

	private class Job {
		int id;
		Integer prio; //null: no priority
		double entryTime; //or waiting time in listAllCompletedJobs
		private Job (int id, Integer prio, double entryTime) {
			this.id = id;
			this.prio = prio;
			this.entryTime = entryTime;
		}
	}
	
	Simulation qSim;
	private ArrayList<Job> q = new ArrayList<Job>(); //queue
	/* ArrayList to hold all jobs after they have been completed and removed from the queue
	 * with they waiting time in place of the entry time
	 */
	private ArrayList<Job> listAllCompletedJobs = new ArrayList<Job>();
	private int qMax = 0; //max size of queue
	private boolean FIFO = true; //true = FIFO; false = LIFO for queue
	private Integer maxPrio = null; //highest priority; needed for weighted wait time
	
	public Q_Custom (Simulation sim) {
		qSim = sim;
	}
	
	public Q_Custom (Simulation sim, boolean fifo) {
		qSim = sim;
		FIFO = fifo;
	}
	
	public Q_Custom (Simulation sim, int prio) {
		qSim = sim;
		if (prio > 0) {
			maxPrio = prio;
		}
	}
	
	public Q_Custom (Simulation sim, boolean fifo, int prio) {
		qSim = sim;
		FIFO = fifo;
		if (prio > 0) {
			maxPrio = prio;
		}
	}
	
	//to put a new job in the queue without a priority
	public boolean put(int id) {
		/* if all the jobs already in the queue have a priority,
		 * new jobs will only be added if they also have a priority
		 */
		if (q.size() > 0 && checkPrio() == true) {
			return false; //all others jobs have a priority; new job cannot be added
		}
		addNewJob(id, null); //new job is added to the queue
		return true;
	}
	
	//to put a new job in the queue with a priority
	public boolean put(int id, int prio) {
		if (prio < 1) { //checks if the priority is positive
			return false;
		}
		addNewJob(id, prio); //new job is added to the queue
		return true;
	}
	
	/* checks whether all jobs in the queue have a priority.
	 * If one does not a false result is returned.
	 * Is needed since a job's priority can only be taken into account,
	 * if all other jobs also have a priority
	 */
	private boolean checkPrio() {
		for (int i = 0; i < q.size(); i++) {
			if (q.get(i).prio == null) {
				return false;
			}
		}
		return true;
	}
	
	//to add a new job to the queue
	private void addNewJob(int id, Integer prio) {
		Job job = new Job(id, prio, qSim.simTime()); //new job is created
		q.add(job); //job is added to the queue
		checkMaxQ(); //checks whether there is a new queue max
	}
	
	//checks whether there is a new queue max
	private void checkMaxQ() {
		if (q.size() > qMax) {
			qMax++;
		}
	}
	
	//to take a job from the queue
	public Integer take() {
		if (q.size() == 0) {
			return null; //in case the queue is empty
		}
		return searchJob().id; //search for a job and return it's ID
	}
	
	/* When searching for a job in the queue priority and arrival time
	 * are taken into account. Only if all jobs have a priority (checkPrio() == true)
	 * will their priority be respected. Otherwise the first or the last job in the queue
	 * are taken, depending on whether FIFO is LIFO is active
	 */
	private Job searchJob() {
		if (checkPrio() == true) {
			return searchPrio(); //all jobs have priority
		} else if (FIFO == true) {
			return removeJob(0); //FIFO: remove job in first position
		}
		return removeJob(q.size() - 1); //LIFO: remove job in last position
	}
	
	// searches for a job taking priority and FIFO or LIFO into account.
	private Job searchPrio() {
		Integer minPrio = null;
		Integer row = null;
		if (FIFO == true) {
			for (int i = 0; i < q.size(); i++) {
				if (minPrio == null || q.get(i).prio < minPrio) {
					minPrio = q.get(i).prio;
					row = i;
				}
			}
		} else {
			for (int i = q.size() - 1; i >= 0; i--) {
				if (minPrio == null || q.get(i).prio < minPrio) {
					minPrio = q.get(i).prio;
					row = i;
				}
			}
		}
		return removeJob(row); //remove found job
	}
	
	//removes the job from the queue
	private Job removeJob (int row) {
		Job job = q.get(row); //job is saved before it's deleted
		q.remove((int)row); //job is removed from queue, effectively deleting it
		job.entryTime = qSim.simTime() - job.entryTime; //job's wait time is calculated
		listAllCompletedJobs.add(job); //job is moved to the array of completed jobs
		return job; //job is returned all the way to take();
	}
	
	public int numItems() {
		return q.size();
	}
	
	public int getMax() {
		return qMax;
	}
	
	/* All jobs are added to the array of completed jobs after they are removed 
	 * from the queue. Their entry time is substituted with their waiting time.
	 * This way there is always access to all previous waiting times
	 */
	public Double getCompletedJobWaitTime(int id) {
		Job job;
		for (int i = 0; i < listAllCompletedJobs.size(); i++) {
			job = listAllCompletedJobs.get(i);
			if (job.id == id) {
				return job.entryTime; //returns the specific job's wait time
			}
		}
		return null;
	}
	
	/* All wait times are only collected during the simulation in an array.
	 * To access the max or mean values they first need to be calculated.
	 * getMaxWait goes through the array once and looks for the highest wait time.
	 * getMeanWait goes through the array and sums up all the waiting times
	 * and then divides them with the number of completed jobs. The result is returned.
	 * getMeanWeightedTimes does the same as getMeanWait but multiplies every late time
	 * according to the job's priority, inverting it (1 -> 10, 10 -> 1)
	 */
	public double getMaxWait() {
		double maxWaitTime = 0.0;
		double waitTime;
		for (int i = 0; i < listAllCompletedJobs.size(); i++) {
			waitTime = listAllCompletedJobs.get(i).entryTime;
			if (waitTime > maxWaitTime) {
				maxWaitTime = waitTime;
			}
		}
		return maxWaitTime;
	}
	
	public double getMeanWait() {
		double meanWaitTime = 0.0;
		double waitTime;
		double totalWaitTime = 0;
		for (int i = 0; i < listAllCompletedJobs.size(); i++) {
			waitTime = listAllCompletedJobs.get(i).entryTime; //wait time is fetched
			totalWaitTime += waitTime; //wait time is added to total wait time
		}
		if (totalWaitTime > 0) { //check to ensure null or a real number is returned
			meanWaitTime = totalWaitTime / listAllCompletedJobs.size();
		}
		return meanWaitTime;
	}
	
	public double getMeanWeightedTime() {
		if (maxPrio == null) {
			return 0.0;
		}
		double meanWeightedWaitTime = 0.0;
		double waitTime;
		int prio;
		double weightedWaitTime;
		double totalWeightedWaitTime = 0;
		for (int i = 0; i < listAllCompletedJobs.size(); i++) {
			waitTime = listAllCompletedJobs.get(i).entryTime; //wait time is fetched
			prio = listAllCompletedJobs.get(i).prio; //priority is fetched
			weightedWaitTime = waitTime * ((prio - maxPrio - 1) * (-1)); //conversion
			totalWeightedWaitTime += weightedWaitTime; //wait time is added to total
		}
		if (totalWeightedWaitTime > 0) { //check to ensure null or a real number is returned
			meanWeightedWaitTime = totalWeightedWaitTime / listAllCompletedJobs.size();
		}
		return meanWeightedWaitTime;
	}
	
	public void printQ() {
		System.out.println("");
		System.out.println("Qprio");
		System.out.println("");
		System.out.println("id prio");
		for (int i = 0; i < q.size(); i++) {
			System.out.println(q.get(i).id + " " + q.get(i).prio);
		}
		System.out.println("");
	}
}
