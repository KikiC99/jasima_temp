package task_25;

import java.util.Map;

import org.junit.Test;

import jasima.core.random.RandomFactory;
import jasima.core.random.discrete.IntSequence;
import jasima.core.random.discrete.IntUniformRange;
import jasima.core.simulation.SimComponentBase;
import jasima.core.simulation.Simulation;
import jasima.core.util.ConsolePrinter;

/* Einsendeaufgabe 2.5 (Lagerbestandsermittlung)
 * 
 * Ermitteln Sie durch Simulation die mittlere Anzahl an abgewiesenen Bestellungen
 * in einer Woche für ein Lager mit folgenden Voraussetzungen:
 * 
 * - Wenn der Lagerbestand (I) auf höchstens zehn Einheiten fällt, erfolgt die
 *   Erteilung eines Produktionsauftrages. Nur ein Produktionsauftrag kann
 *   gleichzeitig ausgeführt werden.
 *   
 * - Die Produktionsmenge des Produktionsauftrages wird zum Zeitpunkt der
 *   Auftragserteilung wie folgt ermittelt: 20 - I.
 *   
 * - Wenn eine Bestellung eingeht und der Lagerbestand ist Null, so muss die
 *   Bestellung abgewiesen werden.
 *   
 * - Die Anzahl der Bestellungen pro Tag liegt gleichverteilt zwischen vier und
 *   sieben.
 *   
 * - Die Zeit zwischen Erteilung des Produktionsauftrages und Eingang der
 *   produzierten Einheiten ist abhängig von der Auslastung des Produktionssystems
 *   und liegt gleichverteilt zwischen null und fünf Tagen.
 *   
 * - Zum Simulationsstart befinden sich 18 Einheiten im Lager.
 * 
 * - Die Simulationsdauer beträgt fünf Wochen.
 * 
 * - Die Bestellungen gehen immer um 12 Uhr mittags ein. Nach Bearbeitung
 *   der Bestellungen erfolgt direkt, wenn notwendig, die Erteilung eines Produktionsauftrages.
 *   Die Lieferung der produzierten Einheiten erfolgt immer
 *   um 17 Uhr.
 */

public class MM1_reject {

	private static boolean consoleLog = false;
	
	private static final double LENGTH_SIM = 7 * 24 * 60; //one week in minutes
	private static final int RANDOM_FACTORY_SEED = 34;
	private static final int MIN_STORE_LEVEL = 10;
	private static final int[] NUM_DAILY_JOBS = {4, 7};
	private static final int[] PRODUCTION_ARRIVAL_DELAY = {0, 5}; //in days
	private static final int INITIAL_STORE_LEVEL = 18;
	private static final int DAILY_JOBS_ARRIVAL_TIME = 12; //in hours
	private static final int DAILY_PRODUCTION_ARRIVAL_TIME = 17; //in hours
	
	static class MM1 extends SimComponentBase {
		
		private int numCreatedJobs = 0;
		private int numFulfilledJobs = 0;
		private int numRejectedJobs = 0;
		private int storeLevel = INITIAL_STORE_LEVEL;
		private int arrivalInterval = (DAILY_PRODUCTION_ARRIVAL_TIME - DAILY_JOBS_ARRIVAL_TIME) * 60;
		private IntSequence numDailyJobs; //returns between 4 and 7, equally distributed
		private IntSequence productionArrivalDelay; //returns between 0 and 5, equally distributed
		
		@Override
		public void init() {
			super.init();
			
			RandomFactory rf = new RandomFactory(getSim(), RANDOM_FACTORY_SEED);
			numDailyJobs = rf.initRndGen(new IntUniformRange(NUM_DAILY_JOBS[0], NUM_DAILY_JOBS[1]),
					"numDailyJobs");
			productionArrivalDelay = rf.initRndGen(new IntUniformRange(PRODUCTION_ARRIVAL_DELAY[0],
					PRODUCTION_ARRIVAL_DELAY[1]), "productionArrivalDelay");
			
			scheduleIn((double)(DAILY_JOBS_ARRIVAL_TIME * 60), getSim().currentPrio(),
					()->createDailyJobs()); //schedules first jobs at 12:00
		}
		
		void createDailyJobs() {
			int numJobs = numDailyJobs.nextInt(); //number of daily job between 4 and 7
			prtLine("jobs: " + numJobs + getDailyTime());
			numCreatedJobs += numJobs;
			for(int i = 0; i < numJobs; i++) {
				tryFulfillJob(); //every job tries to be fulfilled by itself
			}
			checkStoreLevel(); //checks whether the store's level is above 10 or a production is needed
			scheduleIn(1440.0, getSim().currentPrio(), ()->createDailyJobs()); //1440 = 24 * 60
		}
		
		void tryFulfillJob() {
			if(storeLevel > 0) {
				storeLevel--;
				numFulfilledJobs++;
				prtLine("	job fulfilled");
				return;
			}
			numRejectedJobs++;
			prtLine("	job rejected");
		}
		
		void checkStoreLevel() {
			prtLine("	store level: " + storeLevel);
			if(storeLevel > MIN_STORE_LEVEL) {
				return; //if the store's level is above 10 no production is needed
			}
			int productionArrival = productionArrivalDelay.nextInt(); //production arrival in days
			prtLine("	production arrival in " + productionArrival + " day(s)");
			int productionSize = 20 - storeLevel; //necessary production size
			scheduleIn((double)(arrivalInterval + productionArrival * 1440), getSim().currentPrio(),
					()->productionArrival(productionSize)); //schedule arrival at 17:00 in 0 to 5 days
		}
		
		void productionArrival(int productionSize) {
			prtLine("production: " + productionSize + getDailyTime());
			storeLevel += productionSize; //arrived production is added to the store
		}
		
		String getDailyTime() { //returns time in days, hours and minutes to print in the console
			int days = (int)(getSim().simTime() / 1440);
			int hours = (int)((getSim().simTime() - days * 1440) / 60);
			int minutes = (int)((getSim().simTime() - days * 1440 - hours * 60) / 60);
			return " at day " + days + " " + hours + ":" + minutes;
		}
		
		void prtLine(String line) {
			if(consoleLog == true) {
				System.out.println(line);
			}
		}
		
		@Override
		public void produceResults(Map<String, Object> res) {
			super.produceResults(res);
			
			res.put("created jobs", numCreatedJobs);
			res.put("fulfilled jobs", numFulfilledJobs);
			res.put("rejected jobs", numRejectedJobs);
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
