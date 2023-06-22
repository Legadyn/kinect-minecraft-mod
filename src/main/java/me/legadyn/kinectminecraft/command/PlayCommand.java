package me.legadyn.kinectminecraft.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.legadyn.kinectminecraft.ArmorStandMovement;
import me.legadyn.kinectminecraft.KinectArmorStand;
import me.legadyn.kinectminecraft.fabric.ConvertedArmorStand;
import me.legadyn.kinectminecraft.utils.FileUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.render.entity.model.ArmorStandEntityModel;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

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

    private static long wait = 0;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("kinect")
                .then(CommandManager.literal("multiplay").then(CommandManager.argument("action", StringArgumentType.string())
                        .executes(
                                context -> runMulti(context, StringArgumentType.getString(context, "action"))
                        ))
                )
                .then(CommandManager.literal("test").then(CommandManager.argument("action", StringArgumentType.string())
                        .executes(
                                context -> testCommandContext(context, StringArgumentType.getString(context, "action"))
                        ))
                )
                .then(CommandManager.literal("play").then(CommandManager.argument("action", StringArgumentType.string())
                        .executes(
                                context -> run(context, StringArgumentType.getString(context, "action"))
                        ))));


    }

    public static int testCommandContext(CommandContext<ServerCommandSource> context, String action) throws CommandSyntaxException {

        ServerPlayerEntity player = context.getSource().getPlayer();

        ServerWorld world = player.getWorld();
        BlockPos posi = player.getBlockPos().offset(player.getHorizontalFacing(), 2);

        ArmorStandEntity armorStandEntity = EntityType.ARMOR_STAND.create(world);

        ArmorStandEntity armorOtherEntity = EntityType.ARMOR_STAND.create(world);

        world.spawnEntity(armorStandEntity);
        armorStandEntity.setNoGravity(true);
        armorStandEntity.updatePositionAndAngles(posi.getX() + 0.5D, posi.getY(), posi.getZ() + 0.5D, 0.0F, 0.0F);

        world.spawnEntity(armorOtherEntity);
        armorOtherEntity.setNoGravity(true);
        armorOtherEntity.updatePositionAndAngles(posi.getX() + 0.5D, posi.getY(), posi.getZ() + 0.5D, 0.0F, 0.0F);

        player.getServer().getCommandManager().executeWithPrefix(player.getCommandSource().withSilent(), "/data merge entity " + armorStandEntity.getUuidAsString() + " {NoBasePlate:1b,ShowArms:1b}");

        //armorStandEntity.setStackInHand(Hand.MAIN_HAND, player.getMainHandStack());
        armorStandEntity.setRightArmRotation(new EulerAngle(90,0,0));
        Vec3d handPos = getHandPos(armorStandEntity);
        Vec3d shoulderPos = getShoulderPos(armorOtherEntity);
        double x = handPos.getX() - shoulderPos.getX();
        double y = handPos.getY() - shoulderPos.getY();
        double z = handPos.getZ() - shoulderPos.getZ();

        armorOtherEntity.updatePositionAndAngles(shoulderPos.getX() + x, shoulderPos.getY() + y, shoulderPos.getZ() + z,0,0);

        //armorOtherEntity.updatePosition(armorStandEntity.getX() + diffX, armorStandEntity.getY() + diffY, armorStandEntity.getZ() + diffZ);
        player.sendMessage(Text.literal("X: "+ armorStandEntity.getX()+ " Y: "+ armorStandEntity.getY() + " Z: " + armorStandEntity.getZ()), false);
        player.sendMessage(Text.literal("X: "+ armorOtherEntity.getX()+ " Y: "+ armorOtherEntity.getY() + " Z: " + armorOtherEntity.getZ()), false);
        player.sendMessage(Text.literal("X:" + handPos.x + " Y:" + handPos.y + " Z:" + handPos.z), false);
        return 1;

    }
    //works with static coords, not rotation
    public static Vec3d getHandPos(ArmorStandEntity armor) {
        Vec3d pos = armor.getPos();
        float yaw = (float) Math.toRadians(armor.getRightArmRotation().getYaw());
        float pitch =(float) Math.toRadians(armor.getRightArmRotation().getPitch());
        double x = pos.getX() + 0.4 * Math.sin(pitch) * Math.cos(Math.toRadians(yaw));
        double y = pos.getY() + 1.4 * Math.sin(yaw);
        double z = pos.getZ() - 0.4 * Math.sin(yaw) * Math.cos(pitch);
        return new Vec3d(x, y, z);
    }
    /*public static Vec3d getHandPos(ArmorStandEntity armor) {
        Vec3d pos = armor.getPos();
        float yaw = armor.getYaw();
        float pitch = armor.getPitch();
        double x = pos.getX() + 0.4 * Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        double y = pos.getY() + 1.4;
        double z = pos.getZ() - 0.4 * Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        return new Vec3d(x, y, z);
    }*/

    /*public static Vec3d getHandPos(ArmorStandEntity armor) {
        Vec3d pos = armor.getPos();
        EulerAngle rightArmRotation = armor.getRightArmRotation();
        double x = pos.getX() - 0.41;
        double y = pos.getY() + 0.8;
        double z = pos.getZ() + 0.05;
        // Aplicar la rotación del brazo derecho
        x += 0.4 * Math.sin(rightArmRotation.getYaw());
        y += 0.4 * Math.sin(rightArmRotation.getPitch());
        z -= 0.4 * Math.cos(rightArmRotation.getRoll());
        return new Vec3d(x, y, z);
    }*/

    /*public static Vec3d getHandPos(ArmorStandEntity armor) {
        Vec3d pos = armor.getPos();
        EulerAngle armRotation = armor.getRightArmRotation();
        double armRotX = Math.toRadians(armRotation.getPitch());
        double armRotZ = Math.toRadians(armRotation.getYaw());
        float yaw = armor.getYaw();
        float pitch = armor.getPitch();
        double x = pos.getX() + 0.5 * Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        double y = pos.getY() + 1.4;
        double z = pos.getZ() - 0.8 * Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        x += Math.sin(armRotZ) * Math.cos(armRotX);
        z -= Math.cos(armRotZ) * Math.cos(armRotX);
        return new Vec3d(x, y, z);
    }*/



    public static Vec3d getShoulderPos(ArmorStandEntity armor) {
        Vec3d pos = armor.getPos();
        float yaw = armor.getYaw();
        float pitch = armor.getPitch();
        double x = pos.getX() + 0.25 * Math.sin(Math.toRadians(yaw + 90)) * Math.cos(Math.toRadians(pitch));
        double y = pos.getY() + 1.85;
        double z = pos.getZ() - 0.25 * Math.cos(Math.toRadians(yaw + 90)) * Math.cos(Math.toRadians(pitch));
        return new Vec3d(x, y, z);
    }


    public static int run(CommandContext<ServerCommandSource> context, String action) throws CommandSyntaxException {
        ServerCommandSource src = context.getSource();

        //read movements from file
        LinkedList<String> list = FileUtils.readAnimation(action, context.getSource().getPlayer());

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
                        KinectArmorStand.LOGGER.info("coso terminado");
                        return;
                    }
                    KinectArmorStand.LOGGER.info("Tick " + tick);
                    convertedArmorStand.nextMovement(tick);
                    KinectArmorStand.LOGGER.info("Ticky " + tick);
                    FileUtils.writeState(convertedArmorStand.getArmorStand().getUuidAsString(), tick);
                    KinectArmorStand.LOGGER.info("Tickyy " + tick);

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


    /*SplitConvertedArmorStand

        se pasa lista y jugador por parametro
    *

    */
    public static int runMulti(CommandContext<ServerCommandSource> context, String action) throws CommandSyntaxException {
        ServerCommandSource src = context.getSource();

        //read movements from file
        LinkedList<String> list = FileUtils.readAnimation(action, context.getSource().getPlayer());

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(new Runnable() {
            short tick = 0;
            ConvertedArmorStand splitArmorStand = new ConvertedArmorStand(src.getPlayer(), list, true);
            @Override
            public void run() {
                if (splitArmorStand != null) {
                    long startTime = System.currentTimeMillis();

                    tick++;
                    if (tick > list.size() - 1) {
                        splitArmorStand = null;
                        return;
                    }
                    KinectArmorStand.LOGGER.info("Tick " + tick);
                    splitArmorStand.nextSplitMovement(tick);
                    try {
                        FileUtils.writeMultiState(splitArmorStand.getMarker().getUuidAsString(), splitArmorStand.getArmorStands(),tick);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

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

    public static void resumeArmorStand(Entity entity, short ticked, String animation, boolean isMulti) {

        //Read animation file
        LinkedList<String> list = FileUtils.readAnimation(animation, null);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        //New runnable for every armorstand resumed
        executor.scheduleAtFixedRate(new Runnable() {
            //pass tick from json file to resume from
            short tick = ticked;

            ConvertedArmorStand convertedArmorStand = isMulti ? new ConvertedArmorStand((ArmorStandEntity) entity, list, true) : new ConvertedArmorStand((ArmorStandEntity) entity, list) ;

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
