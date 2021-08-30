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
	private static final double TRUCK_INTERVAL = 60.0;
	private static final int TRUCK_CAPACITY = 33; //to be added
	private static final int[] TRUCK_LOAD = {20, 50};
	private static final double PALLET_LOADING_TIME = 1.0;
	private static final double TRUCK_PREPARATION = 10.0;
	private static final double TRUCK_SECURING = 20.0;
	
	static class MM1 extends SimComponentBase {
		
		private ArrayList<Double> loadingTimes = new ArrayList<Double>();
		private Q_Custom q;
		private int trucksArrived = 0;
		private int trucksLoaded = 0;
		private Integer loaderOccupation = null;
		private IntSequence truckLoad;
		
		@Override
		public void init() {
			super.init();
			
			RandomFactory rf = new RandomFactory(getSim(), RANDOM_FACTORY_SEED);
			truckLoad = rf.initRndGen(new IntUniformRange(TRUCK_LOAD[0], TRUCK_LOAD[1]), "truckLoad");
			
			q = new Q_Custom(getSim());
			
			scheduleIn(getSim().simTime(), getSim().currentPrio(), ()->nextTruck());
		}
		
		void nextTruck() {
			int truckID = trucksArrived++;
			prtLine("next truck: " + truckID + " at: " + getSim().simTime());
			q.put(truckID);
			startLoading();
			scheduleIn(TRUCK_INTERVAL, getSim().currentPrio(), ()->nextTruck());
		}
		
		void startLoading() {
			if (loaderOccupation != null) {
				prtLine("	loader occupied");
				return;
			}
			int truckID = q.take();
			loaderOccupation = truckID;
			int load = truckLoad.nextInt();
			double duration = load * PALLET_LOADING_TIME + TRUCK_PREPARATION + TRUCK_SECURING;
			prtLine("	start loading truck: " + truckID + " load: " + load + " duration: " + duration);
			loadingTimes.add(duration);
			scheduleIn(duration, getSim().currentPrio(), ()->finishLoading());
		}
		
		void finishLoading() {
			trucksLoaded++;
			loaderOccupation = null;
			prtLine("finish loading at: " + getSim().simTime());
			if (q.numItems() > 0) {
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
			
			res.put("trucks arrived", trucksArrived);
			res.put("trucks loaded", trucksLoaded);
			res.put("mean wait time", q.getMeanWait());
			
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
