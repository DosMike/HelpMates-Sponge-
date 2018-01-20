package de.dosmike.sponge.helpmates.skript;

import java.util.Optional;

import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;

/** this abstract command tries to transfer items, and returns the transfered itemstack. no type equals any type */
public abstract class scItemTransferAction implements SkriptCommand {
	
	/**
	 * force continues the loop for each slot until one stack was transfered
	 */
	protected Optional<ItemStackSnapshot> transfer(ItemType type, Inventory from, Inventory to, boolean force) {

//		Sponge.getCauseStackManager().getCurrentCause().forEach(cause->HelpMates.l("Cause by %s", cause.toString()));
		
		if (to.size()>=to.capacity()) return Optional.empty(); //to has no more empty slots
		Inventory source = (type!=null) ? from.query(QueryOperationTypes.ITEM_TYPE.of(type)) : from; 
		for (Inventory s : source.slots()) {
			Optional<ItemStack> is = s.poll();
			if (!is.isPresent()) continue;
			ItemStack stack = is.get();
			if (stack.isEmpty() || stack.getType().equals(ItemTypes.AIR) || stack.getType().equals(ItemTypes.NONE)) continue;
			
//			HelpMates.l("Checking %d %s", stack.getQuantity(), stack.getType().getId());
			int consumed = stack.getQuantity();
			InventoryTransactionResult result = to.offer(stack); //move item
			consumed -= stack.getQuantity();
			
//			HelpMates.l("Moved %d %s", consumed, stack.getType().getId());
			
			if (!result.getType().equals(InventoryTransactionResult.Type.SUCCESS) && force) continue;
			if (consumed < 1 && force) continue;
			//return a stack, that contains data about how much was transfered
			stack = stack.copy();
			stack.setQuantity(consumed);
			return Optional.of(stack.createSnapshot());
		}
		return Optional.empty();
	}
	
}
