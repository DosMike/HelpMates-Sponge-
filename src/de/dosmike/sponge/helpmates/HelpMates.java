package de.dosmike.sponge.helpmates;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.Creature;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.event.world.SaveWorldEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

@Plugin(id="dosmike_helpmates", name="HelpMates", version="0.2", authors={"DosMike"})
public class HelpMates {
	
	public static void main(String[] args) { System.err.println("This plugin can not be run as executable!"); }
	
	static HelpMates instance;
	static HelpMates getInstance() { return instance; }
	static UserStorageService userStorage = null;
	static UserStorageService getUserStorage() { return userStorage; }
	private static SpongeExecutorService scheduler;
	
	@Listener
	public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
		if (event.getService().equals(UserStorageService.class)) {
			userStorage = (UserStorageService) event.getNewProvider();
		}
	}
	
	static PluginContainer getContainer() { return Sponge.getPluginManager().fromInstance(getInstance()).get(); }
	
	@Inject
	private Logger logger;
	public static void l(String format, Object... args) { instance.logger.info(String.format(format, args)); }
	public static void w(String format, Object... args) { instance.logger.warn(String.format(format, args)); }
	
	public static Random rng = new Random(System.currentTimeMillis());
	
	/// --- === Main Plugin stuff === --- \\\ 

	private List<Worker> workers = new LinkedList<>();
	private List<Worker> markDeleted = new LinkedList<>();
	boolean workersDirty = false;
	public void markDeleted(Worker worker) {
		synchronized(workers) {
			markDeleted.add(worker);
		}
	}
	public void addWorker(Worker worker) {
		synchronized(workers) {
			workersDirty=true;
			if (!workers.contains(worker)) 
				workers.add(worker);
		}
	}
	public Collection<Worker> getWorkers(User player) {
		List<Worker> result = new LinkedList<>(); 
		synchronized(workers) {
			for (Worker w : workers) {
				if (w.ownerID.equals(player.getUniqueId())) result.add(w);
			}
		}
		return result;
	}
	public Collection<Worker> getAllWorkers() {
		ArrayList<Worker> copy = new ArrayList<>();
		synchronized(workers) {
			copy.addAll(workers);
		}
		return copy;
	}
	public Optional<Worker> getWorkerByAgent(Creature agent) {
		synchronized(workers) {
			for (Worker worker : workers) {
				if (worker.lastKnownID.equals(agent.getUniqueId())) return Optional.of(worker);
			}
			return Optional.empty();
		}
	}
	public Optional<Worker> getWorkerByLocation(Location<World> at) {
		synchronized(workers) {
			for (Worker worker : workers) {
				if (worker.currentMob != null){
					Location<World> tl = worker.currentMob.getLocation();
					if (tl.getExtent().equals(at.getExtent()) && tl.getPosition().distanceSquared(at.getPosition()) < 1.25)
						return Optional.of(worker);
				}
			}
			return Optional.empty();
		}
	}
	
	@Inject
	@DefaultConfig(sharedRoot = false)
	ConfigurationLoader<CommentedConfigurationNode> configManager;
	@Inject
	@DefaultConfig(sharedRoot = true)
	private Path defaultConfig;
	
	@Listener
	public void onServerInit(GamePreInitializationEvent event) {
		instance = this;
		CraftingRegistra.register();
	}
	
	static ParticleEffect robotError = ParticleEffect.builder().type(ParticleTypes.ANGRY_VILLAGER).velocity(new Vector3d(0.0,0.15,0.0)).build(); 
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		Sponge.getEventManager().registerListeners(this, new EventListener());
		userStorage = (UserStorageService)Sponge.getServiceManager().provide(UserStorageService.class).get();
		
		CommandRegistra.register();
		
		loadConfigs();
		(scheduler=Sponge.getScheduler().createSyncExecutor(this)).scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
				synchronized(workers) {
					for (Worker w : markDeleted) {
						w.myscript = null;
						w.setTarget(null);
						
						//Drop items
						Location<World> at = w.getAgent().getLocation().add(new Vector3d(0.0, 1.0, 0.0));
						Item item = (Item) at.getExtent().createEntity(EntityTypes.ITEM, at.getPosition());
						item.offer(Keys.REPRESENTED_ITEM, CraftingRegistra.iRobotSpawner((int) w.fuelLevel, w.mobType).createSnapshot());
						item.setCreator(w.ownerID);
						at.getExtent().spawnEntity(item);
						
						w.dropInventory();
						
						//actually remove
						if (w.currentMob!=null) {
							w.currentMob.remove();
							w.currentMob = null;
						}
						workers.remove(w);
					}
					markDeleted.clear();
					for (Worker w : workers) {
						w.tickMob();
						if (w.getError().isPresent() && rng.nextInt(15)<2 && System.currentTimeMillis()-w.lastErrNotice>1500) { //play error notice
							w.lastErrNotice = System.currentTimeMillis();
							Location<World> at = w.getAgent().getLocation();
							at.getExtent().playSound(SoundTypes.ENTITY_VILLAGER_NO, at.getPosition(), 1.0);
							at.getExtent().spawnParticles(robotError, at.getPosition().add(new Vector3d(0.0,2.0,0.0)));
						}
					}
				}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 1000, 250, TimeUnit.MILLISECONDS);
		l("HelpMates is now ready!");
		w("HelpMates by DosMike : This plugin is currently in Alpha!");
	}
	
	@Listener
	public void onServerStopping(GameStoppingEvent event) {
		saveConfigs();
		try {
			scheduler.awaitTermination(10, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		}
		synchronized(workers) {
			for (Worker w : workers) w.setTarget(null); //to despawn all helpers
		}
	}
	
	long lastSave=0;
	@Listener
	public void onSaveWorlds(SaveWorldEvent event) {
		if (System.currentTimeMillis()-lastSave < 10000 || !workersDirty) return; //dont save more often than every 10 seconds
		saveConfigs();
	}
	
	public void loadConfigs() {
		synchronized(workers) {
			markDeleted.clear();
			for (Worker w : workers) {
				w.myscript = null;
				w.setTarget(null);
				
//				w.dropInventory();
				
				//actually remove
				if (w.currentMob!=null) {
					w.currentMob.remove();
					w.currentMob = null;
				}
				workers.remove(w);
			}
			
			try {
				@SuppressWarnings("unchecked")
				List<ConfigurationNode> node = (List<ConfigurationNode>) configManager.load().getNode("helpmates").getChildrenList();
				
				node.forEach(serialWorker->{
					try {
						workers.add(WorkerSerializer.deserialize(serialWorker));
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
			lastSave = System.currentTimeMillis();
			workersDirty=false;
		}
	}
	
	public void saveConfigs() {
		synchronized(workers) {
			l("Saving %d HelpMates", workers.size());
			
			List<ConfigurationNode> node = new ArrayList<>(workers.size());
			for (Worker w : workers) {
				try {
					node.add(WorkerSerializer.serialize(w));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			CommentedConfigurationNode root = configManager.createEmptyNode();
			root.setComment("Please be careful when editing this list or helpmates may dissapera");
			
			root.getNode("helpmates").setValue(node);
			
			try {
				configManager.save(root);
			} catch (IOException e) {
				e.printStackTrace();
			}
			lastSave = System.currentTimeMillis();
			workersDirty=false;
		}
	}
}
