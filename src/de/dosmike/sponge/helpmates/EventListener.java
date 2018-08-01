package de.dosmike.sponge.helpmates;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Creature;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent.Primary;
import org.spongepowered.api.event.entity.InteractEntityEvent.Secondary;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.world.chunk.LoadChunkEvent;
import org.spongepowered.api.event.world.chunk.UnloadChunkEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;

import de.dosmike.sponge.helpmates.skript.Skript;

public class EventListener {
	/** 
	 * This listener should ensure, that we do not lose and spam goto entities 
	 */
	@Listener
	public void unloadChunk(UnloadChunkEvent event) {
		Collection<Entity> chunkents = event.getTargetChunk().getEntities();
		
		List<Worker> workers = HelpMates.instance.getAllWorkers().stream().filter(worker->worker.targetHelper!=null).collect(Collectors.toList()); //filter workers, that have a target
		workers.removeIf(worker->!chunkents.contains(worker.targetHelper)); //retain workers, that have a target in this chunk
		workers.forEach(worker->worker.despawnHelper());
	}

	/**
	 * This listerner will continue goto commands for workers, once the target chunk loads again
	 */
	@Listener
	public void loadChunk(LoadChunkEvent event) {
		Vector3i chunkpos = event.getTargetChunk().getPosition();
		List<Worker> workers = HelpMates.instance.getAllWorkers().stream()
				.filter(worker->
					worker.target != null && 
					worker.target.getChunkPosition().equals(chunkpos) && 
					worker.targetHelper == null)
				.collect(Collectors.toList()); //filter workers, that want to go into the loaded chunk, and there is no targetHelper yet
		workers.forEach(worker->worker.spawnHelper());
	}
	
	/** @return false indicates a early cancel or failure */
	public boolean useBook(InteractEntityEvent event) {
		Optional<Player> player = event.getCause().first(Player.class);
		if (!player.isPresent()) {
//			HelpMates.l("No Player");
			return false;
		}
		Optional<ItemStack> item = player.get().getItemInHand(HandTypes.MAIN_HAND);
		if (!item.isPresent()) {
//			HelpMates.l("No item");
			return false;
		}
		Optional<List<Text>> pages = item.get().get(Keys.BOOK_PAGES);
		if (!pages.isPresent()) {
//			HelpMates.l("Not a book");
			return false;
		}
		if (event.getTargetEntity() instanceof Creature) {
			Creature agent = (Creature)event.getTargetEntity();
			Worker worker = HelpMates.instance.getWorkerByAgent(agent).orElse(new Worker(agent, player.get()));
			try {
				Skript skript = Skript.parseScript(worker, pages.get());
				worker.setSkript(skript);
				player.get().sendMessage(Text.of(TextColors.GREEN, "Script set"));
			} catch (Exception e) {
				player.get().sendMessage(Text.of(TextColors.RED, e.getMessage()));
				Throwable t = e;
				while ((t = t.getCause())!=null) {
					player.get().sendMessage(Text.of(TextColors.RED, t.getMessage()));
				}
			}
			HelpMates.instance.addWorker(worker);
			return true;
		}
		return false;
	}
	
	@Listener
	public void interactEntity(InteractEntityEvent event) {
		Optional<Player> p = event.getCause().first(Player.class);
		if (!p.isPresent()||!(event.getTargetEntity() instanceof Creature)) return;
		Optional<Worker> w = HelpMates.instance.getWorkerByAgent((Creature)event.getTargetEntity());
		if (!w.isPresent()) return;
		
		//runtime and name
		String sp = w.get().ownerName;
		if (sp.endsWith("s")) sp+="'";
		else sp+="'s";
		sp+=" Robot: ";
		
		if (p.get().hasPermission("helpmates.interact.ignoreowner") ||
			(w.get().ownerID != null && w.get().ownerID.equals(p.get().getUniqueId())) ) {

			Text status = w.get().getError().isPresent()
					? Text.of(TextColors.RED, "Error")
					: w.get().getSkript() == null
						? Text.of(TextColors.GRAY, "Idle")
						: Text.of(TextColors.GREEN, "Running");
			Text prefix = Text.builder(sp).style(TextStyles.ITALIC).onHover(TextActions.showText(
					Text.of("Fuel: ", Worker.getRuntime(w.get().fuelLevel), "/", Worker.getRuntime(Worker.MaxFuelLevel), Text.NEW_LINE, 
							"Status: ", status )
					)).build();
			
			if (event instanceof Secondary) {
				p.get().openInventory(w.get().inventory);
				event.setCancelled(true);
				return;
			} else if (event instanceof Primary) {
				if (useBook(event)) { 
					event.setCancelled(true);
					return;
				} else if (p.get().get(Keys.IS_SNEAKING).orElse(false)) {
					HelpMates.instance.markDeleted(w.get());
					p.get().sendMessage(Text.of(prefix, TextColors.RED, "Bye bye"));
					event.setCancelled(true);
					return;
				}
			}
			Optional<String> message = w.get().getError();
			if (!message.isPresent()) {
				p.get().sendMessage(Text.of(prefix, (w.get().myscript==null?"Hi, I'm currently taking a break":"My current task is \""+w.get().myscript.currentAction()+"\"")));
			} else {
				p.get().sendMessage(Text.of(prefix, TextColors.RED, message.get()));
			}
		} else {
			p.get().sendMessage(Text.of(TextStyles.ITALIC, sp, TextStyles.RESET, "My programmers told me not to talk to strangers"));
		}
		
		
	}

	@Listener
	public void clickBlock(InteractBlockEvent.Secondary event) {
		//get player
		Optional<Player> player = event.getCause().first(Player.class);
		if (!player.isPresent()) return;
		//get used item for block interaction with priority main hand -> off hand
		HandType hand = HandTypes.MAIN_HAND;
		Optional<ItemStack> usedItem = player.get().getItemInHand(hand);
		hand = HandTypes.OFF_HAND;
		if (!usedItem.isPresent()) usedItem = player.get().getItemInHand(hand);
		if (!usedItem.isPresent()) return;
		//check if item is iRobotSpawner
		if (!CraftingRegistra.itemTypeRobot.equals(usedItem.get().toContainer().get(CraftingRegistra.robotItem).orElse(""))) return;
		event.setCancelled(true);
		
		if (!player.get().hasPermission("helpmates.create.base")) {
			player.get().sendMessage(Text.of(TextColors.RED, "You may not place helpmates here"));
			return;
		}
		Optional<String> option = player.get().getOption("helpmates.option.create.limit");
		if (option.isPresent()) {
			try {
				Integer i = Integer.parseInt(option.get());
				if (i < 0) throw new NumberFormatException("Posivite number expected");
				if (HelpMates.instance.getWorkers(player.get()).size() >= i) {
					player.get().sendMessage(Text.of(TextColors.RED, "You've reached your limit of HelpMates"));
					return;
				}
			} catch (Exception e) {
				HelpMates.w("Player %s has an invalid value for Option helpmates.option.create.limit: %s - should be positive number!", player.get().getName(), option.get());
				return;
			}
		}
		
		//create worker entity
		Optional<Location<World>> target = event.getTargetBlock().getLocation();
		if (!target.isPresent()) return;
		Location<World> spawnAt = target.get().getRelative(event.getTargetSide()).add(new Vector3d(0.5,0.0,0.5));
		if (HelpMates.instance.getWorkerByLocation(spawnAt).isPresent()) return;
		EntityType spawnType = Sponge.getRegistry().getType(EntityType.class, (String)usedItem.get().toContainer().get(CraftingRegistra.robotType).orElse("minecraft:husk")).orElse(EntityTypes.HUSK);
		Entity agent = spawnAt.getExtent().createEntity(spawnType, spawnAt.getPosition());
		agent.offer(Keys.IS_SILENT, true);
		agent.offer(Keys.AI_ENABLED, false);
		if (!spawnAt.getExtent().spawnEntity(agent)) {
			agent.remove();
			return;
		}
		
		//register worker
		Worker newWorker = new Worker((Creature)agent, player.get());
		int fuel = (Integer) usedItem.get().toContainer().get(CraftingRegistra.robotFuel).orElse(0);
		newWorker.fuelLevel=fuel;
		
		HelpMates.instance.addWorker(newWorker);
		
		//consume item
		player.get().getInventory()
			.query(QueryOperationTypes.ITEM_STACK_CUSTOM.of(item-> {
				DataContainer c = item.toContainer();
				EntityType entityType = Sponge.getRegistry().getType(EntityType.class, (String)c.get(CraftingRegistra.robotType).orElse("minecraft:husk")).orElse(EntityTypes.HUSK);
				return CraftingRegistra.itemTypeRobot.equals(c.get(CraftingRegistra.robotItem).orElse("")) &&
						fuel == (Integer)c.get(CraftingRegistra.robotFuel).orElse(0) &&
						spawnType.equals(entityType)
						;
			}))
			.poll(1);
	}
	
//	@Listener
//	public void breakBlockByWorker(ChangeBlockEvent.Break event) {
//		
//	}
	@Listener
	public void droppedItems(DropItemEvent.Destruct event) {
		Optional<Worker> responsible = event.getCause().first(Worker.class);
		if (!responsible.isPresent()) return;
		event.setCancelled(true); //don't drop items dropped by blocks that a worker destroyed
	}
	@Listener
	public void droppedItems(DropItemEvent.Pre event) {
		Optional<Worker> responsible = event.getCause().first(Worker.class);
		if (!responsible.isPresent()) return;
		List<ItemStack> remainder = new LinkedList<>();
		event.getDroppedItems().forEach(drop->{ //try to add drops into the inventory
			HelpMates.l("Original Drop: %d %s", drop.getQuantity(), drop.getType().getId());
			ItemStack asStack = drop.createStack();
			responsible.get().getInventory().offer(asStack);
			remainder.add(asStack); //stuff that the worker couldn't hold will be put in here
		});
		event.getDroppedItems().clear();
		remainder.forEach(remain->{ //refill the drop list with stuff we were unable to pick up
			HelpMates.l("Post Drop: %d %s", remain.getQuantity(), remain.getType().getId());	
			event.getDroppedItems().add(remain.createSnapshot());
		});
	}
	
	/*
	@Listener
	public void clickyStuffEvent(ClickInventoryEvent event) {
		event.getTransactions().forEach(transaction->{
			if (transaction.getSlot() instanceof CraftingOutput) { //check if a outputslot changes
				Inventory checkon = event.getTargetInventory();
				InventoryArchetype it = checkon.getArchetype();
				
				for (Inventory i : checkon) {
					HelpMates.l("Sub %s", i.getClass().getSimpleName());
				}
				Optional<CraftingInventory> craftingInventory = findByInterface(checkon, CraftingInventory.class);
				if (!craftingInventory.isPresent()) return;
				HelpMates.l("cInv %s", craftingInventory.get().getClass().getSimpleName());
								
//				Inventory craftingInput = checkon.query(QueryOperationTypes.INVENTORY_TYPE.of(GridInventory.class));
//				Inventory craftingInventory = Inventory.builder().from(checkon.first()).build(HelpMates.instance);
				
				Optional<CraftingRecipe> result = Sponge.getRegistry().getCraftingRecipeRegistry().findMatchingRecipe(craftingInventory.get().getCraftingGrid(), Sponge.getServer().getWorlds().iterator().next());
				if (result.isPresent()) {
					ItemStackSnapshot output = result.get().getResult(craftingInventory.get().getCraftingGrid());
					CraftItemEvent subevent;
					if (event.getCursorTransaction().getFinal().equals(ItemStackSnapshot.NONE)) {
						HelpMates.l("Creating CrafItem Pre with %s", output.getType().getId());
						subevent = new CraftItemEvent.Pre(result.get(), output, checkon, event);
					} else if (event.getCursorTransaction().getFinal().equals(output)) {
						HelpMates.l("Creating CrafItem Post with %s", output.getType().getId());
						subevent = new CraftItemEvent.Post(result.get(), output, checkon, event);
					} else {
						HelpMates.l("Dunno man, cursor DOF: %s %s %s",
								event.getCursorTransaction().getDefault().getType().getId(),
								event.getCursorTransaction().getOriginal().getType().getId(),
								event.getCursorTransaction().getFinal().getType().getId());
						return;
					}
					Sponge.getEventManager().post(subevent);
					if (subevent.isCancelled()) event.setCancelled(true);
				}
			}
		});
	}
	// can't get spognes query to work for this, so i wrote my own
	// since inventory adapters are common and not API i have to go by name
	private <T extends Inventory> Optional<T> findByInterface(Inventory parent, Class<T> iName) {
		assert iName.isInterface();
		if (iName.isInstance(parent) && ArrayUtils.contains(parent.getClass().getInterfaces(), iName)) return Optional.of((T)parent);
		if (!parent.hasChildren()) return Optional.empty();
		Optional<T> result;
		for (Inventory i : parent) {
			result = findByInterface(i, iName);
			if (result.isPresent()) return result;
		}
		return Optional.empty();
	}
	
	@Listener
	public void craftItemEvent(CraftItemEvent event) {
		if (event instanceof CraftItemEvent.Pre) {
			HelpMates.l("About to craft a %s", event.getCraftingResult().getType().getId());
		} else {
			HelpMates.l("Crafted a %s", event.getCraftingResult().getType().getId());
		}
	}*/
}
