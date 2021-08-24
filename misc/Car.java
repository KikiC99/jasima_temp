package misc;

import jasima.core.simulation.SimComponentBase;
import jasima.core.simulation.Simulation;

/* Car example, based on https://simpy.readthedocs.io/en/latest/simpy_intro/basic_concepts.html
 * Car alternates between parking for 5 minutes and driving for 2 minutes
 * Simulation runs for 20 minutes
 */

public class Car {

private static final double Simulation_Duration = 20.0; //Simulation duration
	
	static class CarModel extends SimComponentBase{
		
		private double park_duration = 5.0; //parking duration
		private double trip_duration = 2.0; //driving duration
		
		public void init() {
			super.init();
			scheduleIn(0, getSim().currentPrio(), this::startPark); //the first instance of parking is initialized at time 0
		}
		
		void startPark() {
			System.out.println("Start parking at " + getSim().simTime()); //parking starting time is printed in the console
			scheduleIn(park_duration, getSim().currentPrio(), this::startDrive); //driving is scheduled in 5 minutes
		}
		
		void startDrive() {
			System.out.println("Start driving at " + getSim().simTime()); //driving starting time is printed in the console
			scheduleIn(trip_duration, getSim().currentPrio(), this::startPark); //parking is scheduled in 2 minutes
		}
		
	}

	public static void main(String[] args) {
		
		Simulation sim = new Simulation(); //a new simulation is created
		sim.setSimulationLength(Simulation_Duration); //the duration of the simulation is in this case set to 20 minutes
		sim.addComponent(new CarModel()); //the class 'CarModel' is added to the simulation
		sim.performRun(); //finally the simulation is run
		
	}
}
