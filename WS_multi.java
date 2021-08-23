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

public class WS_multi extends Shop{

	private static final double LENGHT_SIM = 10.0;
	
	float meanArrival = 2.0f;
	float meanService = 3.0f;
	
	final static int NUM_JOB_TYPES = 10;
	
	WorkStation route[] = new WorkStation[NUM_JOB_TYPES];
	
	SummaryStat[] jobTypeDelay;
	
	public static void main(String args[]) {
		Simulation s = new Simulation();
		WS_multi store = new WS_multi();
		s.addComponent(store);
		s.setSimulationLength(LENGHT_SIM);
		
		for(int i = 0; i < NUM_JOB_TYPES; i++) {
			store.addMachine(new WorkStation(1));
		}
		
		WorkStation ws[] = store.getMachines();
		
		for(int i = 0; i < NUM_JOB_TYPES; i++) {
			store.route[i] = ws[i];
		}
		
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
				
				jobTypeDelay[job.getJobType()].value(simTime() - job.getArriveTime());
			}
		}, false);
		
		super.init();
		
		addJobSource(new JobSource() {
			@Override
			public Job createNextJob() {
				Job job = new Job(WS_multi.this);
				
				job.setRelDate(simTime() + meanArrival);
				job.setJobType(0);
				
				WorkStation ms = route[job.getJobType()];
				
				job.setOps(Util.initializedArray(1, Operation.class));
				job.getOps()[0].setMachine(ms);
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
