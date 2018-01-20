package de.dosmike.sponge.helpmates.skript;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.block.GrowthData;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;

import com.flowpowered.math.vector.Vector3i;

import de.dosmike.sponge.helpmates.HelpMates;
import de.dosmike.sponge.helpmates.Worker;

/** this command will try to use a block at a specified location. If it is a carrier the carrier will be used for inventory commands (take put fuel) */
public class scFarm implements SkriptCommand {
	
	boolean done=false;
	Worker thisWorker;
	String[] what;
	
	boolean walking=true;
	Location<World> farmTarget = null;
	Set<Vector3i> farmLand = null;
	Set<Vector3i> harvest = null;
	boolean any;
	ItemType plant = null;
	
	public scFarm(Worker robot, String... target) {
		what = target;
		thisWorker = robot;
		for (String s : what) if ("anything".equalsIgnoreCase(s)) { any = true; break; }
	}
	
	@Override
	public boolean isDone() {
		return done;
	}
	
	@Override
	public void execute(boolean first) {
		if (first) { //initialize variables and scan field
			done=false; 
			farmLand = null; 
			walking = false; 
			harvest = new HashSet<>(); 
			scanLand(); //get the whole field
			if (farmLand == null || farmLand.isEmpty()) {
				done = true;
			}
		} else if (walking) {
			if ((thisWorker.getAgent().getLocation().getExtent().equals(farmTarget.getExtent()))&&(thisWorker.getAgent().getLocation().getPosition().distanceSquared(farmTarget.getPosition()) <= 4.0)) {
				walking = false;
				thisWorker.setTarget(null);
			}
		} else if (harvest.isEmpty()) { //currently nothing in range/selected
			Set<Vector3i> remove = getInRange(); //get all crops the worker can harvest from his current location
			HelpMates.l("%d in range", remove.size());
			if (remove.isEmpty()) { //there's no more fields in range
				if (farmLand.isEmpty()) {
					done = true;
					return;
				} else { //we still have farmland to visit, go there 
					Optional<Vector3i> ripe = getNextClosest();//closest harvestable from the whole field
					if (!ripe.isPresent()) { //nothing to harvest on this farmland
						done = true;
						return ;
					}
					//go to the closest harvestable
					farmTarget = thisWorker.getAgent().getLocation().getExtent().getLocation(ripe.get());
					walking = true;
					thisWorker.setTarget(farmTarget);
					
					plant = null; //reset the plant holder
				}
			}
			harvest = getHarvestableOf(remove); //filter those that are ripe
			farmLand.removeAll(remove); //remove all from the super list, these fields have been looked at
		} else if (plant == null) { //last execute we planted, so this execute we harvest
			World world = thisWorker.getAgent().getLocation().getExtent();
			Vector3i at = harvest.iterator().next();
			farmTarget = world.getLocation(at);
			harvest.remove(at);
			Optional<GrowthData> gd = farmTarget.get(GrowthData.class);
			if (!gd.isPresent()) {
				return;
			}
			//check again, could be replanted and now not be ready
			int maxAge = world.getLocation(at).getBlockType().getAllBlockStates().size()-1; //as the MutableBoundValue::max is always 15... what a ripoff ;P
			MutableBoundedValue<Integer> age = gd.get().growthStage();
			if (age.get() == maxAge) {
				if (SkriptCommand.breakBlock(thisWorker, farmTarget, ItemStack.of(ItemTypes.AIR, 1), false));
					plant = farmTarget.getBlock().getType().getItem().orElse(null); //should return the crops for this plant
			}
		} else {
			Optional<ItemStack> plantThis = thisWorker.getInventory().query(QueryOperationTypes.ITEM_TYPE.of(plant)).poll(1);
			if (plantThis.isPresent() && plantThis.get().getQuantity() > 0)
				SkriptCommand.placeBlock(thisWorker, farmTarget, Direction.DOWN, plant, false);
			
		}
	}
	
	void scanLand() {
		BlockType[] toharvest = {};
		if (!any) {
			toharvest = new BlockType[what.length];
			int i = 0;
			for (String s : what) {
				Optional<ItemType> it = Sponge.getRegistry().getType(ItemType.class, s);
				Optional<BlockType> bt = Sponge.getRegistry().getType(BlockType.class, s);
				if (bt.isPresent()) toharvest[i] = bt.get();
				else if (it.isPresent() && it.get().getBlock().isPresent()) toharvest[i] = it.get().getBlock().get();
				else {
					thisWorker.setError("I don't know how to harvest any "+s); 
					return;
				}
				if (!toharvest[i].getDefaultState().supports(Keys.GROWTH_STAGE)) {
					thisWorker.setError("I don't thing "+s+" is a harvesable plant");
					return;
				}
			}
		}
		
		Location<World> start = thisWorker.getAgent().getLocation().add(0, 0.5, 0); //supposed to be at crop hight reguardless of in- or outside farm
		for (int z=-2; z<3; z++)
			for (int x=-2; x<3; x++) {
				Location<World> relative = start.add(x, 0, z);
				if ((any || ArrayUtils.contains(toharvest, relative.getBlockType())) && relative.getBlock().supports(Keys.GROWTH_STAGE)) {
					farmLand = new HashSet<>();
					scanLand(relative, 0);
					return;
				}
			}
				
	}
	void scanLand(Location<World> around, int overflowGuard) {
		if (overflowGuard >= 32 || farmLand.contains(around.getBlockPosition())) return;
		if (!around.getBlock().supports(Keys.GROWTH_STAGE)) {
			return;
		}
		farmLand.add(around.getBlockPosition());
		scanLand(around.getRelative(Direction.NORTH), overflowGuard+1);
		scanLand(around.getRelative(Direction.EAST), overflowGuard+1);
		scanLand(around.getRelative(Direction.SOUTH), overflowGuard+1);
		scanLand(around.getRelative(Direction.WEST), overflowGuard+1);
	}
	Set<Vector3i> getInRange() {
		return farmLand.stream().filter(block-> block.distanceSquared(thisWorker.getAgent().getLocation().getBlockPosition())<20.0 )
				.collect(Collectors.toSet());
	}
	Set<Vector3i> getHarvestableOf(Set<Vector3i> blocks) {
		Extent ee = thisWorker.getAgent().getLocation().getExtent();
		Vector3i agentBlock = thisWorker.getAgent().getLocation().getBlockPosition();
		return blocks.stream().filter(block->{
			Optional<GrowthData> growth = ee.getLocation(block).get(GrowthData.class);
			int maxAge = ee.getLocation(block).getBlockType().getAllBlockStates().size()-1; //as the MutableBoundValue::max is always 15... what a ripoff ;P
//			HelpMates.l("Growth at %s: %d/%d (%d)", block.toString(), growth.get().growthStage().get(), growth.get().growthStage().getMaxValue(), maxAge);
			return growth.isPresent() && growth.get().growthStage().get() == maxAge;
		})
		.sorted(new Comparator<Vector3i>() {
			@Override
			public int compare(Vector3i o1, Vector3i o2) {
				return Double.compare(agentBlock.distanceSquared(o1), agentBlock.distanceSquared(o1));
			}
		})
		.collect(Collectors.toSet());
	}
	Optional<Vector3i> getNextClosest() {
		Extent ee = thisWorker.getAgent().getLocation().getExtent();
		Vector3i agentBlock = thisWorker.getAgent().getLocation().getBlockPosition();
		Set<Vector3i> meta = farmLand.stream().filter(block->{
			Optional<GrowthData> growth = ee.getLocation(block).get(GrowthData.class);
			return growth.isPresent() && growth.get().growthStage().get() == growth.get().growthStage().getMaxValue();
		})
		.sorted(new Comparator<Vector3i>() {
			@Override
			public int compare(Vector3i o1, Vector3i o2) {
				return Double.compare(agentBlock.distanceSquared(o1), agentBlock.distanceSquared(o1));
			}
		})
		.collect(Collectors.toSet());
		return (meta.isEmpty() ? Optional.empty() : Optional.of(meta.iterator().next()));
	}
	
	@Override
	public String toString() {
		return "Harvest " + StringUtils.join(what, " ");
	}
}
