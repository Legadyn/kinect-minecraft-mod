package me.legadyn.kinectminecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.legadyn.kinectminecraft.KinectArmorStand;
import me.legadyn.kinectminecraft.fabric.Converted;
import me.legadyn.kinectminecraft.fabric.MultiArmorStand;
import me.legadyn.kinectminecraft.fabric.SimpleArmorStand;
import me.legadyn.kinectminecraft.utils.FileUtils;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayCommand {

    private static long wait = 0;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("kinect")
                .then(CommandManager.literal("multiplay").then(CommandManager.argument("animation", StringArgumentType.string())
                        .executes(
                                context -> runMulti(context, StringArgumentType.getString(context, "animation"),context.getSource().getPlayer().getX(),context.getSource().getPlayer().getY(),context.getSource().getPlayer().getZ())
                        ).then(coordinateArguments(PlayCommand::runMulti)))
                )
                .then(CommandManager.literal("test").then(CommandManager.argument("action", StringArgumentType.string())
                        .executes(
                                context -> testCommandContext(context, StringArgumentType.getString(context, "action"))
                        ))
                )
                .then(CommandManager.literal("play").then(CommandManager.argument("animation", StringArgumentType.string())
                        .executes(
                                context -> run(context, StringArgumentType.getString(context, "animation"), context.getSource().getPlayer().getX(),context.getSource().getPlayer().getY(),context.getSource().getPlayer().getZ())
                        ).then(coordinateArguments(PlayCommand::run)))));


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


    public static int run(CommandContext<ServerCommandSource> context, String action, double x, double y, double z) throws CommandSyntaxException {
        ServerCommandSource src = context.getSource();

        //read movements from file
        LinkedList<String> list = FileUtils.readAnimation(action, context.getSource().getPlayer());

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new Runnable() {
            short tick = 0;
            SimpleArmorStand convertedArmorStand = new SimpleArmorStand(src.getPlayer(), list, x, y, z);
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
                    FileUtils.writeState(convertedArmorStand.getArmorStand().getUuidAsString(), tick, action);

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
        }, 0, 50, TimeUnit.MILLISECONDS);
        KinectArmorStand.scheduledExecutorServices.add(executor);
        return 1;
    }


    /*SplitConvertedArmorStand

        se pasa lista y jugador por parametro
    *

    */
    public static int runMulti(CommandContext<ServerCommandSource> context, String action, double x, double y, double z) {
        ServerCommandSource src = context.getSource();

        //read movements from file
        LinkedList<String> list = FileUtils.readAnimation(action, context.getSource().getPlayer());

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(new Runnable() {
            short tick = 0;
            MultiArmorStand splitArmorStand = new MultiArmorStand(src.getPlayer(), list, x, y, z);
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
                    splitArmorStand.nextMovement(tick);
                    try {
                        FileUtils.writeMultiState(splitArmorStand.getArmorStand().getUuidAsString(), splitArmorStand.getArmorStands(),tick, action);

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
        }, 0, 50, TimeUnit.MILLISECONDS);
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

            Converted convertedArmorStand = isMulti ? new MultiArmorStand((ArmorStandEntity) entity, list) : new SimpleArmorStand((ArmorStandEntity) entity, list) ;

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
                    if(convertedArmorStand instanceof SimpleArmorStand) {
                        FileUtils.writeState(convertedArmorStand.getArmorStand().getUuidAsString(), tick, animation);
                    } else {
                        FileUtils.writeMultiState(convertedArmorStand.getArmorStand().getUuidAsString(), convertedArmorStand.getArmorStands(), tick, animation);
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
        }, 0, 50, TimeUnit.MILLISECONDS);
        KinectArmorStand.scheduledExecutorServices.add(executor);
    }

    //Functional interface for use the same method for both commands, runMulti and runSimple
    @FunctionalInterface
    interface ContextExecutor {
        int execute(CommandContext<ServerCommandSource> context, String action, double x, double y, double z) throws CommandSyntaxException;
    }

    private static ArgumentBuilder<ServerCommandSource, ?> coordinateArguments(ContextExecutor executor) {
        return CommandManager.argument("x", DoubleArgumentType.doubleArg())
                .then(CommandManager.argument("y", DoubleArgumentType.doubleArg())
                        .then(CommandManager.argument("z", DoubleArgumentType.doubleArg())
                                .executes(context -> executor.execute(
                                        context,
                                        StringArgumentType.getString(context, "animation"),
                                        DoubleArgumentType.getDouble(context, "x"),
                                        DoubleArgumentType.getDouble(context, "y"),
                                        DoubleArgumentType.getDouble(context, "z")
                                ))
                        )
                );
    }
}
