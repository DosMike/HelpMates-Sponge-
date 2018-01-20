package de.dosmike.sponge.helpmates.skript;

import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3i;

import de.dosmike.sponge.helpmates.Worker;

public class scVariable implements SkriptCommand {
	
	Worker thisWorker;
	String varName;
	Location<World> targetLocation;
	
	public scVariable(Worker robot, String name, Location<World> at) {
		thisWorker = robot;
		varName = name;
		targetLocation = at;
	}
	
	@Override
	public void execute(boolean first) {
		thisWorker.getSkript().setVariable(varName, targetLocation);
	}
	
	@Override
	public boolean isDone() {
		return true;
	}
	
	@Override
	public String toString() {
		Vector3i serialPos = targetLocation.getBlockPosition();
		return varName+" is at "+targetLocation.getExtent().getName()+" "+serialPos.getX()+" "+serialPos.getY()+" "+serialPos.getZ();
	}
}
