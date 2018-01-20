package de.dosmike.sponge.helpmates.skript;

import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import de.dosmike.sponge.helpmates.Worker;

public interface SkriptCommand {
	public void execute(boolean first);
	public boolean isDone();
	
	/**
	 * Checks if this worker is allowed the interact with a certain block.<br>
	 * This requires protection plugins to check context simulated player or user in the cause stack!
	 * At the time of writing this NO protection plugin seems to do this so it will always succeed
	 * 
	 * @param block the usable block to test for with {@link InteractBlockEvent.Secondary.MainHand}
	 * @param silent if true the robot will not automatically go into error state if this method would return false
	 * @return false if a permission plugin cancelled this event due to permissive restrictions
	 */
	static boolean testUseBlockPermission(Worker worker, Location<World> block, boolean silent) {
		try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
			if (worker.getOwner().isPresent()) { 
				frame.addContext(EventContextKeys.PLAYER_SIMULATED, worker.getOwner().get().getProfile());
				frame.addContext(EventContextKeys.OWNER, worker.getOwner().get()); //appropriate?
				frame.pushCause(worker.getOwner().get()); //since the worker was spawned by this user, but the use may be offline
			}
			frame.pushCause(worker);
			
//			Sponge.getCauseStackManager().getCurrentCause().forEach(cause->HelpMates.l("Cause by %s", cause.toString()));
			
			//use block, never use item in hand
			Tristate useBlockResult=Tristate.TRUE, originalUseBlockResult=Tristate.TRUE, useItemResult=Tristate.FALSE, originalUseItemResult=Tristate.FALSE;
			InteractBlockEvent.Secondary.MainHand event = SpongeEventFactory.createInteractBlockEventSecondaryMainHand(Sponge.getCauseStackManager().getCurrentCause(), 
					originalUseBlockResult, useBlockResult, originalUseItemResult, useItemResult, 
					HandTypes.MAIN_HAND, Optional.empty(), block.createSnapshot(), Direction.UP);
			Sponge.getEventManager().post(event);
			if (event.isCancelled() && !silent)
				worker.setError("I'm not permitted to interact with this");
			return !event.isCancelled();
		}
	}
	
	/** 
	 * Checks if the workers owner has a certain permission
	 * @param silent if true the robot will not automatically go into error state if this method would return false
	 * @return true if the owner has permission or the worker has no owner (assuming it was spawned by an admin-like entity) 
	 */
	static boolean testOwnerPermission(Worker worker, String permission, boolean silent) {
		Optional<User> owner = worker.getOwner();
		if (owner.isPresent()) {
			if (owner.get().hasPermission(permission)) return true;
			if (!silent) worker.setError("I'm not powerfull enough to teleport");
			return false;
		} else return true;
	}
	
	/**
	 * Breaks the block by simulating a digBlock event if the worker has a owner.
	 * As the digBlock function requires a GameProfile this will never work for workers without owner.
	 * 
	 * Any drop can be catched by listening to BreakBlockEvent with the Worker as cause
	 * 
	 * Please note, that World->Extent->InteractableVolume->digBlock is not yet implemented
	 * 
	 * @return true if the block was destroyed 
	 */
	static boolean breakBlock(Worker worker, Location<World> at, ItemStack with, boolean silent) {
		if (!worker.getOwner().isPresent()) {
			worker.setError("PC Could not break block at "+at.getBlockPosition().toString());
			return false;
		}
		try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
			frame.addContext(EventContextKeys.PLAYER_SIMULATED, worker.getOwner().get().getProfile());
			frame.addContext(EventContextKeys.OWNER, worker.getOwner().get()); //appropriate?
			frame.pushCause(worker.getOwner().get()); //since the worker was spawned by this user, but the use may be offline
			frame.pushCause(worker);
			boolean success = false;				
//			success = at.getExtent().digBlockWith(at.getBlockPosition(), with, worker.getOwner().get().getProfile());
			if (!success && !silent) worker.setError("Could not break block at "+at.getBlockPosition().toString());
			return success;
		} catch (Exception e) {
			worker.setError("I'm confused!");
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Places a block by simulating a placeBlock event if the worker has a owner.
	 * As the placeBlock function requires a GameProfile this will never work for workers without owner.
	 * This method will also fail if there is no blockState for this item
	 * 
	 * Please note, that World->Extent->InteractableVolume->placeBlock is not yet implemented
	 * 
	 * @param side will go relative to {@link at} and place on the opposite face
	 * @return true if the block was placed 
	 */
	static boolean placeBlock(Worker worker, Location<World> at, Direction side, ItemType fromItem, boolean silent) {
		if (!worker.getOwner().isPresent() || !fromItem.getBlock().isPresent()) {
			worker.setError("PC Could not place block at "+at.getBlockPosition().toString());
			return false;
		}
		try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
			frame.addContext(EventContextKeys.PLAYER_SIMULATED, worker.getOwner().get().getProfile());
			frame.addContext(EventContextKeys.OWNER, worker.getOwner().get()); //appropriate?
			frame.pushCause(worker.getOwner().get()); //since the worker was spawned by this user, but the use may be offline
			frame.pushCause(worker);

			//might require a block down?
			boolean success = false;
//			success = at.getExtent().placeBlock(at.getBlockRelative(side).getBlockPosition(), fromItem.getBlock().get().getDefaultState(), side.getOpposite(), worker.getOwner().get().getProfile());
			if (!success && !silent) worker.setError("Could not place block at "+at.getBlockPosition().toString());
			return success;
		} catch (Exception e) {
			worker.setError("I'm confused!");
			e.printStackTrace();
			return false;
		}
	}
}
