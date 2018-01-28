package de.dosmike.sponge.helpmates.skript;

import java.util.Optional;

import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;

import de.dosmike.sponge.helpmates.HelpMates;
import de.dosmike.sponge.helpmates.Worker;

/** this command will try to use a block at a specified location. If it is a carrier the carrier will be used for inventory commands (take put fuel) */
public class scUse extends scGoto {
	
	public scUse(Worker robot, Object target) {
		super(robot, target);
	}
	
	@Override
	public boolean isDone() {
		boolean done = super.isDone();
		if (done) {
			if (SkriptCommand.testUseBlockPermission(thisWorker, liveTarget, false)) {
				Optional<TileEntity> te = liveTarget.getTileEntity();
				if (te.isPresent()) {
					HelpMates.l("TileEntity: %s", te.get().getClass().getSimpleName());
					if (te.get() instanceof Carrier) HelpMates.l("  is Carrier");
					if (te.get() instanceof Inventory) HelpMates.l("  is Inventory");
				}
				if (te.isPresent() && te.get() instanceof Carrier) {
					thisWorker.setOpenCarrier((Carrier)te.get());
				} else
				if (liveTarget.supports(Keys.OPEN) && !liveTarget.getBlockType().equals(BlockTypes.IRON_DOOR)) {
					boolean state = liveTarget.get(Keys.OPEN).orElse(false);
					liveTarget.offer(Keys.OPEN, !state);
				} else 
				if (liveTarget.supports(Keys.POWERED)) {
					boolean state = liveTarget.get(Keys.POWERED).orElse(false);
					liveTarget.offer(Keys.POWERED, !state);
				}
			}
		}
		return done;
	}
	
	@Override
	public String toString() {
		String gotoString = super.toString();
		
		return "Use" + gotoString.substring(4);
	}
}
