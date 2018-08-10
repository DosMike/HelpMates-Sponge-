package de.dosmike.sponge.helpmates;

import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Creature;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import de.dosmike.sponge.mikestoolbox.living.BoxLiving;
import de.dosmike.sponge.mikestoolbox.living.CustomEffect;

/** for now it's converting robots, it's tho supposed to convert cores, once possible */
public class fxCastType implements CustomEffect {
	
	int progress=0, stage=0;
	
	boolean valid=true;
	EntityType initial = null;
	
	@Override
	public String getName() {
		return "helpmate:casttype";
	}
	
	@Override
	public boolean isInstant() {
		return false;
	}
	
	@Override
	public double getDuration() {
		return 7;
	}
	
	@Override
	public boolean isRunning() {
		return valid;
	}
	
	@Override
	public void onApply(Living entity) {
		if (!(entity instanceof Player) || BoxLiving.hasCustomEffect(entity, getClass())) { valid = false; return; }
		Optional<EntityType> living = validateHands((Player)entity);
		if (!living.isPresent()) {
			((Player)entity).sendMessage(Text.of(TextColors.RED, "Please hold one core and one lapis, with the lapis named after the target mob type (e.g. minecraft:cow)"));
			valid = false;
		} else {
			String perm = "helpmates.cast.entity."+living.get().getId().replaceAll(":", ".");
			if (!((Player)entity).hasPermission(perm)) {
				((Player)entity).sendMessage(Text.of(TextColors.RED, "You need permission "+perm+" for this type of entity"));
				valid = false;
			} else {
				initial = living.get();
				if (initial.equals(EntityTypes.PLAYER)) initial = EntityTypes.HUMAN; //you can't manually spawn players, humans are fake players
				((Player)entity).sendMessage(Text.of(TextStyles.ITALIC, "Please focus while casting!"));
			}
		}
	}
	
	@Override
	public void onTick(Living entity, int dt) {
		if (!(entity instanceof Player)) { valid = false; return; }
		progress += dt;
		if (progress > 1000) {
			stage++;
			progress -= 1000;
			
			Optional<EntityType> type = validateHands((Player)entity);
			if (!type.isPresent() || !type.get().equals(initial)) {
				((Player)entity).sendMessage(Text.of("You interrupted the cast"));
				valid = false; 
				return; 
			}
			if (stage == 1) {
				((Player)entity).sendMessage(Text.of(TextColors.WHITE, "I have a helper"));
			} else if (stage == 3) {
				((Player)entity).sendMessage(Text.of(TextColors.YELLOW, "I have a ", Text.builder("type key").onHover(TextActions.showText(Text.of(initial.getId()))).color(TextColors.BLUE).build() ));
			} else if (stage == 5) {
				((Player)entity).sendMessage(Text.of(TextColors.GOLD, TextStyles.BOLD, TextStyles.OBFUSCATED, "A", TextStyles.RESET, TextColors.GOLD, TextStyles.BOLD, " UH ", TextStyles.OBFUSCATED, "B"));
			} else if (stage == 6) {
				
				convertYay((Player)entity);
				
				((Player)entity).sendMessage(Text.of(TextColors.GREEN, initial.getId() + " helper"));
				((Player)entity).sendMessage(Text.of(TextColors.GRAY, "Sucessfully changed type of robot"));
			}
		}
	}
	
	private Optional<EntityType> validateHands(Player player) {
		Optional<ItemStack> main = player.getItemInHand(HandTypes.MAIN_HAND);
		Optional<ItemStack> off = player.getItemInHand(HandTypes.OFF_HAND);
		if (!main.isPresent() || !off.isPresent()) return Optional.empty();
		ItemStack a = main.get(), b = off.get();
		if (a.isEmpty() || b.isEmpty()) return Optional.empty();
		String typename = null;
		if (a.getQuantity() != 1 || b.getQuantity() != 1) return Optional.empty();
		
		if (CraftingRegistra.itemTypeRobot.equals(a.toContainer().get(CraftingRegistra.robotItem).orElse(""))) {
			if (b.getType().equals(ItemTypes.DYE) && b.get(Keys.DYE_COLOR).orElse(DyeColors.BLACK).equals(DyeColors.BLUE)) {
				typename = b.get(Keys.DISPLAY_NAME).orElse(Text.EMPTY).toPlain();
				if (typename.equals("")) return Optional.empty();
			}
		} else if (CraftingRegistra.itemTypeRobot.equals(b.toContainer().get(CraftingRegistra.robotItem).orElse(""))) {
			if (a.getType().equals(ItemTypes.DYE) && a.get(Keys.DYE_COLOR).orElse(DyeColors.BLACK).equals(DyeColors.BLUE)) {
				typename = a.get(Keys.DISPLAY_NAME).orElse(Text.EMPTY).toPlain();
				if (typename.equals("")) return Optional.empty();
			}
		}
		
		if (typename==null) return Optional.empty();
		Optional<EntityType> type = Sponge.getRegistry().getType(EntityType.class, typename);
		if (!type.isPresent()) return Optional.empty();
		
		if (Creature.class.isAssignableFrom(type.get().getEntityClass())) return type;
		
		return Optional.empty();
	}
	
	private void convertYay(Player player) {
		Optional<ItemStack> main = player.getItemInHand(HandTypes.MAIN_HAND);
		Optional<ItemStack> off = player.getItemInHand(HandTypes.OFF_HAND);
		if (!main.isPresent() || !off.isPresent()) return;
		ItemStack a = main.get(), b = off.get();
		if (a.isEmpty() || b.isEmpty()) return;
		if (a.getQuantity() != 1 || b.getQuantity() != 1) return;
		
		if (CraftingRegistra.itemTypeRobot.equals(a.toContainer().get(CraftingRegistra.robotItem).orElse(""))) {
			player.setItemInHand(HandTypes.OFF_HAND, null);
			player.setItemInHand(HandTypes.MAIN_HAND, 
					CraftingRegistra.iRobotSpawner((Integer)a.toContainer().get(CraftingRegistra.robotFuel).orElse(0), initial));
		} else if (CraftingRegistra.itemTypeRobot.equals(b.toContainer().get(CraftingRegistra.robotItem).orElse(""))) {
			player.setItemInHand(HandTypes.MAIN_HAND, null);
			player.setItemInHand(HandTypes.OFF_HAND, 
					CraftingRegistra.iRobotSpawner((Integer)b.toContainer().get(CraftingRegistra.robotFuel).orElse(0), initial));
		}
	}
}
