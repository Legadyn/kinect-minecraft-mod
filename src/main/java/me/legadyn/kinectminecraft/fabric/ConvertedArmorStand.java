package me.legadyn.kinectminecraft.fabric;

import me.legadyn.kinectminecraft.ArmorStandMovement;
import me.legadyn.kinectminecraft.SplitAmorStandMovement;
import me.legadyn.kinectminecraft.utils.FileUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ConvertedArmorStand {

    private ServerPlayerEntity player;
    private ArmorStandEntity armorStand;
    private ArmorStandEntity centerMarker;
    HashMap<String, ArmorStandEntity> armorStands;
    private LinkedList<ArmorStandMovement> movements = new LinkedList<>();
    double playerYaw, armorYaw, centerYaw;
    Vec3d centerPos;

    public ConvertedArmorStand(ArmorStandEntity armorStand, LinkedList<String> list) {
        this.armorStand = armorStand;
        addMovements(list);
    }
    public ConvertedArmorStand(ServerPlayerEntity player, LinkedList<String> list) {
        //check if serverworld works
        World world = player.getEntityWorld();
        BlockPos pos = player.getBlockPos().offset(player.getHorizontalFacing(), 2);
        playerYaw = player.getYaw();
        armorStand = EntityType.ARMOR_STAND.create(world);
        armorStand.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getBodyYaw(), 0.0F);
        armorStand.setNoGravity(true);
        armorYaw = armorStand.getBodyRotation().getYaw();

        world.spawnEntity(armorStand);
        player.getServer().getCommandManager().executeWithPrefix(player.getCommandSource().withSilent(),"/data merge entity "+ armorStand.getUuidAsString() + " {NoBasePlate:1b,ShowArms:1b}");

        addMovements(list);
    }

    public ConvertedArmorStand(ServerPlayerEntity player, LinkedList<String> list, boolean isMulti) {

        this.player = player;
        ServerWorld world = player.getWorld();
        BlockPos pos = player.getBlockPos().offset(player.getHorizontalFacing(), 2);
        playerYaw = player.getYaw();

        centerMarker = EntityType.ARMOR_STAND.create(world);
        centerMarker.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getBodyYaw(), 0.0F);
        centerMarker.setNoGravity(true);
        world.spawnEntity(centerMarker);
        centerYaw = centerMarker.getYaw();

        armorStands = SplitArmorStand.createArmorMap(pos, world, player);
        centerYaw = armorStands.get("head").bodyYaw;
        centerPos = centerMarker.getPos();
        addMovements(list);
    }

    public HashMap<String, ArmorStandEntity> getArmorStands() {
        return armorStands;
    }

    public ConvertedArmorStand(ArmorStandEntity entity, LinkedList<String> list, boolean isMulti) {
        //TO DO resume split armor stand with json
        centerMarker = entity;
        centerPos = centerMarker.getPos();
        armorStands = FileUtils.getMultiState(entity.getUuidAsString(), entity.getWorld());
        addMovements(list);
    }

    public void addMovements(LinkedList<String> list) {
        list.forEach(s -> {
            String[] decoded = s.split(":");
            ArmorStandMovement movement = new ArmorStandMovement();
            movement.convert(decoded);
            movements.add(movement);
        });
    }

    public void nextMovement(short tick) {
        ArmorStandMovement move = movements.get(tick);
        //player.sendMessage(new LiteralText("Yaw: " + armorStand.getYaw() + " Pitch: " + armorStand.getPitch()+ " X:" + armorStand.getX() + " Y:" + armorStand.getY() + " Z:" + armorStand.getZ()), false);

        //player.sendMessage(new LiteralText("Yaw: " + armorStand.getYaw() + " Pitch: " + armorStand.getPitch()+ " X:" + armorStand.getX() + " Y:" + armorStand.getY() + " Z:" + armorStand.getZ()), false);
        armorStand.setHeadRotation(toEulerAngle(new Vec3f(move.headPitch,0,0)));

        armorStand.setRightArmRotation(toEulerAngle(new Vec3f(-move.right_armX,move.right_armY,0)));
        armorStand.setLeftArmRotation(toEulerAngle(new Vec3f(-move.left_armX,move.left_armY,0)));

        armorStand.setRightLegRotation(toEulerAngle(new Vec3f(move.right_legX,move.right_legY,0)));
        armorStand.setLeftLegRotation(toEulerAngle(new Vec3f(move.left_legX,move.left_legY,0)));

        //check if is refresh or update - check if armorstand.getyaw is ok
        armorStand.updatePositionAndAngles(armorStand.getX() - move.vecZ, armorStand.getY() - move.vecY, armorStand.getZ() - move.vecX, (float) (armorYaw - (move.yaw * 0.8)),  move.pitch + 10);

    }

    public void nextSplitMovement(short tick) {
        ArmorStandMovement move = movements.get(tick);
        centerYaw = centerMarker.getBodyRotation().getYaw();
        for(Map.Entry<String, ArmorStandEntity> entry : armorStands.entrySet()) {
            String key = entry.getKey();
            ArmorStandEntity armorStandEntity = entry.getValue();
            NbtCompound nbt = armorStandEntity.getMainHandStack().getOrCreateNbt();
            //Vec3d centerPos = centerMarker.getPos();

            switch (key) {
                case "head":
                    armorStandEntity.setHeadRotation(toEulerAngle(new Vec3f(move.headPitch, 0, 0)));
                    Vec3d headOffset = new Vec3d(0,0,0);//new Vec3d(0, nbt.getFloat("Long"), 0);
                    armorStandEntity.updatePositionAndAngles(centerPos.getX() + headOffset.getX(), centerPos.getY() + headOffset.getY(), centerPos.getZ() + headOffset.getZ(), (float) (centerYaw - ((move.yaw * 0.8))), move.pitch + 10);
                    //armorStandEntity.updatePositionAndAngles(centerPos.getX() + headOffset.getX(), centerPos.getY() + headOffset.getY(), centerPos.getZ() + headOffset.getZ(), (float) (centerYaw - ((move.yaw * 0.8))), move.pitch + 10);
                    break;

                case "leftForeArm":
                    armorStandEntity.setLeftArmRotation(toEulerAngle(new Vec3f(-move.left_lower_armX, move.left_lower_armY, 0)));
                    Vec3d leftForeArmOffset = new Vec3d(0,0,0);//new Vec3d(nbt.getFloat("Long"), 0, 0);
                    armorStandEntity.updatePositionAndAngles(centerPos.getX() + leftForeArmOffset.getX(), centerPos.getY() + leftForeArmOffset.getY(), centerPos.getZ() + leftForeArmOffset.getZ(), (float) (centerYaw - ((move.yaw * 0.8))), move.pitch + 10);
                    //armorStandEntity.refreshPositionAndAngles(new BlockPos(centerMarker.getPos().add(leftForeArmOffset)), (float) (centerYaw - ((move.yaw * 0.8)/120)), move.pitch + 10);
                    break;

                case "rightForeArm":
                    armorStandEntity.setRightArmRotation(toEulerAngle(new Vec3f(-move.right_lower_armX, move.right_lower_armY, 0)));
                    Vec3d rightForeArmOffset = new Vec3d(0,0,0);//new Vec3d(-nbt.getFloat("Long"), 0, 0);
                    armorStandEntity.updatePositionAndAngles(centerPos.getX() + rightForeArmOffset.getX(), centerPos.getY() + rightForeArmOffset.getY(), centerPos.getZ() + rightForeArmOffset.getZ(), (float) (centerYaw - ((move.yaw * 0.8))), move.pitch + 10);
                    //armorStandEntity.refreshPositionAndAngles(new BlockPos(centerMarker.getPos().add(rightForeArmOffset)), (float) (centerYaw - ((move.yaw * 0.8)/120)), move.pitch + 10);
                    break;

                case "leftShoulder":
                    armorStandEntity.setLeftArmRotation(toEulerAngle(new Vec3f(-move.left_upper_armX, move.left_upper_armY, 0)));
                    Vec3d leftShoulderOffset = new Vec3d(0,0,0);//new Vec3d(nbt.getFloat("Long")-1, 0, 0);
                    armorStandEntity.updatePositionAndAngles(centerPos.getX() + leftShoulderOffset.getX(), centerPos.getY() + leftShoulderOffset.getY(), centerPos.getZ() + leftShoulderOffset.getZ(), (float) (centerYaw - ((move.yaw * 0.8))), move.pitch + 10);
                    //armorStandEntity.refreshPositionAndAngles(new BlockPos(centerMarker.getPos().add(leftShoulderOffset)), (float) (centerYaw - ((move.yaw * 0.8)/120)), move.pitch + 10);
                    break;

                case "rightShoulder":
                    armorStandEntity.setRightArmRotation(toEulerAngle(new Vec3f(-move.right_upper_armX, move.right_upper_armY, 0)));
                    Vec3d rightShoulderOffset = new Vec3d(0,0,0); //new Vec3d(-nbt.getFloat("Long")+1, 0, 0);
                    armorStandEntity.updatePositionAndAngles(centerPos.getX() + rightShoulderOffset.getX(), centerPos.getY() + rightShoulderOffset.getY(), centerPos.getZ() + rightShoulderOffset.getZ(), (float) (centerYaw - ((move.yaw * 0.8))), move.pitch + 10);
                    //armorStandEntity.refreshPositionAndAngles(new BlockPos(centerMarker.getPos().add(rightShoulderOffset)), (float) (centerYaw - ((move.yaw * 0.8)/120)), move.pitch + 10);
                    break;

                default:
                    break;
            }

        }
        //check if is refresh or update - check if armorstand.getyaw is ok
        centerMarker.updatePositionAndAngles(centerMarker.getX() - move.vecZ, centerMarker.getY() - move.vecY, centerMarker.getZ() - move.vecX, (float) (centerYaw - (move.yaw * 0.8)), move.pitch + 10);
    }

    private EulerAngle toEulerAngle(Vec3f rotation) {
        float pitch = rotation.getX();
        float yaw = rotation.getY();
        float roll = rotation.getZ();
        return new EulerAngle(pitch, yaw, roll);
    }

    public ArmorStandEntity getMarker() {
        return centerMarker;
    }

    public ArmorStandEntity getArmorStand() {
        return armorStand;
    }
}
