package de.dosmike.sponge.helpmates.skript;

import org.spongepowered.api.item.inventory.slot.FuelSlot;

import de.dosmike.sponge.helpmates.Worker;

/** This command tries to put items into a {@link FuelSlot}, otherwise the full inventory is used */
public class scFuel extends scInventoryInteraction {
	
	String[] what;

	public scFuel(Worker robot, String... what) {
		this.thisWorker = robot;
		this.what = what;
	}
	
	@Override
	public void execute(boolean first) {
		take(FuelSlot.class, false, what);
	}
	
	@Override
	public String toString() {
		return "Fuel "+String.join(" ", what);
	}
}
