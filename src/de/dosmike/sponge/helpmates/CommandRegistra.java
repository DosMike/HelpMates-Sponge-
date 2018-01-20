package de.dosmike.sponge.helpmates;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult.Type;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import de.dosmike.sponge.mikestoolbox.living.BoxLiving;

public class CommandRegistra {
	private static EntityType[] forbidden = { 
			EntityTypes.PLAYER,
			EntityTypes.ZOMBIE,
			EntityTypes.SKELETON,
	};
	static void register() {
		Map<List<String>, CommandSpec> children = new HashMap<>();
		children.put(Arrays.asList("opmate", "op", "give", "create"), CommandSpec.builder()
				.arguments(GenericArguments.flags().valueFlag(
						GenericArguments.integer(Text.of("fuel")), "f"
				).valueFlag(
						GenericArguments.catalogedElement(Text.of("type"), EntityType.class), "t"
				).buildWith(GenericArguments.none()))
				.executor((src,args)->{
					if (!(src instanceof Player)) throw new CommandException(Text.of("Player only"));
					ItemStack item;
					int fuel = 0;
					EntityType type = EntityTypes.HUSK;
					if (args.hasAny("fuel")) {
						fuel = args.<Integer>getOne("fuel").get();
						if (fuel < 0 || fuel > Worker.MaxFuelLevel) 
							throw new CommandException(Text.of("Fuel amount has to be positive and below "+Worker.MaxFuelLevel));
					}
					if (args.hasAny("type")) {
						EntityType ctype = args.<EntityType>getOne("type").get();
						if (!Living.class.isAssignableFrom(ctype.getEntityClass()))
							throw new CommandException(Text.of("Entity type needs to be of Living type!"));
						if (ArrayUtils.contains(forbidden, ctype))
							throw new CommandException(Text.of(ctype.getId() + " are problematic and thus forbidden"));
					}
					item = CraftingRegistra.iRobotSpawner(fuel, type);
					InventoryTransactionResult result = ((Player)src).getInventory().offer(item);
					if (!result.getType().equals(Type.SUCCESS)) {
						throw new CommandException(Text.of("Please make some space in you inventory"));
					} else {
						src.sendMessage(Text.of(TextColors.GREEN, "Here's your HelpMate"));
					}
					return CommandResult.success();
				})
				.permission("helpmates.command.give.base")
				.build());
		children.put(Arrays.asList("delete"), CommandSpec.builder()
				.arguments(GenericArguments.user(Text.of("Player")))
				.executor((src,args)->{
					User user = args.<User>getOne("Player").orElseThrow(()->new CommandException(Text.of("Unknown user")));
					Collection<Worker> allMates = HelpMates.instance.getWorkers(user);
					allMates.forEach(mate->HelpMates.instance.markDeleted(mate));
					src.sendMessage(Text.of("You've deleted all "+allMates.size()+" HelpMate(s) "+user.getName()+" created"));
					return CommandResult.success();
				})
				.permission("helpmates.command.delete.base")
				.build());
		children.put(Arrays.asList("cast", "convert", "change", "type"), CommandSpec.builder()
				.arguments(GenericArguments.none())
				.executor((src,args)->{
					if (!(src instanceof Player)) throw new CommandException(Text.of("Player only"));
					BoxLiving.addCustomEffect((Player)src, new fxCastType());
					return CommandResult.success();
				})
				.permission("helpmates.command.cast.base")
				.build());
		
		Sponge.getCommandManager().register(HelpMates.instance, CommandSpec.builder()
				.description(Text.of("Admin command to manage HelpMates."))
				.extendedDescription(Text.of("Administractive commands:\n",
						TextColors.GOLD, " /hm opmate -f FUEL", TextColors.WHITE, " to give you a spawner with the specified amount of fuel (100 000 max)\n",
						TextColors.GOLD, " /hm delete <Player>", TextColors.WHITE, " to remove all HelpMates created by this player"))
				.children(children)
				.build()
				, "helpmates", "mates", "hm");
	}
}
