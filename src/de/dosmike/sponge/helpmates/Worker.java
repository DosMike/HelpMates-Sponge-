package de.dosmike.sponge.helpmates;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.ai.Goal;
import org.spongepowered.api.entity.ai.GoalTypes;
import org.spongepowered.api.entity.ai.task.builtin.creature.AttackLivingAITask;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.entity.living.Creature;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.extra.fluid.FluidStack;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.common.reflect.TypeToken;

import de.dosmike.sponge.helpmates.forgehelper.InventoryTile;
import de.dosmike.sponge.helpmates.skript.Skript;
import de.dosmike.sponge.mikestoolbox.living.BoxLiving;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

public class Worker {
	public static final long MaxFuelLevel = 100_000;
	
	Location<World> target=null, lastKnownPos=null;
	UUID lastKnownID=null;
	Entity targetHelper=null;
	Creature currentMob=null;
	Skript myscript=null;
//	List<ItemStack> inventory = new LinkedList<>();
	Inventory inventory = null;
	FluidStack fluidInventory;
	InventoryTile useWith = null;
	String error = null;
	String ownerName = null;
	UUID ownerID = null;
	long lastErrNotice=0;
	EntityType mobType = EntityTypes.HUSK;
	long fuelLevel = 0;
	float speedMod = 1f; //for later: maybe implement speed upgrades?
	
	/*public Worker(Location<World> spawn, User owner) {
		lastKnownPos = spawn;
		ownerName = owner.getName();
		ownerID = owner.getUniqueId();
		inventory = Inventory.builder()
			.of(InventoryArchetypes.DISPENSER)
			.property("inventorytitle", new InventoryTitle(Text.of(getMobTitle())))
			.build(HelpMates.instance);
	}*/
	/**
	 * will restore base informations for this worker from the configuration node
	 * @throws ObjectMappingException 
	 */
	@SuppressWarnings("serial")
	public Worker(ConfigurationNode node) throws ObjectMappingException {
		lastKnownID = node.getNode("agent").getNode("lastKnownID").getValue(TypeToken.of(UUID.class));
		lastKnownPos = node.getNode("agent").getNode("lastKnownPosition").getValue(new TypeToken<Location<World>>(){});
		mobType = node.getNode("agent").getNode("entityType").getValue(TypeToken.of(EntityType.class));
		ownerName = node.getNode("owner").getNode("name").getString("?");
		ownerID = node.getNode("owner").getNode("userID").getValue(TypeToken.of(UUID.class));
		inventory = Inventory.builder()
				.of(InventoryArchetypes.DISPENSER)
				.property("inventorytitle", new InventoryTitle(Text.of(getMobTitle())))
				.build(HelpMates.instance);
	}
	public Worker(Creature agent, User owner) {
		lastKnownPos = agent.getLocation();
		lastKnownID = agent.getUniqueId();
		currentMob = agent;
		mobType = agent.getType();
		agent.offer(Keys.IS_SILENT, true);
        agent.offer(Keys.INVULNERABLE, true);
		ownerName = owner.getName();
		ownerID = owner.getUniqueId();
		setMobTitle();
		inventory = Inventory.builder()
				.of(InventoryArchetypes.DISPENSER)
				.property("inventorytitle", new InventoryTitle(Text.of(getMobTitle())))
				.build(HelpMates.instance);
	}
	
	public Creature getAgent() {
		return currentMob;
	}
	public Optional<User> getOwner() {
		Optional<Player> online = Sponge.getServer().getPlayer(ownerID);
		if (online.isPresent()) return Optional.of((User)online.get());
		return HelpMates.userStorage.get(ownerID);
	}
	public void setTarget(Location<World> targetLocation) {
		despawnHelper();
		if (targetLocation == null) {
			target = null;
			if (targetHelper != null) {
				targetHelper.remove();
				targetHelper = null;
			}
			if (currentMob != null)
				currentMob.setTarget(null);
		} else {
			setOpenCarrier(null); //close continaer
			target = targetLocation;
			Optional<Chunk> chunk = target.getExtent().getChunk(target.getChunkPosition());
			if (chunk.isPresent() && chunk.get().isLoaded()) {
				spawnHelper();
			}
		}
	}
	
	public void setSkript(Skript skript) {
		error = null;
		if (fuelLevel >= 0 && fuelLevel < 20) fuelLevel = 20;
		myscript = skript;
		setMobTitle();
	}
	public Skript getSkript() {
		return myscript;
	}
	
	/**
	 * Despawn the entity the currentMob can path towards.<br>
	 * This also disables the currentMobs AI as the AI is only required for pathing.
	 */
	public void despawnHelper() {
		if (target != null && targetHelper != null) {
			currentMob.offer(Keys.AI_ENABLED, false); //disable AI for the time being, until the goto can be continued
			targetHelper.remove();
			targetHelper = null;
//			HelpMates.l("Depawned Helper at %s", target.toString());
		}
	}
	/**
	 * Spawn ha invisible helper slime that the currentMob can path towards.<br>
	 * The slime will be invisible and silent, and this functions enables the currentMobs AI to start pathing.
	 */
	public void spawnHelper() {
		Optional<Chunk> chunk = target.getExtent().getChunk(target.getChunkPosition());
		if (chunk.isPresent() && chunk.get().isLoaded()) {
			Entity e = target.getExtent().createEntity(EntityTypes.SLIME, target.getPosition());
			e.offer(Keys.AI_ENABLED, false);
			e.offer(Keys.IS_SILENT, true);
			e.offer(Keys.INVULNERABLE, true);
			e.offer(Keys.VANISH, true);
			e.offer(Keys.SLIME_SIZE, 0); //smales size acording to wiki
			if (!target.getExtent().spawnEntity(e)) {
//				e.remove();
				throw new RuntimeException("Could not spawn target helper for worker "+this.toString());
			} 
//			HelpMates.l("Spawned Helper at %s", target.toString());
			targetHelper = e;
			currentMob.offer(Keys.AI_ENABLED, true); //start walking
			
			//works better for peacefull mobs
			//div the move speed by move speed will result in only out base speed being left over
			Double speedFix = BoxLiving.getMovementSpeed(currentMob).orElse(0.7);
			HelpMates.l("Attributes.generic.movementSpeed: %f", speedFix);
			speedFix = 0.20 / speedFix; //the base speed i want mobs to move with
			AttackLivingAITask task = AttackLivingAITask.builder().longMemory().speed(speedFix*speedMod).build(currentMob);
			Optional<Goal<Agent>> goal = currentMob.getGoal(GoalTypes.TARGET);
			if (goal.isPresent()) {
				goal.get().clear(); //remove other goals, you will do as we command
				goal.get().addTask(Integer.MAX_VALUE, task);
			}
			Optional<Goal<Agent>> goal2 = currentMob.getGoal(GoalTypes.NORMAL);
			if (goal2.isPresent()) {
				goal2.get().clear(); //remove other goals, you will do as we command
				goal2.get().addTask(Integer.MAX_VALUE, task);
			}
			//works only for hostile mobs
			currentMob.setTarget(e);
		}
	}
	
	public void tickMob() {
		World w = lastKnownPos.getExtent();
		Optional<Chunk> chunk = w.getChunkAtBlock(lastKnownPos.getBlockPosition()); 
		if (currentMob == null) {
			if (chunk.isPresent() && chunk.get().isLoaded()) {
				//first try to link the entity again:
				Collection<Entity> ents = chunk.get().getEntities();
				for (Entity ent : ents) {
					if ( ent.getType().equals(mobType) && ent.isLoaded() && (ent.getUniqueId().equals(lastKnownID) ))
					{	//check if npc already belongs to a different shop
//						HelpMates.l("Adapted a %s", ent.getClass().getSimpleName());
						currentMob = (Creature)ent;
						lastKnownPos = currentMob.getLocation();
						break;
					}
				}
				if (currentMob == null) { //unable to restore
					Entity newAgent = w.createEntity(mobType, lastKnownPos.getPosition());
			        newAgent.offer(Keys.AI_ENABLED, false);
			        newAgent.offer(Keys.IS_SILENT, true);
			        setMobTitle();
			        newAgent.offer(Keys.INVULNERABLE, true);
			        if (w.spawnEntity(newAgent)) {
			        	lastKnownID = newAgent.getUniqueId();
			        	currentMob =(Creature) newAgent;
//			        	HelpMates.l("Respawned Mob");
			        } else {
//			        	HelpMates.l("Could not respawn Mob");
//			        	mewAgent.remove();
			        }
				}
			}
		} else if (currentMob != null) {
			if (!currentMob.isLoaded() || !chunk.isPresent() || !chunk.get().isLoaded()) {
				currentMob = null;	//allowing minecraft to free the resources
			} else  if (currentMob.isRemoved() || currentMob.get(Keys.HEALTH).orElse(1.0)<=0) { 
				currentMob = null;
			} else {
				if (myscript != null) {
					if (myscript.isDone()) {
						myscript = null;
						setMobTitle();
					} else if (fuelLevel > 0) {
						fuelLevel --;
						if (myscript.tick())
							despawnHelper();
					} else {
						setError("I'm too hungry, please give me something to eat");
					}
				}
				lastKnownPos = currentMob.getLocation();
				if (target != null && targetHelper != null) {
					currentMob.setTarget(targetHelper);
				}
			}
		}
	}
	public void setOpenCarrier(InventoryTile te) {
		useWith = te;
	}
	public Optional<InventoryTile> getOpenCarrier() {
		return Optional.ofNullable(useWith);
	}
	public Inventory getInventory() {
		return inventory;
	}
	public void dropInventory() {
		//drop inventory
		Item item;
		for (Inventory slot : inventory.slots()) {
			Optional<ItemStack> stack = slot.peek();
			if (stack.isPresent()) {
				item = (Item) lastKnownPos.getExtent().createEntity(EntityTypes.ITEM, lastKnownPos.getPosition());
				item.offer(Keys.REPRESENTED_ITEM, stack.get().createSnapshot());
				item.setCreator(ownerID);
//				l("Dropping %d %s", stack.get().getQuantity(), stack.get().getType().getId());
				lastKnownPos.getExtent().spawnEntity(item);
			}
		}
	}
	
	/** will reduce the stack size until 0 or max fuel level of 100000 (~25000s / ~7h)
	 * @returns the amount of consumed items from the stack
	 */
	public int increaseFuelLevel(ItemStack stack, int max) {
//		HelpMates.l("Consuming %d %s", stack.getQuantity(), stack.getType().getId());
		if (!stack.getType().equals(ItemTypes.REDSTONE)) return 0;
		int energy = 900; //amount, one dust increases energy
		int quantity = stack.getQuantity();
//		HelpMates.l(" Gives %d times %d", quantity, energy);
		long space = Math.min((MaxFuelLevel - fuelLevel)/energy, max); //maximum items that could be consumed before overflowing fuellevel
		int consume = (int)Math.min(space, quantity);
//		HelpMates.l("  Can consume %d", consume);
		if (consume >= quantity) {
			stack.setQuantity(0);
			fuelLevel += quantity * energy;
			return quantity;
		} else if (consume > 0) {
			stack.setQuantity(quantity-consume);
			fuelLevel += consume * energy;
			return consume;
		} else return 0;
	}
	public int increaseFuelLevel(ItemStack stack) {
		return increaseFuelLevel(stack, stack.getQuantity());
	}
	
	public void setError(String error) {
		this.error = error;
		if (myscript != null) myscript = null; 
		setTarget(null);
		setMobTitle();
	}
	public Optional<String> getError() {
		return Optional.ofNullable(error);
	}
	private void setMobTitle() {
		if (currentMob != null) {
			if (error != null)
				currentMob.offer(Keys.DISPLAY_NAME, Text.of(TextColors.RED, "I have an error"));
			else {
				currentMob.offer(Keys.DISPLAY_NAME, Text.of((myscript==null?TextColors.WHITE:TextColors.GREEN), getMobTitle()));
			}
		}
	}
	private String getMobTitle() {
		String on = ownerName;
		if (on.endsWith("s")) on+="'";
		else on+="'s";
		return on+" Robot";
	}
	public static String getRuntime(long fuelLevel) {
		long main=fuelLevel/4, second=0;
		int index=0;
		long[] max = {60,60,24};
		String[] units = {"s", "m", "h", "d"};
		while (main > max[index] && index+1<max.length) {
			second = main%max[index];
			main = main/max[index];
			index++;
		}
		String result = (main > 0 ? main+units[index] : "") + (second > 0 ? second+units[index-1] : "");
		return main == 0 ? "0" : result;
	}
}
