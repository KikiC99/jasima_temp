package misc;

import java.util.ArrayList;

import jasima.core.simulation.Simulation;

public class Q_Custom {

	private class Job {
		int id;
		Integer prio;
		double entryTime;
		private Job (int id, Integer prio, double entryTime) {
			this.id = id;
			this.prio = prio;
			this.entryTime = entryTime;
		}
	}
	
	Simulation qSim;
	ArrayList<Job> q = new ArrayList<Job>();
	private int qMax = 0;
	private double maxWaitTime = 0;
	private double meanWaitTime = 0;
	private int finishedJobs = 0;
	private boolean FIFO;
	
	public Q_Custom (Simulation sim) {
		qSim = sim;
		FIFO = true;
	}
	
	public Q_Custom (Simulation sim, boolean fifo) {
		qSim = sim;
		FIFO = fifo;
	}
	
	private void addWaitTime(double waitTime) {
		meanWaitTime = (meanWaitTime * finishedJobs++ + waitTime) / (finishedJobs);
		if (waitTime > maxWaitTime) {
			maxWaitTime = waitTime;
		}
	}
	
	private boolean checkPrio() {
		for (int i = 0; i < q.size(); i++) {
			if (q.get(i).prio == null) {
				return false;
			}
		}
		return true;
	}
	
	private Job finishJob (int row) {
		Job job = q.get(row);
		q.remove((int)row);
		addWaitTime(qSim.simTime() - job.entryTime);
		return job;
	}
	
	private Job searchJob() {
		if (checkPrio() == true) {
			return searchPrio();
		} else if (FIFO == true) {
			return finishJob(0);
		}
		return finishJob(q.size() - 1);
	}
	
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
		return finishJob(row);
	}
	
	public void checkMaxQ() {
		if (q.size() > qMax) {
			qMax++;
		}
	}
	
	public void put(int id) {
		Job newJob = new Job(id, null, qSim.simTime());
		q.add(newJob);
		checkMaxQ();
	}
	
	public void put(int id, int prio) {
		Job newJob = new Job(id, prio, qSim.simTime());
		q.add(newJob);
		checkMaxQ();
	}
	
	public Integer take() {
		if (q.size() == 0) {
			return null;
		}
		return searchJob().id;
	}
	
	public int numItems() {
		return q.size();
	}
	
	public int getMax() {
		return qMax;
	}
	
	public double getMaxWait() {
		return maxWaitTime;
	}
	
	public double getMeanWait() {
		return meanWaitTime;
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
