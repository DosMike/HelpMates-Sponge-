package de.dosmike.sponge.helpmates.skript;

/** This command will always succeed the specified command, meaning it will only trigger the first time */
public class scTry implements SkriptCommand {
	
	SkriptCommand toTry;
	boolean isDone;
	public scTry(SkriptCommand other) {
		toTry = other;
	}
	
	@Override
	public void execute(boolean first) {
		isDone = !first;
		toTry.execute(first);
		isDone = true;
	}
	
	@Override
	public String toString() {
		return "Try to "+toTry.toString();
	}

	@Override
	public boolean isDone() {
		return isDone;
	}
}
