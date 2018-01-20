package de.dosmike.sponge.helpmates.skript;

import de.dosmike.sponge.helpmates.Worker;

public class scWait implements SkriptCommand {
	
	Worker thisWorker;
	long targetTime = 0;
	int seconds;
	public scWait(Worker robot, int seconds) {
		thisWorker = robot;
		if (seconds <= 0) {
			throw new RuntimeException("Can't wait a negative time");
		}
		this.seconds = seconds;
	}
	
	@Override
	public void execute(boolean first) {
		if (first) {
			targetTime = System.currentTimeMillis()+(seconds*1000l);
//			HelpMates.l("Set target time to %d", targetTime);
		}
	}
	
	@Override
	public boolean isDone() {
//		HelpMates.l("Delta %d >= %d", System.currentTimeMillis(), targetTime);
		return System.currentTimeMillis()>=targetTime;
	}
	
	@Override
	public String toString() {
		return "Wait "+seconds;
	}
}
