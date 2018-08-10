package de.dosmike.sponge.helpmates.forgehelper;

import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;

public class OreDictAbstraction {
	private static boolean isForgeLoaded;
	static {
		try {
			Class.forName("net.minecraftforge.common.MinecraftForge");
			isForgeLoaded = true;
		} catch (Exception e) {
			isForgeLoaded = false;
		}
	}
	
	/** look at the item in this slot and return the type, if it matches the specified type
	 * OR if the item has a oreDirct entry that starts with the query.
	 * in order to query for ore dict properly you'll have to use the NEI/JEI like $ore syntax */
	public static Optional<ItemType> getContainedItemType(Inventory slots, String itemTypeQuery) {
		for (Inventory i : slots.slots()) {
			Optional<ItemStack> stack = i.peek();
			if (!stack.isPresent()) continue; //slot is empty
			Optional<ItemType> type = getContainedItemType(stack.get(), itemTypeQuery);
			if (type.isPresent()) return type;
		}
		return Optional.empty();
	}
	
	/** look at the item and return the type, if it matches the specified type OR if the item 
	 * has a oreDirct entry that starts with the query.<br>
	 * in order to query for ore dict properly you'll have to use the NEI/JEI like $ore syntax */
	public static Optional<ItemType> getContainedItemType(ItemStack slot, String itemTypeQuery) {
		ItemType type = slot.getType();
		if (type.equals(ItemTypes.AIR) || type.equals(ItemTypes.NONE) || slot.getQuantity() < 1) return Optional.empty();
		
		Optional<ItemType> searched = lookup(itemTypeQuery);
		if (searched.isPresent()) {
			if (searched.get().equals(type)) return searched;
			else return Optional.empty();
		} else if (isForgeLoaded) { //Forge check
			//by only using this class is forge is loaded we can use forge code in that class
			//without breaking anything on vanilla servers as classes are only loaded when
			//needed by the classloader.
			return ForgeOreDictAbstraction.isTypeOfOre(slot, itemTypeQuery); 
		} else throw new NoSuchFieldError("Don't know what "+itemTypeQuery+" is supposed to mean");
	}
	
	private static Optional<ItemType> lookup(String itemTypeQuery) {
		Optional<ItemType> type = Sponge.getRegistry().getType(ItemType.class, itemTypeQuery);
		if (type.isPresent()) return type;
		Optional<BlockType> blocktype = Sponge.getRegistry().getType(BlockType.class, itemTypeQuery);
		if (type.isPresent()) {
			Optional<ItemType> itemtype = blocktype.get().getItem();
			if (itemtype.isPresent()) return itemtype;
		}
		return Optional.empty();
	}
}
