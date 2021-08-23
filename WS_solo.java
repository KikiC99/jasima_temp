package task_22;

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

public class WS_solo extends Shop{
	
	private static double LENGHT_SIM = 100.0;
	
	float meanArrival = 2.0f;
	float meanService = 3.0f;
	
	WorkStation ws;
	
	SummaryStat[] jobTypeDelay;
	
	public static void main(String args[]) {
		Simulation s = new Simulation();
		WS_solo store = new WS_solo();
		s.addComponent(store);
		s.setSimulationLength(LENGHT_SIM);
		
		WorkStation machine = new WorkStation(1);
		
		store.addMachine(machine);
		
		store.ws = machine;
		
		s.init();
		s.beforeRun();
		s.run();
		s.afterRun();
		s.done();
		store.report();
	}
	
	public void init() {
		installMachineListener(new WorkStationListener() {
			
			@Override
			public void operationStarted(WorkStation m, PrioRuleTarget b, int oldSetupState, int newSetupState, double setupTime) {
				assert b.numJobsInBatch() == 1;
				Job job = b.job(0);
				
				jobTypeDelay[0].value(simTime() - job.getArriveTime());
			}
		}, false);
		
		super.init();
		
		addJobSource(new JobSource() {
			@Override
			public Job createNextJob() {
				Job job = new Job(WS_solo.this);
				
				job.setRelDate(simTime() + meanArrival);
				
				job.setOps(Util.initializedArray(1, Operation.class));
				job.getOps()[0].setMachine(ws);
				job.getOps()[0].setProcTime(meanService);
				
				return job;
			}
		});
		
		jobTypeDelay = Util.initializedArray(1, SummaryStat.class); 
	}
	
	@Override
	public void done() {
		super.done();
	}
	
	public void addRecord(String s) {
		System.out.println(s);
	}
	
	public void report() {
		addRecord("Average delay in queue");
		addRecord(String.valueOf(jobTypeDelay[0].mean()));
		addRecord("Max delay in queue");
		addRecord(String.valueOf(jobTypeDelay[0].max()));
	}

}