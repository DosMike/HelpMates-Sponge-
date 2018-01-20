package de.dosmike.sponge.helpmates.skript;

import de.dosmike.sponge.helpmates.Worker;

/** This command will set the Skript instruction pointer back to the beginning */
public class scRepeat implements SkriptCommand {
	
	Worker thisWorker;
	public scRepeat(Worker robot) {
		thisWorker = robot;
	}
	
	@Override
	public void execute(boolean first) {
		if (!first) return;
		thisWorker.getSkript().restart();
	}
	
	@Override
	public boolean isDone() {
		return true;
	}
	
	@Override
	public String toString() {
		return "Repeat";
	}
}
