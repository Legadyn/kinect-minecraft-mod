package me.legadyn.kinectminecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.legadyn.kinectminecraft.KinectArmorStand;
import me.legadyn.kinectminecraft.fabric.MovingArmorStand;
import me.legadyn.kinectminecraft.fabric.SplitArmorStand;
import me.legadyn.kinectminecraft.utils.FileUtils;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedList;

public class SaveCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
            LiteralArgumentBuilder<ServerCommandSource> kinectCommand = CommandManager.literal("kinect").executes(
                    context -> {
                        KinectArmorStand.realtimeArmorStand = new MovingArmorStand(context.getSource().getPlayer().getPos());
                        KinectArmorStand.realtimeArmorStand.spawn(context.getSource().getPlayer());

                        return 1;
                    }
            )
                    .then(CommandManager.literal("save")
                            .then(CommandManager.literal("start")
                                    // "/kinect save start"
                                    .executes(context -> {
                                        startSaving(context);
                                        return 1;
                                    }))
                            .then(CommandManager.literal("stop")
                                    .then(CommandManager.argument("arg", StringArgumentType.string())
                                            //"/kinect save stop <arg>"
                                            .executes(context -> {
                                                String arg = StringArgumentType.getString(context, "arg");
                                                stopSaving(context, arg);
                                                return 1;
                                            }))))
                    .then(CommandManager.literal("multi")
                            .executes(context -> {
                                KinectArmorStand.splitArmorStand = new SplitArmorStand(context.getSource().getPlayer().getPos());
                                KinectArmorStand.splitArmorStand.spawn(context.getSource().getPlayer());
                                return 1;
                            }));
            dispatcher.register(kinectCommand);
    }

    public static void startSaving(CommandContext<ServerCommandSource> context) {
        KinectArmorStand.getInstance().startSaving();
    }

    public static void stopSaving(CommandContext<ServerCommandSource> context, String arg) {
        KinectArmorStand.startSaving = false;
        LinkedList<String> list = new LinkedList<>();
        KinectArmorStand.cache.forEach(c -> {
            String line = c.headPitch + ":" +
                    c.right_armX + ":" + c.right_armY  + ":" +
                    c.left_armX + ":" + c.left_armY + ":" +
                    c.right_legX + ":" + c.right_legY + ":" +
                    c.left_legX + ":" + c.left_legY + ":" +
                    c.right_upper_armX + ":" + c.right_upper_armY  + ":" +
                    c.left_upper_armX + ":" + c.left_upper_armY  + ":" +
                    c.vecX + ":" + c.vecY + ":" + c.vecZ + ":" + c.yaw + ":" + c.pitch;
            list.add(line);
        });
        try {
            FileUtils.writeAnimation(arg, list);
        } catch (IOException e) {
            KinectArmorStand.LOGGER.info("Error while saving animation: " + e.getMessage());
        }
    }
}
