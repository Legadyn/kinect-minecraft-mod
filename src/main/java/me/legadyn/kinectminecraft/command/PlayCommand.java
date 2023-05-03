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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Time;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class PlayCommand {

    private static ConvertedArmorStand convertedArmorStand;

    private static long wait = 0;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(CommandManager.literal("kinect")
                .then(CommandManager.literal("play").then(CommandManager.argument("action", StringArgumentType.string())
                        .executes(
                                context -> run(context, StringArgumentType.getString(context, "action"))
                        ))));


    }

    public static int run(CommandContext<ServerCommandSource> context, String action) throws CommandSyntaxException {
        ServerCommandSource src = context.getSource();

        //read movements from file
        LinkedList<String> list = FileUtils.readAnimation(action);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(new Runnable() {
            short tick = 0;
            ConvertedArmorStand convertedArmorStand = new ConvertedArmorStand(src.getPlayer(), list);
            @Override
            public void run() {
                if (convertedArmorStand != null) {
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
                       wait = (long) (delayMillis - duration);
                        try {
                            TimeUnit.MILLISECONDS.sleep(wait);
                        } catch (InterruptedException e) {
                            executor.shutdownNow();
                        }
                    }
                }
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
        KinectArmorStand.scheduledExecutorServices.add(executor);
        return 1;
    }

    public static void resumeArmorStand(ArmorStandEntity armorStand, short ticked, String animation) {

        //Read animation file
        LinkedList<String> list = FileUtils.readAnimation(animation);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        //New runnable for every armorstand resumed
        executor.scheduleAtFixedRate(new Runnable() {
            //pass tick from json file to resume from
            short tick = ticked;
            ConvertedArmorStand convertedArmorStand = new ConvertedArmorStand(armorStand, list);

            @Override
            public void run() {
                if (convertedArmorStand != null) {
                    long startTime = System.currentTimeMillis();

                    tick++;
                    if (tick > list.size() - 1) {
                        convertedArmorStand = null;
                        return;
                    }
                    convertedArmorStand.nextMovement(tick);
                    FileUtils.writeState(convertedArmorStand.getArmorStand().getUuidAsString(), tick);

                    long duration = System.currentTimeMillis() - startTime;
                    float delayMillis = 30;
                    if (duration < delayMillis) {
                        wait = (long) (delayMillis - duration);
                        try {
                            TimeUnit.MILLISECONDS.sleep(wait);
                        } catch (InterruptedException e) {
                            executor.shutdownNow();
                        }
                    }
                }
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
        KinectArmorStand.scheduledExecutorServices.add(executor);
    }
}
