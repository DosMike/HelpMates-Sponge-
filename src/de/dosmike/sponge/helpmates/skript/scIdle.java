package de.dosmike.sponge.helpmates.skript;

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.property.block.SolidCubeProperty;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import de.dosmike.sponge.helpmates.HelpMates;
import de.dosmike.sponge.helpmates.Worker;

public class scIdle implements SkriptCommand {
	
	Worker thisWorker;
	long targetTime = 0;
	int radius;
	int waittime;
	boolean atTarget = false;
	int lookTicks = -50;
	Location<World> liveTarget;
	public scIdle(Worker robot, int radius, int maxwait) {
		thisWorker = robot;
		if (radius <= 0) {
			throw new RuntimeException("Can't idle around negative radius");
		}
		if (maxwait <= 0) {
			throw new RuntimeException("Can't idle for negative seconds");
		}
		this.radius = radius;
		this.waittime = maxwait;
	}
	
	@Override
	public void execute(boolean first) {
		if (!first) {
			if (atTarget) {
				if (thisWorker.getTarget().isPresent())
					thisWorker.setTarget(null);
				if (HelpMates.rng.nextInt(100) < ++lookTicks) {
					HelpMates.l("Looking!");
					//turn around a bit to make them feel more alive
					thisWorker.getAgent().setHeadRotation(new Vector3d(
							HelpMates.rng.nextDouble()*90-45,
							HelpMates.rng.nextDouble()*90-45+thisWorker.getAgent().getRotation().getY(),
							0.0));
					lookTicks=-50;
				}
			}
			return;
		}
		lookTicks=-50;
		atTarget = false;
		targetTime = System.currentTimeMillis()+(HelpMates.rng.nextInt(waittime)*1000l);
//		HelpMates.l("Set target time to %d", targetTime);
		//dice new location
		liveTarget=null;
		for (int r=radius;r>0&&liveTarget==null;r--)
			for (int i=0;i<10;i++) {
				Location<World> l = thisWorker.getAgent().getLocation();
				Location<World> t = l.add(HelpMates.rng.nextInt(r*2)-r, 0, HelpMates.rng.nextInt(r*2)-r);
				SolidCubeProperty solid = t.getBlockRelative(Direction.DOWN).getBlock().getProperty(SolidCubeProperty.class).orElse(null);
				if (solid == null) continue;
				if (t.getBlockType().equals(BlockTypes.AIR) && solid.getValue())
					liveTarget = t;
			}
		if (liveTarget != null)
			thisWorker.setTarget(liveTarget);
	}
	
	@Override
	public boolean isDone() {
//		HelpMates.l("Delta %d >= %d", System.currentTimeMillis(), targetTime);
		if ((thisWorker.getAgent().getLocation().getExtent().equals(liveTarget.getExtent()))&&
			(thisWorker.getAgent().getLocation().getPosition().distanceSquared(liveTarget.getPosition()) <= 1.0+thisWorker.getAgentRadius()))
			atTarget = true;
		boolean done = (atTarget) &&
				(System.currentTimeMillis()>=targetTime) ;
		return done;
	}
	
	@Override
	public String toString() {
		return "Idle within "+radius+" for "+waittime;
	}
}
