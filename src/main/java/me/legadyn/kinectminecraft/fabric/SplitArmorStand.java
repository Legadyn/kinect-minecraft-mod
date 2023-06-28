package me.legadyn.kinectminecraft.fabric;

import me.legadyn.kinectminecraft.ArmorStandMovement;
import me.legadyn.kinectminecraft.KinectArmorStand;
import me.legadyn.kinectminecraft.event.ChangeRotationEvent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

import java.util.HashMap;
import java.util.Map;

public class SplitArmorStand {
    Vec3d position;
    private ArmorStandEntity centerMarker;
    private HashMap<String, ArmorStandEntity> armorStands = new HashMap<>();

    double playerYaw;

    float centerYaw;

    public SplitArmorStand(Vec3d pos) {
        position = pos;
        position.add(position.multiply(3));

    }

    public void spawn(ServerPlayerEntity player) {
        ServerWorld world = player.getWorld();
        BlockPos pos = player.getBlockPos().offset(player.getHorizontalFacing(), 2);
        playerYaw = player.getYaw();

        centerMarker = EntityType.ARMOR_STAND.create(world);
        centerMarker.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getBodyYaw(), 0.0F);
        centerMarker.setNoGravity(true);
        centerMarker.setInvisible(true);
        centerMarker.setInvulnerable(true);
        centerMarker.addScoreboardTag("center");

        armorStands = createArmorMap(pos,world,player);

        ChangeRotationEvent.registerArmorStand(centerMarker, armorStands);

        world.spawnEntity(centerMarker);

    }

    public void update(String packet) {

        //marker doesn't exists, kill the armor stands
        if (centerMarker == null) {
            for(Map.Entry<String, ArmorStandEntity> entry : armorStands.entrySet()) {
                String key = entry.getKey();
                ArmorStandEntity armorStandEntity = entry.getValue();
                armorStandEntity.kill();
            }
            return;
        }

        String[] decoded = packet.split(":");
        ArmorStandMovement move;
        // try catch for unknown error (packet has 33-19 on pitch and it should be 33:19)
        try {
            move = new ArmorStandMovement(decoded);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        //adds every movement to the main cache in KinectArmorStand
        if (KinectArmorStand.isStartSaving()) {
            KinectArmorStand.getInstance().getCache().add(move);
        }

        centerYaw = centerMarker.getYaw();

        for(Map.Entry<String, ArmorStandEntity> entry : armorStands.entrySet()) {
            String key = entry.getKey();
            ArmorStandEntity armorStandEntity = entry.getValue();
            NbtCompound nbt = armorStandEntity.getMainHandStack().getOrCreateNbt();
            //Vec3d centerPos = centerMarker.getPos();
            //make armorstand rotates with kinect body (float) (centerYaw - ((move.yaw * 0.8))), move.pitch + 10

            double distancia = Math.abs(armorStandEntity.getX() - centerMarker.getX());
            double angulo = Math.toRadians(centerMarker.getYaw());
            double offsetX = Math.cos(angulo) * distancia;
            double offsetZ = Math.sin(angulo) * distancia;
            Vec3d posSecundario;
            switch (key) {
                case "head":
                    armorStandEntity.setHeadRotation(toEulerAngle(new Vec3f(move.headPitch, 0, 0)));
                    Vec3d headOffset = new Vec3d(0, nbt.getFloat("Long"), 0);
                    armorStandEntity.updatePositionAndAngles(armorStandEntity.getX(), armorStandEntity.getY(), armorStandEntity.getZ(), centerMarker.getYaw(), centerMarker.getPitch());
                    //armorStandEntity.updatePositionAndAngles(centerPos.getX() + headOffset.getX(), centerPos.getY() + headOffset.getY(), centerPos.getZ() + headOffset.getZ(), (float) (centerYaw - ((move.yaw * 0.8))), move.pitch + 10);
                    break;

                case "leftForeArm":
                    armorStandEntity.setLeftArmRotation(toEulerAngle(new Vec3f(-move.left_lower_armX, move.left_lower_armY, 0)));
                    Vec3d leftForeArmOffset = new Vec3d(nbt.getFloat("Long"), 0, 0);
                    armorStandEntity.updatePositionAndAngles(armorStandEntity.getX(), armorStandEntity.getY(), armorStandEntity.getZ(), centerMarker.getYaw(), centerMarker.getPitch());
                    //armorStandEntity.refreshPositionAndAngles(new BlockPos(centerMarker.getPos().add(leftForeArmOffset)), (float) (centerYaw - ((move.yaw * 0.8)/120)), move.pitch + 10);
                    break;

                case "rightForeArm":
                    armorStandEntity.setRightArmRotation(toEulerAngle(new Vec3f(-move.right_lower_armX, move.right_lower_armY, 0)));
                    Vec3d rightForeArmOffset = new Vec3d(-nbt.getFloat("Long"), 0, 0);
                    armorStandEntity.updatePositionAndAngles(armorStandEntity.getX(), armorStandEntity.getY(), armorStandEntity.getZ(), centerMarker.getYaw(), centerMarker.getPitch());
                    //armorStandEntity.refreshPositionAndAngles(new BlockPos(centerMarker.getPos().add(rightForeArmOffset)), (float) (centerYaw - ((move.yaw * 0.8)/120)), move.pitch + 10);
                    break;

                case "leftShoulder":
                    armorStandEntity.setLeftArmRotation(toEulerAngle(new Vec3f(-move.left_upper_armX, move.left_upper_armY, 0)));
                    Vec3d leftShoulderOffset = new Vec3d(nbt.getFloat("Long")-1, 0, 0);
                    posSecundario = centerMarker.getPos().add(offsetX, 0, offsetZ);
                    armorStandEntity.updatePositionAndAngles(posSecundario.getX(), armorStandEntity.getY(), posSecundario.getZ(), centerMarker.getYaw(), centerMarker.getPitch());
                    break;

                case "rightShoulder":
                    armorStandEntity.setRightArmRotation(toEulerAngle(new Vec3f(-move.right_upper_armX, move.right_upper_armY, 0)));
                    posSecundario = centerMarker.getPos().subtract(offsetX, 0, offsetZ);
                    armorStandEntity.updatePositionAndAngles(posSecundario.getX(), armorStandEntity.getY(), posSecundario.getZ(), centerMarker.getYaw(), centerMarker.getPitch());
                    //armorStandEntity.refreshPositionAndAngles(new BlockPos(centerMarker.getPos().add(rightShoulderOffset)), (float) (centerYaw - ((move.yaw * 0.8)/120)), move.pitch + 10);
                    break;

                default:
                    break;
            }
        }
        //check if is refresh or update - check if armorstand.getyaw is ok
        //centerMarker.refreshPositionAndAngles(centerMarker.getBlockPos().subtract(new Vec3i(move.vecZ, move.vecY, move.vecX)),0,0);
        centerMarker.updatePosition(centerMarker.getX() - (move.vecZ), centerMarker.getY() - (move.vecY), centerMarker.getZ() - (move.vecX)); //(float) (centerYaw - ((move.yaw * 0.8))), move.pitch + 10
        //player.sendMessage(new LiteralText("Yaw: " + centerMarker.getYaw() + " Pitch: " + centerMarker.getPitch()+ " X:" + centerMarker.getX() + " Y:" + centerMarker.getY() + " Z:" + centerMarker.getZ()), false);
    }

    private static ArmorStandEntity generateArmorStand(BlockPos pos, ServerWorld world, ServerPlayerEntity player) {
        ArmorStandEntity armorStand1 = EntityType.ARMOR_STAND.create(world);
        armorStand1.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F);
        armorStand1.setNoGravity(true);

        if (player.getMainHandStack().getOrCreateNbt().get("Long") == null) {
            player.getMainHandStack().getOrCreateNbt().putFloat("Long", 3.0f);
        }
        armorStand1.setStackInHand(Hand.MAIN_HAND, player.getMainHandStack());
        world.spawnEntity(armorStand1);
        player.getServer().getCommandManager().executeWithPrefix(player.getCommandSource().withSilent(), "/data merge entity " + armorStand1.getUuidAsString() + " {NoBasePlate:1b,ShowArms:1b}");
        return armorStand1;
    }

    public static HashMap<String, ArmorStandEntity> createArmorMap(BlockPos pos, ServerWorld world, ServerPlayerEntity player) {
        HashMap<String, ArmorStandEntity> armorStands1 = new HashMap<>();
        armorStands1.put("head", generateArmorStand(pos.add(new Vec3i(0, 2, 0)), world, player));
        //armorStands1.put("leftForeArm", generateArmorStand(pos.add(new Vec3i(2, 0, 0)), world, player));
        //armorStands1.put("rightForeArm", generateArmorStand(pos.add(new Vec3i(-2, 0, 0)), world, player));
        //armorStands.put("leftLeg", generateArmorStand(pos.add(new Vec3i(1, -2, 0)), world, player));
        // armorStands.put("rightLeg", generateArmorStand(pos.add(new Vec3i(-1, -2, 0)), world, player));
        armorStands1.put("leftShoulder", generateArmorStand(pos.add(new Vec3i(1, 0, 0)), world, player));
        armorStands1.put("rightShoulder", generateArmorStand(pos.add(new Vec3i(-1, 0, 0)), world, player));

        for (Map.Entry<String, ArmorStandEntity> entry : armorStands1.entrySet()) {
            ArmorStandEntity entity = entry.getValue();
            entity.addScoreboardTag("kinect");
            if(entry.getKey().contains("head")) {
                entity.addScoreboardTag("kinect_head");
            }
            if (entry.getKey().contains("left")) {
                entity.addScoreboardTag("kinect_arm");
                entity.addScoreboardTag("kinect_arm_left");

                if (entry.getKey().contains("Shoulder")) {
                    entity.addScoreboardTag("kinect_arm_left_shoulder");
                } else if(entry.getKey().contains("ForeArm")) {
                    entity.addScoreboardTag("kinect_arm_left_forearm");
                }
            } else {
                entity.addScoreboardTag("kinect_arm");
                entity.addScoreboardTag("kinect_arm_right");

                if (entry.getKey().contains("Shoulder")) {
                    entity.addScoreboardTag("kinect_arm_right_shoulder");
                } else if(entry.getKey().contains("ForeArm")) {
                    entity.addScoreboardTag("kinect_arm_right_forearm");
                }
            }

        }
        return armorStands1;
    }

    private EulerAngle toEulerAngle(Vec3f rotation) {
        float pitch = rotation.getX();
        float yaw = rotation.getY();
        float roll = rotation.getZ();
        return new EulerAngle(pitch, yaw, roll);
    }

}
