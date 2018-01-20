package de.dosmike.sponge.helpmates.skript;

import org.spongepowered.api.item.inventory.slot.InputSlot;

import de.dosmike.sponge.helpmates.Worker;

/** This command tries to put items into a {@link InputSlot}, otherwise the full inventory is used */
public class scGive extends scInventoryInteraction {
	
	String[] what;

	public scGive(Worker robot, String... what) {
		this.thisWorker = robot;
		this.what = what;
	}
	
	@Override
	public void execute(boolean first) {
		take(InputSlot.class, false, what);
	}
	
	@Override
	public String toString() {
		return "Put "+String.join(" ", what);
	}
}
