package me.legadyn.kinectminecraft;

import me.legadyn.kinectminecraft.command.PlayCommand;
import me.legadyn.kinectminecraft.command.SaveCommand;
import me.legadyn.kinectminecraft.fabric.MovingArmorStand;
import me.legadyn.kinectminecraft.fabric.SplitArmorStand;
import me.legadyn.kinectminecraft.socket.SocketReceivedPacket;
import me.legadyn.kinectminecraft.socket.UDPServer;
import me.legadyn.kinectminecraft.utils.FileUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KinectArmorStand implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("kinectminecraft");

	private static KinectArmorStand instance;
	public static MovingArmorStand realtimeArmorStand;
	public static SplitArmorStand splitArmorStand;
	private UDPServer kinectServer;
	public static boolean startSaving = false;
	public static ServerWorld overworld;
	public static LinkedList<ArmorStandMovement> cache = new LinkedList<>();

	public static List<ScheduledExecutorService> scheduledExecutorServices = new ArrayList<>();

	public static ServerPlayerEntity playertest;
	DatagramSocket udpSocket;
	public KinectArmorStand() throws SocketException {
	}

	private final ScheduledExecutorService resumeScheduler = Executors.newScheduledThreadPool(1);

	@Override
	public void onInitialize() {
		instance = this;

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

			PlayCommand.register(dispatcher, registryAccess, environment);
			SaveCommand.register(dispatcher, registryAccess, environment);
		});


		SocketReceivedPacket.EVENT.register((event) -> {
			if(splitArmorStand != null) {
				splitArmorStand.update(event);
				return false;
			}
			if(realtimeArmorStand != null) {
				realtimeArmorStand.update(event);
			}
			return false;
		});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {

			try {
				kinectServer = new UDPServer(); //nueva instancia del servidor, se crea el servidor
				//udpSocket = new DatagramSocket(); //se hace un socket para poder enviar mensajes desde el cliente
			} catch (SocketException e) {
				e.printStackTrace();
			}
			LOGGER.info("Servidor iniciado!");

			//get overworld in fabric
			overworld = server.getWorld(World.OVERWORLD);

			FileUtils.createConfigFile(overworld);
			FileUtils fileUtils = new FileUtils(overworld);

			//resume armorstand stuff
			Iterator<String> keys = FileUtils.jsonObject.keys();
			while(keys.hasNext()) { //read every armorstand obj
				String uuid = keys.next();
				if(overworld.getEntity(UUID.fromString(uuid)) != null) return;

				JSONObject uuidObject = FileUtils.jsonObject.getJSONObject(uuid);
				short tick = (short) uuidObject.getFloat("tick");
				String animation = uuidObject.getString("animation");

				//Schedule task to 3 sec to give time to the server to load the entities
				try {

					resumeScheduler.schedule(() -> {

						server.execute(() -> {
							if(overworld.getEntity(UUID.fromString(uuid)).getScoreboardTags().contains("center")) {
								PlayCommand.resumeArmorStand(overworld.getEntity(UUID.fromString(uuid)), tick, animation, true);
							} else {
								PlayCommand.resumeArmorStand(overworld.getEntity(UUID.fromString(uuid)), tick, animation, false);
							}
						});

						}, 2, TimeUnit.SECONDS);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			// Stoping all tasks on executorservice
			for(ScheduledExecutorService executorService : scheduledExecutorServices) {
				executorService.shutdown();
			}
			scheduledExecutorServices.clear();
			kinectServer.stop();
		});
	}

	public void sendPacket(String message, InetAddress address, int port) throws Exception {
		byte[] buf = message.getBytes();
		DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
		udpSocket.send(packet);
	}

	public LinkedList<ArmorStandMovement> getCache() {
		return cache;
	}

	public static KinectArmorStand getInstance() {
		return instance;
	}

	public static boolean isStartSaving() {
		return startSaving;
	}

	public void startSaving() {
		startSaving = true;
	}

	public MovingArmorStand getArmorStand() {
		return realtimeArmorStand;
	}


}
