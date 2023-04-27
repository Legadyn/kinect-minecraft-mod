package me.legadyn.kinectminecraft.mixin;

import me.legadyn.kinectminecraft.KinectArmorStand;
import me.legadyn.kinectminecraft.socket.SocketReceivedPacket;
import me.legadyn.kinectminecraft.socket.UDPServer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

@Mixin(TitleScreen.class)
public class ExampleMixin {
	@Inject(at = @At("HEAD"), method = "init()V")
	private void init(CallbackInfo info) throws SocketException {
		KinectArmorStand.LOGGER.info("This line is printed by an example mod mixin!");

		/*InetAddress address = null;
		try {
			address = InetAddress.getByName("127.0.0.1");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		int port = 62034;
		String message = "Hello World!";
		UDPServer server = new UDPServer();
		try {
			server.sendPacket(message, address, port);
		} catch (Exception e) {
			e.printStackTrace();
		}

		SocketReceivedPacket.EVENT.register((event) -> {
			KinectArmorStand.LOGGER.info("Evento recibido: " + event);
			return ActionResult.PASS;
		});*/
	}
}
