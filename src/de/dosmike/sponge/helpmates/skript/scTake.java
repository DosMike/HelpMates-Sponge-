package de.dosmike.sponge.helpmates.skript;

import org.spongepowered.api.item.inventory.slot.OutputSlot;

import de.dosmike.sponge.helpmates.Worker;

/** This command tries to take something from a {@link OutputSlot} if possible, otherwise the full inventory is used */
public class scTake extends scInventoryInteraction {
	
	String[] what;

	public scTake(Worker robot, String... what) {
		this.thisWorker = robot;
		this.what = what;
	}
	
	@Override
	public void execute(boolean first) {
		take(OutputSlot.class, true, what);
	}
	
	@Override
	public String toString() {
		return "Take "+String.join(" ", what);
	}
}
