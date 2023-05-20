package me.legadyn.kinectminecraft.fabric;

import me.legadyn.kinectminecraft.ArmorStandMovement;
import me.legadyn.kinectminecraft.SplitAmorStandMovement;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MarkerEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ConvertedArmorStand {

    private ArmorStandEntity armorStand;
    private MarkerEntity centerMarker;
    HashMap<String, ArmorStandEntity> armorStands;
    private LinkedList<ArmorStandMovement> movements = new LinkedList<>();
    double playerYaw;

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
        armorStand.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        armorStand.setNoGravity(true);

        world.spawnEntity(armorStand);
        player.getServer().getCommandManager().execute(player.getCommandSource().withSilent(),"/data merge entity "+ armorStand.getUuidAsString() + " {NoBasePlate:1b,ShowArms:1b}");

        addMovements(list);
    }

    public ConvertedArmorStand(ServerPlayerEntity player, MarkerEntity marker, LinkedList<String> list) {
        this.centerMarker = marker;
        ServerWorld world = player.getWorld();
        BlockPos pos = player.getBlockPos().offset(player.getHorizontalFacing(), 2);
        playerYaw = player.getYaw();

        centerMarker = EntityType.MARKER.create(world);
        centerMarker.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        centerMarker.setNoGravity(true);
        world.spawnEntity(centerMarker);

        SplitArmorStand.createArmorMap(armorStands, pos, world, player);
        addMovements(list);
    }

    public ConvertedArmorStand(MarkerEntity entity, LinkedList<String> list) {
        //TO DO resume split armor stand with json
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

        //if a split armorstand exists, takes priority
        if(centerMarker != null) {
            for(Map.Entry<String, ArmorStandEntity> entry : armorStands.entrySet()) {
                String key = entry.getKey();
                ArmorStandEntity armorStandEntity = entry.getValue();

                switch (key) {
                    case "head":
                        armorStandEntity.setHeadRotation(toEulerAngle(new Vec3f(move.headPitch, 0, 0)));
                        armorStandEntity.refreshPositionAndAngles(centerMarker.getBlockPos().add(new Vec3i(0, 2, 0)), 0, 0);
                        break;

                    case "leftForeArm":
                        armorStandEntity.setLeftArmRotation(toEulerAngle(new Vec3f(-move.left_armX, move.left_armY, 0)));
                        armorStandEntity.refreshPositionAndAngles(centerMarker.getBlockPos().add(new Vec3i(1, 0, 0)), 0, 0);
                        break;

                    case "rightForeArm":
                        armorStandEntity.setRightArmRotation(toEulerAngle(new Vec3f(-move.right_armX, move.right_armY, 0)));
                        armorStandEntity.refreshPositionAndAngles(centerMarker.getBlockPos().add(new Vec3i(-1, 0, 0)), 0, 0);
                        break;

                    //case "leftLeg" -> armorStandEntity.setLeftLegRotation(toEulerAngle(new Vec3f(move.left_legX, move.left_legY, 0)));
                    //case "rightLeg" -> armorStandEntity.setRightLegRotation(toEulerAngle(new Vec3f(move.right_legX, move.right_legY, 0)));
                    case "leftShoulder":
                        armorStandEntity.setLeftArmRotation(toEulerAngle(new Vec3f(-move.left_upper_armX, move.left_upper_armY, 0)));
                        armorStandEntity.refreshPositionAndAngles(centerMarker.getBlockPos().add(new Vec3i(2, 0, 0)), 0, 0);
                        break;

                    case "rightShoulder":
                        armorStandEntity.setRightArmRotation(toEulerAngle(new Vec3f(-move.right_upper_armX, move.right_upper_armY, 0)));
                        armorStandEntity.refreshPositionAndAngles(centerMarker.getBlockPos().add(new Vec3i(-2, 0, 0)), 0, 0);
                        break;

                    default:
                        break;
                }
            }
            centerMarker.updatePositionAndAngles(centerMarker.getX() - move.vecZ, centerMarker.getY() - move.vecY , centerMarker.getZ() - move.vecX, (float) (centerMarker.getYaw() - (move.yaw * 0.8)),  move.pitch + 10);
            return;
        }

        armorStand.setHeadRotation(toEulerAngle(new Vec3f(move.headPitch,0,0)));

        armorStand.setRightArmRotation(toEulerAngle(new Vec3f(-move.right_armX,move.right_armY,0)));
        armorStand.setLeftArmRotation(toEulerAngle(new Vec3f(-move.left_armX,move.left_armY,0)));

        armorStand.setRightLegRotation(toEulerAngle(new Vec3f(move.right_legX,move.right_legY,0)));
        armorStand.setLeftLegRotation(toEulerAngle(new Vec3f(move.left_legX,move.left_legY,0)));

        //check if is refresh or update - check if armorstand.getyaw is ok
        armorStand.updatePositionAndAngles(armorStand.getX() - move.vecZ, armorStand.getY() - move.vecY , armorStand.getZ() - move.vecX, (float) (playerYaw - (move.yaw * 0.8)),  move.pitch + 10);

    }

    public void nextSplitMovement(short tick) {
        ArmorStandMovement move = movements.get(tick);

        for(Map.Entry<String, ArmorStandEntity> entry : armorStands.entrySet()) {
            String key = entry.getKey();
            ArmorStandEntity armorStandEntity = entry.getValue();

            switch (key) {
                case "head": armorStandEntity.setHeadRotation(toEulerAngle(new Vec3f(move.headPitch, 0, 0)));
                    armorStandEntity.refreshPositionAndAngles(centerMarker.getBlockPos().add(new Vec3i(0, 2, 0)),0,0);
                    break;

                case "leftForeArm": armorStandEntity.setLeftArmRotation(toEulerAngle(new Vec3f(-move.left_armX, move.left_armY, 0)));
                    armorStandEntity.refreshPositionAndAngles(centerMarker.getBlockPos().add(new Vec3i(1, 0, 0)),0,0);
                    break;

                case "rightForeArm": armorStandEntity.setRightArmRotation(toEulerAngle(new Vec3f(-move.right_armX, move.right_armY, 0)));
                    armorStandEntity.refreshPositionAndAngles(centerMarker.getBlockPos().add(new Vec3i(-1, 0, 0)),0,0);
                    break;

                //case "leftLeg" -> armorStandEntity.setLeftLegRotation(toEulerAngle(new Vec3f(move.left_legX, move.left_legY, 0)));
                //case "rightLeg" -> armorStandEntity.setRightLegRotation(toEulerAngle(new Vec3f(move.right_legX, move.right_legY, 0)));
                case "leftShoulder": armorStandEntity.setLeftArmRotation(toEulerAngle(new Vec3f(-move.left_upper_armX, move.left_upper_armY, 0)));
                    armorStandEntity.refreshPositionAndAngles(centerMarker.getBlockPos().add(new Vec3i(2, 0, 0)),0,0);
                    break;

                case "rightShoulder": armorStandEntity.setRightArmRotation(toEulerAngle(new Vec3f(-move.right_upper_armX, move.right_upper_armY, 0)));
                    armorStandEntity.refreshPositionAndAngles(centerMarker.getBlockPos().add(new Vec3i(-2, 0, 0)),0,0);
                    break;

                default: break;
            }

        }
        //check if is refresh or update - check if armorstand.getyaw is ok
        centerMarker.updatePositionAndAngles(centerMarker.getX() - move.vecZ, centerMarker.getY() - move.vecY , centerMarker.getZ() - move.vecX, (float) (centerMarker.getYaw() - (move.yaw * 0.8)),  move.pitch + 10);
    }

    private EulerAngle toEulerAngle(Vec3f rotation) {
        float pitch = rotation.getX();
        float yaw = rotation.getY();
        float roll = rotation.getZ();
        return new EulerAngle(pitch, yaw, roll);
    }

    public ArmorStandEntity getArmorStand() {
        return armorStand;
    }
}
