package de.dosmike.sponge.helpmates;

import java.util.Arrays;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColors;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipeRegistry;
import org.spongepowered.api.item.recipe.crafting.Ingredient;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import com.google.common.collect.Lists;

public class CraftingRegistra {
	public static final DataQuery robotItem = DataQuery.of("UnsafeData", "robotitem");
	public static final String itemTypePart = "robotpart";
	public static final String itemTypeCore = "robotcore";
	public static final String itemTypeProcessor = "robotprocessor";
	public static final String itemTypeRobot = "robot";
	
	public static final DataQuery robotFuel = DataQuery.of("UnsafeData", "robotfuel");
	public static final DataQuery robotType = DataQuery.of("UnsafeData", "robottype");
	public static final DataQuery robotSpeed = DataQuery.of("UnsafeData", "robotspeed");
	
	static void register() {
		CraftingRecipeRegistry reg = Sponge.getRegistry().getCraftingRecipeRegistry();
		reg.register(rRobotProcessor);
		reg.register(rRobotPart);
		reg.register(rRobotCore);
		reg.register(rRobotSpawner);
	}
	
	static ItemStack iRobotProcessor = ItemStack.builder().fromContainer(
			ItemStack.builder()
			.itemType(ItemTypes.HEAVY_WEIGHTED_PRESSURE_PLATE)
			.add(Keys.DISPLAY_NAME, Text.of(TextColors.YELLOW, "Robot Processor"))
			.add(Keys.ITEM_ENCHANTMENTS, Lists.newArrayList(Enchantment.of(EnchantmentTypes.UNBREAKING, 1)))
			.add(Keys.HIDE_ENCHANTMENTS, true)
			.build().toContainer()
			.set(robotItem, itemTypeProcessor)
		).build();
	
	static ItemStack iRobotPart = ItemStack.builder().fromContainer(
			ItemStack.builder()
			.itemType(ItemTypes.FLOWER_POT)
			.add(Keys.DISPLAY_NAME, Text.of(TextColors.YELLOW, "Robot Part"))
			.add(Keys.ITEM_ENCHANTMENTS, Lists.newArrayList(Enchantment.of(EnchantmentTypes.UNBREAKING, 1)))
			.add(Keys.HIDE_ENCHANTMENTS, true)
			.build().toContainer()
			.set(robotItem, itemTypePart)
		).build();
	
	static ItemStack iRobotCore = ItemStack.builder().fromContainer(
			ItemStack.builder()
			.itemType(ItemTypes.FIREWORK_CHARGE)
			.add(Keys.DISPLAY_NAME, Text.of(TextColors.YELLOW, "HelpMate Core"))
			.add(Keys.ITEM_ENCHANTMENTS, Lists.newArrayList(Enchantment.of(EnchantmentTypes.UNBREAKING, 1)))
			.add(Keys.HIDE_ENCHANTMENTS, true)
			.build().toContainer()
			.set(robotItem, itemTypeCore)
		).build();
	
	private static ItemStack iRobotSpawner = ItemStack.builder().fromContainer(
			ItemStack.builder()
			.itemType(ItemTypes.CHORUS_FRUIT_POPPED)
			.add(Keys.DISPLAY_NAME, Text.of(TextColors.AQUA, "HelpMate"))
			.add(Keys.ITEM_ENCHANTMENTS, Lists.newArrayList(Enchantment.of(EnchantmentTypes.UNBREAKING, 1)))
			.add(Keys.HIDE_ENCHANTMENTS, true)
			.build().toContainer()
			.set(robotItem, itemTypeRobot)
		).build();
	static ItemStack iRobotSpawner(int durability) {
		return iRobotSpawner(durability, EntityTypes.HUSK);
	}
	static ItemStack iRobotSpawner(int durability, EntityType type) {
		ItemStack stack = ItemStack.builder().fromContainer(iRobotSpawner.copy().toContainer()
				.set(robotFuel, durability)
				.set(robotType, type.getId())
			).build();
		
		stack.offer(Keys.ITEM_LORE, Arrays.asList(new Text[]{ 
				Text.of(TextStyles.RESET, TextColors.GRAY, "Fuel: ", TextColors.WHITE, Worker.getRuntime(durability)+"/"+Worker.getRuntime(Worker.MaxFuelLevel)),
				Text.of(TextStyles.RESET, TextColors.GRAY, "Type: ", TextColors.WHITE, type.getName())
			}));
		return stack;
	}
	
	static CraftingRecipe rRobotProcessor = CraftingRecipe.shapedBuilder()
			.aisle("rlr", "gng", "rlr")
			.where('l', Ingredient.builder().with(item -> item.getType().equals(ItemTypes.DYE) && DyeColors.BLUE.equals(item.get(Keys.DYE_COLOR).orElse(null)))
					.withDisplay(ItemTypes.DYE)
					.build())
			.where('g', Ingredient.of(ItemTypes.GLOWSTONE_DUST))
			.where('n', Ingredient.of(ItemTypes.GOLD_NUGGET))
			.where('r', Ingredient.of(ItemTypes.REDSTONE))
			.result( iRobotProcessor )
			.build(itemTypeProcessor, HelpMates.instance);
	
	static CraftingRecipe rRobotPart = CraftingRecipe.shapedBuilder()
			.aisle(" r ", "rer", " r ")
			.where('e', Ingredient.of(ItemTypes.ENDER_EYE))
			.where('r', Ingredient.of(ItemTypes.COMPARATOR))
			.result( iRobotPart )
			.build(itemTypePart, HelpMates.instance);
	
	static CraftingRecipe rRobotCore = CraftingRecipe.shapedBuilder()
			.aisle(" h ", "pap", " p ")
			.where('h', Ingredient.builder().with(item->itemTypeProcessor.equals(item.toContainer().get(robotItem).orElse("")))
					.withDisplay(iRobotPart.getType())
					.build())
			.where('p', Ingredient.builder().with(item->itemTypePart.equals(item.toContainer().get(robotItem).orElse("")))
					.withDisplay(iRobotPart.getType())
					.build())
			.where('a', Ingredient.of(ItemTypes.ARMOR_STAND))
			.result( iRobotCore )
			.build(itemTypeCore, HelpMates.instance);
	
	static CraftingRecipe rRobotSpawner = CraftingRecipe.shapedBuilder()
			.aisle("+h+", "c#l", "+b+")
			.where('+', Ingredient.of(ItemTypes.STRING))
			.where('h', Ingredient.of(ItemTypes.LEATHER_HELMET))
			.where('c', Ingredient.of(ItemTypes.LEATHER_CHESTPLATE))
			.where('l', Ingredient.of(ItemTypes.LEATHER_LEGGINGS))
			.where('b', Ingredient.of(ItemTypes.LEATHER_BOOTS))
			.where('#', Ingredient.builder().with(item->itemTypeCore.equals(item.toContainer().get(robotItem).orElse("")))
					.withDisplay(iRobotCore.getType())
					.build())
			.result( iRobotSpawner )
			.build(itemTypeRobot, HelpMates.instance);
}
