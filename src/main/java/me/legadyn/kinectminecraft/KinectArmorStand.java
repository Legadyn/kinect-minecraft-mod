package me.legadyn.kinectminecraft;

import me.legadyn.kinectminecraft.command.PlayCommand;
import me.legadyn.kinectminecraft.fabric.MovingArmorStand;
import me.legadyn.kinectminecraft.socket.SocketReceivedPacket;
import me.legadyn.kinectminecraft.socket.UDPServer;
import me.legadyn.kinectminecraft.utils.FileUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KinectArmorStand implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("kinectminecraft");

	private static KinectArmorStand instance;
	private MovingArmorStand armorstand;
	private UDPServer server;
	private Runnable task;
	private boolean startSaving = false;
	public static ServerWorld overworld;
	public static List<Thread> threadList = new ArrayList<>();
	public static List<ScheduledExecutorService> scheduledExecutorServices = new ArrayList<>();

	DatagramSocket udpSocket;

	public KinectArmorStand() throws SocketException {

	}

	/*private LinkedList<ArmorStandMovement> cache = new LinkedList<>();

	private ConvertedArmorStand convertedArmorStand;*/
	public static KinectArmorStand getInstance() {
		return instance;
	}
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	@Override
	public void onInitialize() {
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			LOGGER.info("¡Cliente iniciado!");

			InetAddress address = null;
			try {
				address = InetAddress.getByName("localhost");
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			int port = 62034;
			String message = "Hello World!";
			try {
				server = new UDPServer(); //nueva instancia del servidor, se crea el servidor
				udpSocket = new DatagramSocket(); //se hace un socket para poder enviar mensajes desde el cliente
			} catch (SocketException e) {
				e.printStackTrace();
			}
			try {
				for (int i = 0; i < 5; i++) {
					sendPacket(message + i, address, port);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			SocketReceivedPacket.EVENT.register((event) -> {
				LOGGER.info("Evento recibido: " + event);
				return ActionResult.PASS;
			});

			CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
				//KinectCommand.register(dispatcher);
				PlayCommand.register(dispatcher, dedicated);
		});
	});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LOGGER.info("¡Servidor iniciado!");
			overworld = server.getWorld(World.OVERWORLD);
			FileUtils.createConfigFile(overworld);
			FileUtils fileUtils = new FileUtils(overworld);
			Iterator<String> keys = FileUtils.jsonObject.keys();
			while(keys.hasNext()) {
				String uuid = keys.next();

				JSONObject uuidObject = FileUtils.jsonObject.getJSONObject(uuid);
				short tick = (short) uuidObject.getFloat("tick");
				String animation = uuidObject.getString("animation");
				long delay = 2;
				scheduler.schedule(() -> server.execute(() -> PlayCommand.resumeArmorStand(((ArmorStandEntity) overworld.getEntity(UUID.fromString(uuid))), tick, animation)), delay, TimeUnit.SECONDS);

			}
		});

		ServerWorldEvents.UNLOAD.register((MinecraftServer server, ServerWorld world) -> {

			if (world.getRegistryKey().equals(World.OVERWORLD)) {
				// Detener los threads de los armorstands
				for (Thread thread : threadList) {
					thread.interrupt();
					LOGGER.info("Thread descargado");
				}
				threadList.clear();
			}
		});
	}

	public void sendPacket(String message, InetAddress address, int port) throws Exception {
		byte[] buf = message.getBytes();
		DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
		udpSocket.send(packet);
	}


}
