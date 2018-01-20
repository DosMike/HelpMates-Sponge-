package de.dosmike.sponge.helpmates.skript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;

import de.dosmike.sponge.helpmates.Worker;

public class Skript {
	List<SkriptCommand> cmds = new ArrayList<>();
//	Iterator<SkriptCommand> cmd = null;
	int cmdpos = 0;
	SkriptCommand current=null;
	boolean done = false;
	boolean first = false;
	
	/** @return true if stepped */
	public boolean tick() {
		if (current == null) {
			cmdpos=0;
			current = cmds.get(cmdpos);
			current.execute(true);
			first = false;
		} else if (!first && current.isDone()) {
			step();
			return true;
		} else {
			current.execute(first);
			first = false;
		}
		return false;
	}
	void step() {
		cmdpos++;
		first = true;
		if (cmdpos < cmds.size()){
			current = cmds.get(cmdpos);
//			current.execute();
		} else {
			done = true;
		}
	}
	public void restart() {
		current = null;
	}
	public boolean isDone() {
		return done;
	}

	private Map<String, Location<World>> variables = new HashMap<>();
	void setVariable(String name, Location<World> location) {
		variables.put(name, location);
	}
	Optional<Location<World>> getVariable(String name) {
		return Optional.ofNullable(variables.get(name));
	}
	
	static Optional<SkriptCommand> parseCommand(Worker forWorker, String command) {
		command = command.trim();
		if (command.isEmpty() || command.startsWith("#") || command.startsWith("//") || command.startsWith("'"))
			return Optional.empty(); //empty lines and comments
		
		String[] parts = command.split(" ");
		if (parts.length > 3 && parts[1].equalsIgnoreCase("is") && parts[2].equalsIgnoreCase("at")) {
			if (!parts[0].matches("\\w*"))
				throw new RuntimeException("Location names may only container letters from A-Z, digits from 0-9 and/or the underscore _");
			try {
				Location<World> target;
				if (parts.length == 6)
					target = forWorker.getAgent().getLocation().getExtent().getLocation(new Vector3d( Integer.parseInt(parts[3])+0.5, Integer.parseInt(parts[4])+0.5, Integer.parseInt(parts[5])+0.5 ));
				else if (parts.length == 7)
					target = Sponge.getServer() .getWorld(parts[3]).get().getLocation(new Vector3d( Integer.parseInt(parts[4])+0.5, Integer.parseInt(parts[5])+0.5, Integer.parseInt(parts[6])+0.5 ));
				else 
					throw new RuntimeException("Invalid location for "+parts[0]+": '[world] X Y Z' expected");
				return Optional.of(new scVariable(forWorker, parts[0], target));
			} catch (Exception e) {
				throw new RuntimeException("Invalid Location for Variable: "+e.getMessage());
			}
		} else if ((parts[0].equalsIgnoreCase("GoTo"))) {
			try {
				Object target;
				if (parts.length == 4) {
					target = forWorker.getAgent().getLocation().getExtent().getLocation(new Vector3d( Integer.parseInt(parts[1])+0.5, Integer.parseInt(parts[2])+0.5, Integer.parseInt(parts[3])+0.5 ));
				} else if (parts.length == 5) {
					target = Sponge.getServer() .getWorld(parts[1]).get().getLocation(new Vector3d( Integer.parseInt(parts[2])+0.5, Integer.parseInt(parts[3])+0.5, Integer.parseInt(parts[4])+0.5 ));
				} else if (parts.length == 2){
					target = parts[1];
				} else {
					throw new RuntimeException("Invalid use of GoTo: location name or '[world] X Y Z' expected");
				}
				return Optional.of(new scGoto(forWorker, target));
			} catch (Exception e) {
				throw new RuntimeException("Invalid Location for GoTo: "+e.getMessage());
			}
		} else if ((parts[0].equalsIgnoreCase("Use"))) {
			try {
				Object target;
				if (parts.length == 4) {
					target = forWorker.getAgent().getLocation().getExtent().getLocation(new Vector3d( Integer.parseInt(parts[1])+0.5, Integer.parseInt(parts[2])+0.5, Integer.parseInt(parts[3])+0.5 ));
				} else if (parts.length == 5) {
					target = Sponge.getServer() .getWorld(parts[1]).get().getLocation(new Vector3d( Integer.parseInt(parts[2])+0.5, Integer.parseInt(parts[3])+0.5, Integer.parseInt(parts[4])+0.5 ));
				} else if (parts.length == 2){
					target = parts[1];
				} else {
					throw new RuntimeException("Invalid use of Use: location name or '[world] X Y Z' expected");
				}
				return Optional.of(new scUse(forWorker, target));
			} catch (Exception e) {
				throw new RuntimeException("Invalid Location for Use: "+e.getMessage());
			}
		} else if (parts[0].equalsIgnoreCase("Repeat") && parts.length == 1) {
			return Optional.of(new scRepeat(forWorker));
		} else if (parts[0].equalsIgnoreCase("Take") && parts.length > 1) {
			return Optional.of(new scTake(forWorker, Arrays.copyOfRange(parts, 1, parts.length)));
		} else if (parts[0].equalsIgnoreCase("Put") && parts.length > 1) {
			return Optional.of(new scGive(forWorker, Arrays.copyOfRange(parts, 1, parts.length)));
		} else if (parts[0].equalsIgnoreCase("Fuel") && parts.length > 1) {
			return Optional.of(new scFuel(forWorker, Arrays.copyOfRange(parts, 1, parts.length)));
		} else if (parts[0].equalsIgnoreCase("Charge") && parts.length > 1) {
			try {
				int am = Integer.parseInt(parts[1]);
				if (am <= 0) throw new RuntimeException();
				return Optional.of(new scCharge(forWorker, am));
			} catch (Exception e) {
				throw new RuntimeException("Invalid use of Charge: requires a positive number as limit");
			}
		} else if (parts[0].equalsIgnoreCase("TpTo")) {
			try {
				Object target;
				if (parts.length == 4) {
					target = forWorker.getAgent().getLocation().getExtent().getLocation(new Vector3d( Integer.parseInt(parts[1])+0.5, Integer.parseInt(parts[2])+0.5, Integer.parseInt(parts[3])+0.5 ));
				} else if (parts.length == 5) {
					target = Sponge.getServer() .getWorld(parts[1]).get().getLocation(new Vector3d( Integer.parseInt(parts[2])+0.5, Integer.parseInt(parts[3])+0.5, Integer.parseInt(parts[4])+0.5 ));
				} else if (parts.length == 2){
					target = parts[1];
				} else {
					throw new RuntimeException("Invalid use of TpTo: location name or '[world] X Y Z' expected");
				}
				return Optional.of(new scTeleport(forWorker, target));
			} catch (Exception e) {
				throw new RuntimeException("Invalid Location for TpTo: "+e.getMessage());
			}
		} else if (parts[0].equalsIgnoreCase("Harvest") && parts.length > 1) {
			return Optional.of(new scFarm(forWorker, Arrays.copyOfRange(parts, 1, parts.length)));
		} else if (parts[0].equalsIgnoreCase("Wait") && parts.length == 2) {
			return Optional.of(new scWait(forWorker, Integer.parseInt(parts[1])));
		} else if (parts[0].equalsIgnoreCase("Try")) {
			int offset = 4;
			if (parts.length>2 && parts[1].equalsIgnoreCase("to")) offset += 3;
			String subcommand = command.substring(offset);
			if (subcommand.startsWith("Try")) 
				throw new RuntimeException("Can't try to try");
			Optional<SkriptCommand> scmd = parseCommand(forWorker, subcommand);
			if (!scmd.isPresent()) 
				throw new RuntimeException("You have to write a command to try after `Try to`");
			return Optional.of(new scTry(scmd.get()));
		}
		throw new RuntimeException("Unknown command "+command);
	}
	
	public static Skript parseScript(Worker forWorker, String string) {
		Skript result = new Skript();
		String[] lines = string.split("\n");
		int lineno = 1;
		try {
			for (String line : lines) {
//				HelpMates.l("Parsing %s", line);
				Optional<SkriptCommand> cmd = parseCommand(forWorker, line);
				if (cmd.isPresent()) {
//					HelpMates.l("+ %s", cmd.get().getClass().getSimpleName());
					result.cmds.add(cmd.get());
				} else {
//					HelpMates.l("+ Comment");
				}
				lineno++;
			}
		} catch (Exception e) {
			throw new RuntimeException("You script has an error on Line "+lineno, e);
		}
		return result;
	}
	public static Skript parseScriptLines(Worker forWorker, List<String> lines) {
		Skript result = new Skript();
		int lineno = 1;
		try {
			for (String line : lines) {
//				HelpMates.l("Parsing %s", line);
				Optional<SkriptCommand> cmd = parseCommand(forWorker, line);
				if (cmd.isPresent()) {
//					HelpMates.l("+ %s", cmd.get().getClass().getSimpleName());
					result.cmds.add(cmd.get());
				} else {
//					HelpMates.l("+ Comment");
				}
				lineno++;
			}
		} catch (Exception e) {
			throw new RuntimeException("You script has an error on Line "+lineno, e);
		}
		return result;
	}
	public static Skript parseScript(Worker forWorker, Text text) {
		return parseScript(forWorker, text.toPlain());
	}
	public static Skript parseScript(Worker forWorker, List<Text> pages) {
		return parseScript(forWorker, Text.joinWith(Text.NEW_LINE, pages));
	}
	public String currentAction() {
		return current != null ? current.toString() : "Initializing";
	}
	public List<String> serialize() {
		List<String> result = new ArrayList<>(cmds.size());
		for (SkriptCommand cmd : cmds) {
			result.add(cmd.toString());
		}
		return result;
	}
}
