package de.dosmike.sponge.helpmates;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.common.reflect.TypeToken;

import de.dosmike.sponge.helpmates.skript.Skript;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class WorkerSerializer {
	@SuppressWarnings("serial")
	public static ConfigurationNode serialize(Worker worker) throws ObjectMappingException {
		CommentedConfigurationNode ccn = HelpMates.instance.configManager.createEmptyNode();
		
		ConfigurationNode agent = ccn.getNode("agent");
		agent.getNode("lastKnownID").setValue(TypeToken.of(UUID.class), worker.lastKnownID);
		agent.getNode("lastKnownPosition").setValue(new TypeToken<Location<World>>(){}, worker.lastKnownPos);
		agent.getNode("entityType").setValue(TypeToken.of(EntityType.class), worker.mobType);
		
		ConfigurationNode owner = ccn.getNode("owner");
		owner.getNode("name").setValue(TypeToken.of(String.class), worker.ownerName);
		if (worker.ownerID != null)
			owner.getNode("userID").setValue(TypeToken.of(UUID.class), worker.ownerID);
		
		CommentedConfigurationNode script = ccn.getNode("script");
		script.setComment("The content from the last set script if it did not finish. will automaticall execute");
		if (worker.getSkript() != null)
			script.setValue(new TypeToken<List<String>>(){}, worker.getSkript().serialize());
		
		List<ItemStack> item = new ArrayList<>(worker.getInventory().size());
		for (Inventory i : worker.getInventory().slots()) {
			if (i.size()==1 && i.peek().get().getQuantity()>0) {
				item.add(i.peek().get());
			}
		}
		ccn.getNode("inventory").setValue(new TypeToken<List<ItemStack>>(){}, item);
		
		ccn.getNode("fuelLevel").setValue(TypeToken.of(Long.class), worker.fuelLevel);
		
		return ccn;
	}
	public static Worker deserialize(ConfigurationNode fromNode) {
		try {
			Worker ret = new Worker(fromNode);
			
			if (!fromNode.getNode("script").isVirtual()) {
				List<String> script = fromNode.getNode("script").getList(TypeToken.of(String.class));
				if (!script.isEmpty()) {
					Skript restored = Skript.parseScriptLines(ret, script);
					ret.setSkript(restored);
				}
			}
			if (!fromNode.getNode("inventory").isVirtual()) {
				List<ItemStack> inv = fromNode.getNode("inventory").getList(TypeToken.of(ItemStack.class));
				for (ItemStack i : inv) {
					ret.inventory.offer(i);
				}
			}
			ret.fuelLevel = fromNode.getNode("fuelLevel").getLong(0);
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
