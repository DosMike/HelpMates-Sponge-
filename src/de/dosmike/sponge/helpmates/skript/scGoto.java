package de.dosmike.sponge.helpmates.skript;

import java.util.Optional;

import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import de.dosmike.sponge.helpmates.Worker;

/** This command causes the worker to go to a given location or lookup a location in the variable dictionary */
public class scGoto implements SkriptCommand {
	
	Worker thisWorker;
	Object targetLocation;
	Location<World> liveTarget; //only known at runtime
	/** @param target has to be either String (varName) or ocation&lt;World> */
	public scGoto(Worker robot, Object target) {
		thisWorker = robot;
		targetLocation = target;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(boolean first) {
		if (!first) return;
		if (targetLocation instanceof Location<?>) {
			liveTarget = (Location<World>)targetLocation;
			thisWorker.setTarget(liveTarget);
		} else if (targetLocation instanceof String) {
			Optional<Location<World>> target = thisWorker.getSkript().getVariable((String)targetLocation);
			if (!target.isPresent())
				thisWorker.setError("I don't know a location named '"+(String)targetLocation+"'");
			else {
				liveTarget = target.get();
				thisWorker.setTarget(liveTarget);
			}
		} else {
			thisWorker.setError("BEEP BOOP unable to resolve variable "+targetLocation);
		}
	}
	
	@Override
	public boolean isDone() {
		boolean done = (thisWorker.getAgent().getLocation().getExtent().equals(liveTarget.getExtent()))&&(thisWorker.getAgent().getLocation().getPosition().distanceSquared(liveTarget.getPosition()) <= 1.0+thisWorker.getAgentRadius());
		return done;
	}
	
	@Override
	public String toString() {
		if (targetLocation instanceof Location<?>) {
			@SuppressWarnings("unchecked")
			Location<World> serial = (Location<World>) targetLocation;
			return "GoTo "+serial.getExtent().getName()+" "+serial.getBlockX()+" "+serial.getBlockY()+" "+serial.getBlockZ();
		} else if (targetLocation instanceof String) {
			return "GoTo "+(String)targetLocation;
		} else {
			return "//GoTo ?";
		}
	}
}
