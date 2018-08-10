package de.dosmike.sponge.helpmates.skript;

import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;

import de.dosmike.sponge.helpmates.Worker;
import de.dosmike.sponge.helpmates.forgehelper.InventoryTile;
import de.dosmike.sponge.helpmates.forgehelper.OreDictAbstraction;

/** This abstract command transfered items from one inventory to another, with a specified item type id or keyword */
public abstract class scInventoryInteraction extends scItemTransferAction {
	
	protected Worker thisWorker;
	protected boolean isDone=false;
	
	/** Can transfer a specific item type, anything, everything or something between inventories<br>
	 *  Everything means until from is can't transfer anymore, Something means at least one stack (waiting), Anything means one stack if possible
	 * @param operations are executed in order, recommended is one. Use either the item id string or a keyword
	 */
	protected <T extends Inventory> void take(Class<T> inventoryType, boolean toWorker, String... operations) {
		try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
			frame.pushCause(thisWorker);
			if (thisWorker.getOwner().isPresent()) { 
				frame.addContext(EventContextKeys.PLAYER_SIMULATED, thisWorker.getOwner().get().getProfile());
				frame.addContext(EventContextKeys.OWNER, thisWorker.getOwner().get()); //appropriate?
			}
		
		isDone = false;
		Optional<InventoryTile> carrier = thisWorker.getOpenCarrier();
		if (!carrier.isPresent()) {
//			HelpMates.l("Carrier was not opened!");
			thisWorker.setError("I did not open a container");
			return;
		}
		//get all outputs
		Inventory other = carrier.get().getInventory();
		other = other.query(QueryOperationTypes.TYPE.of(inventoryType));
		if (other.capacity() < 1) {
//			HelpMates.l("No such inventory class, using whole inventory");
			other = carrier.get().getInventory(); //basically if there are no output slots, use the whole inventory again
		}
		
//		for (Inventory s : other.slots()) {
//			Optional<ItemStack> is = ((Slot)s).peek();
//			if (is.isPresent())
//				HelpMates.l("Selected slot: %d %s", is.get().getQuantity(), is.get().getType().getId());
//			else
//				HelpMates.l("Selected another empty slot");
//		}
		
		for (String thing : operations) {
//			HelpMates.l("Looping, %s", thing);
			if (thing.equalsIgnoreCase("anything")) {
				transfer(null, (toWorker?other:thisWorker.getInventory()), (toWorker?thisWorker.getInventory():other), !toWorker); //i want to force if we are putting into the container to get some stack that's accepted by smelting/fuel input
				isDone = true; return;
			} else if (thing.equalsIgnoreCase("something")) {
				Optional<ItemStackSnapshot> t = transfer(null, (toWorker?other:thisWorker.getInventory()), (toWorker?thisWorker.getInventory():other), true);
				if (t.isPresent()) isDone = true; return;
			} else if (thing.equalsIgnoreCase("everything")) {
				Optional<ItemStackSnapshot> t = transfer(null, (toWorker?other:thisWorker.getInventory()), (toWorker?thisWorker.getInventory():other), true);
				if (!t.isPresent()) isDone=true; return;
			} else {
				Optional<ItemType> type = null;
				try {
					type = OreDictAbstraction.getContainedItemType((toWorker?other:thisWorker.getInventory()), thing);
				} catch (NoSuchFieldError err) {
					thisWorker.setError(err.getMessage());
					return;
				}
				if (type == null || !type.isPresent()) continue; //maybe something else is available
				if (type.get().equals(ItemTypes.AIR)) {
					thisWorker.setError("I can't handle air");
					return;
				}
				Optional<ItemStackSnapshot> t = transfer(type.get(), (toWorker?other:thisWorker.getInventory()), (toWorker?thisWorker.getInventory():other), true);
				if (t.isPresent()) isDone=true; return;
			}
		}
		
		}
	}
	
	@Override
	public boolean isDone() {
		return isDone;
	}
}
