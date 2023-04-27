package me.legadyn.kinectminecraft.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.legadyn.kinectminecraft.KinectArmorStand;
import me.legadyn.kinectminecraft.fabric.ConvertedArmorStand;
import me.legadyn.kinectminecraft.utils.FileUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayCommand {

    private static ConvertedArmorStand convertedArmorStand;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(CommandManager.literal("kinect")
                .then(CommandManager.literal("play").then(CommandManager.argument("action", StringArgumentType.string())
                        .executes(
                                context -> run(context, StringArgumentType.getString(context, "action"))
                        ))));


    }

    public static int run(CommandContext<ServerCommandSource> context, String action) {
        ServerCommandSource src = context.getSource();

        InputStream inputStream = KinectArmorStand.class.getClassLoader().getResourceAsStream(action + ".txt");
            if (inputStream == null) {
                src.sendError(new LiteralText("File " + action + ".txt not found"));
                return 0;
            }

        Scanner s = new Scanner(inputStream);

        LinkedList<String> list = new LinkedList<>();
        while (s.hasNext()) {
            list.add(s.next());
        }
        s.close();

        try {
            convertedArmorStand = new ConvertedArmorStand(src.getPlayer(), list);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        /*executor.scheduleAtFixedRate(() -> {
            if (convertedArmorStand != null) {
                convertedArmorStand.nextMovement();
            }
        }, 0, 30, TimeUnit.MILLISECONDS);*/

        // Ejecuta la acción que deseas realizar cada 1 segundo
        Thread thread = new Thread(() -> {
            short tick = 0;
                while (convertedArmorStand != null && !Thread.currentThread().isInterrupted()) {
                    long startTime = System.currentTimeMillis();

                    tick++;
                    if (tick > list.size() - 1) {
                        convertedArmorStand = null;
                        return;
                    }
                    KinectArmorStand.LOGGER.info("Tick " + tick);
                    convertedArmorStand.nextMovement(tick);
                    FileUtils.writeState(convertedArmorStand.getArmorStand().getUuidAsString(), tick);

                    long duration = System.currentTimeMillis() - startTime;
                    float delayMillis = 30;
                    if (duration < delayMillis) {
                        try {
                            Thread.sleep((long) (delayMillis - duration));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } //Thread.currentThread().interrupt();
            });

        thread.start();
        KinectArmorStand.threadList.add(thread);

        return 1;
    }

    public static void resumeArmorStand(ArmorStandEntity armorStand, short ticked, String animation) {

        //KinectArmorStand.LOGGER.info("Resuming armor stand " + armorStand.getUuidAsString() + " at tick " + ticked + " with animation " + animation);
        InputStream inputStream = KinectArmorStand.class.getClassLoader().getResourceAsStream("armorstand" + ".txt");
        if (inputStream == null) {
            KinectArmorStand.LOGGER.error("File " + "armorstand" + ".txt not found");
            return;
        }
        Scanner s = new Scanner(inputStream);

        LinkedList<String> list = new LinkedList<>();
        while (s.hasNext()) {
            list.add(s.next());
        }
        s.close();

        convertedArmorStand = new ConvertedArmorStand(armorStand, list);
        Thread thread = new Thread() {
            short tick = ticked;
            @Override
            public void run() {
                while (convertedArmorStand != null && !Thread.currentThread().isInterrupted()) {
                    long startTime = System.currentTimeMillis();

                    tick++;
                    if (tick > list.size() - 1) {
                        convertedArmorStand = null;
                        KinectArmorStand.LOGGER.info("Terminado en tick" + tick);
                        return;
                    }
                    KinectArmorStand.LOGGER.info("Tick " + tick);
                    convertedArmorStand.nextMovement(tick);
                    FileUtils.writeState(armorStand.getUuidAsString(), tick);

                    long duration = System.currentTimeMillis() - startTime;
                    float delayMillis = 30;
                    if (duration < delayMillis) {
                        try {
                            Thread.sleep((long) (delayMillis - duration));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } //Thread.currentThread().interrupt();
            }
        };
        thread.start();
        KinectArmorStand.threadList.add(thread);
    }
}
