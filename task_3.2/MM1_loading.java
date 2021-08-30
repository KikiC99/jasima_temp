package task_32;

import java.util.Map;

import org.junit.Test;

import jasima.core.random.RandomFactory;
import jasima.core.random.continuous.DblExp;
import jasima.core.random.continuous.DblSequence;
import jasima.core.random.discrete.IntConst;
import jasima.core.simulation.SimComponentBase;
import jasima.core.simulation.Simulation;
import jasima.core.util.ConsolePrinter;
import misc.Q_Custom;

public class MM1_loading {
	
	private static boolean consoleLog = false;
	
	private static final double LENGTH_SIM = 100.0;
	private static final int RANDOM_FACTORY_SEED = 33;
	private static final int NUM_LOADING_VEHICLES = 1;
	private static final double MEAN_TRUCK_ARRIVAL_TIME = 3.75;
	private static final double MEAN_TRUCK_LOADING_TIME = 6.0;
	private static final int MEAN_TRUCK_LOADING_TIME_AUTO = 2;
	
	static class MM1 extends SimComponentBase {
		
		private int numTrucksCreated = 0;
		private int numTrucksLoaded = 0;
		private double totalCosts = 0.0;
		
		private DblSequence truckArrivalTime;
		private DblSequence truckLoadingTime;
		
		private Q_Custom q;
		
		//null: no job; >= 0: truck ID
		private Integer[] loadingVehiclesOccupation = new Integer[NUM_LOADING_VEHICLES];
		
		@Override
		public void init() {
			super.init();
			
			q = new Q_Custom(getSim());
			
			RandomFactory rf = new RandomFactory(getSim(), RANDOM_FACTORY_SEED);
			truckArrivalTime = rf.initRndGen(new DblExp(MEAN_TRUCK_ARRIVAL_TIME), "truckArrivalTime");
			//truckLoadingTime = rf.initRndGen(new DblExp(MEAN_TRUCK_LOADING_TIME), "truckLoadingTime");
			truckLoadingTime = rf.initRndGen(new IntConst(MEAN_TRUCK_LOADING_TIME_AUTO), "truckLoadingTime");
			
			for (int i = 0; i < NUM_LOADING_VEHICLES; i++) {
				loadingVehiclesOccupation[i] = null;
			}
			
			//first truck is scheduled
			scheduleIn(truckArrivalTime.nextDbl(), getSim().currentPrio(), ()->nextTruck());
		}
		
		void nextTruck() {
			int truckID = numTrucksCreated++;
			prtLine("new truck; id: " + truckID + "; at: " + getSim().simTime());
			prtLine("	put in queue: " + q.put(truckID));
			//possibility of loading the truck is checked
			checkTruckLoading();
			//next truck is scheduled
			scheduleIn(truckArrivalTime.nextDbl(), getSim().currentPrio(), ()->nextTruck());
		}
		
		void checkTruckLoading() {
			//checkOccupationMachines returns: ID of available machine; or null
			Integer machineID = checkOccupationMachines();
			if (machineID == null) { //in case all machines are occupied
				prtLine("	all machines occupied");
				return;
			}
			//q.take returns: ID of truck in queue; or null
			Integer truckID = q.take();
			if (truckID == null) { //the queue is empty
				prtLine("	no trucks in queue");
				return;
			}
			//calculation of the cost, based on the trucks waiting time in the queue
			Double waitTimeTruck = q.getCompletedJobWaitTime((int)truckID);
			prtLine("	start loading; truck: " + truckID + "; machine: " + machineID);
			prtLine("	wait time truck: " + waitTimeTruck);
			int cost = 0;
			if (waitTimeTruck > 0.0) {
				cost = ((int)(waitTimeTruck / 60) + 1) * 40;
				totalCosts += cost;
			}
			prtLine("	cost: " + cost);
			//the truck's ID is saved in the machine's array to signal that the machine is loading
			loadingVehiclesOccupation[machineID] = truckID;
			//finish of the loading is scheduled
			scheduleIn(truckLoadingTime.nextDbl(), getSim().currentPrio(), ()->finishTruckLoading(machineID));
		}
		
		void finishTruckLoading(int machineID) {
			prtLine("finish loading; machine: " + machineID + "; at: " + getSim().simTime());
			numTrucksLoaded++;
			//the machine is freed up again and signaled as waiting
			loadingVehiclesOccupation[machineID] = null;
			//check if another truck is waiting in the queue
			checkTruckLoading();
		}
		
		Integer checkOccupationMachines() {
			//searches for a free machine. In loadingVehiclesOccupation null signals the machine is waiting
			for (int i = 0; i < NUM_LOADING_VEHICLES; i++) {
				if (loadingVehiclesOccupation[i] == null) {
					//a waiting machine is found and it's ID is returned
					return i;
				}
			}
			//all machines are occupied
			return null;
		}
		
		void prtLine(String line) {
			if (consoleLog == true) {
				System.out.println(line);
			}
		}
		
		@Override
		public void produceResults(Map<String, Object> res) {
			super.produceResults(res);
			
			res.put("total costs", totalCosts);
			res.put("num trucks created", numTrucksCreated);
			res.put("num trucks loaded", numTrucksLoaded);
			res.put("mean wait time", q.getMeanWait());
		}
	}
	
	@Test
	public void eventOriented() {
		
		Simulation sim = new Simulation();
		sim.setSimulationLength(LENGTH_SIM);
		sim.addComponent(new MM1());
		Map<String, Object> res = sim.performRun();
		ConsolePrinter.printResults(null, res);
	}
}
