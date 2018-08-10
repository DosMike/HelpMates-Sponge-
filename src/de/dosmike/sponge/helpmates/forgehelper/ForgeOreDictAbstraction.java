package de.dosmike.sponge.helpmates.forgehelper;

import java.util.Optional;

import org.spongepowered.api.item.ItemType;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/** by only using this class is forge is loaded we can use forge code in that class
 * without breaking anything on vanilla servers as classes are only loaded when
 * needed by the classloader. */
class ForgeOreDictAbstraction {
	/** when calling this method I assume we're coming from OreDictAbstraction
	 * so I'll only look in the oredict and not search the itemId again.<br>
	 * @return the ItemType if the stack is of the ore searched
	 */
	static Optional<ItemType> isTypeOfOre(org.spongepowered.api.item.inventory.ItemStack spongeStack, String dict) {
		if (dict.length()<2 || dict.charAt(0)!='$') 
			throw new NoSuchFieldError("Try using a oreDict entry with $oreDictEntry");
		dict = dict.substring(1);
		if (invalidQuery(dict))
			throw new NoSuchFieldError("Don't know what $"+dict+" is supposed to mean");
		ItemStack item = ItemStackUtil.toNative(spongeStack);
		 
		for (int oreID : OreDictionary.getOreIDs(item)) {
			String oreDictName = OreDictionary.getOreName(oreID);
			if (oreDictName.startsWith(dict)) 
				return Optional.of(spongeStack.getType()); 
		}
		return Optional.empty();
	}
	
	static boolean invalidQuery(String query) {
		for (String s : OreDictionary.getOreNames()) {
			if (s.startsWith(query)) return false;
		}
		return true;
	}
}
