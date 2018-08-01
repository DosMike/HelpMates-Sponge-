package de.dosmike.sponge.helpmates.skript;

import java.util.Optional;

import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;

import de.dosmike.sponge.helpmates.Worker;

/** This command consumes a food item from the inventory to increase the initial runtime of 20 timesteps (~5 sec) */
public class scCharge implements SkriptCommand {
	
	int amount, consumed;
	boolean done;
	Worker thisWorker;
	
	public scCharge(Worker robot, int amount) {
		this.thisWorker = robot;
		this.amount = amount;
	}
	
	public void execute(boolean first) {
		if (first) {
			if (thisWorker.isAdminsPuppy()) {
				consumed = amount;
				done = true;
				thisWorker.setFuelLevel(Worker.MaxFuelLevel);
				return;
			} else {
				done = false;
				consumed = 0;
			}
		}
		int sub = 0;
		Inventory slots = thisWorker.getInventory().query(QueryOperationTypes.ITEM_TYPE.of(ItemTypes.REDSTONE));
		Optional<ItemStack> stack = slots.peek();
		if (!stack.isPresent()) {
			done = true;
			return;
		} else {
			ItemStack is = stack.get();
			sub = thisWorker.increaseFuelLevel(is, amount-consumed);
			if (sub > 0) slots.poll(sub);
		}
		if (sub == 0) done = true; //could not refuel anymore;
	}
	public boolean isDone() {
		return done || consumed >= amount;
	}
	
	@Override
	public String toString() {
		return "Charge "+amount;
	}
}
