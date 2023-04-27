package me.legadyn.kinectminecraft.fabric;

import me.legadyn.kinectminecraft.ArmorStandMovement;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

public class ConvertedArmorStand {

    private ArmorStandEntity armorStand;
    private LinkedList<ArmorStandMovement> movements = new LinkedList<>();
    double playerYaw;

    public ConvertedArmorStand(ArmorStandEntity armorStand, LinkedList<String> list) {
        this.armorStand = armorStand;
        list.forEach(s -> {
            String[] decoded = s.split(":");
            ArmorStandMovement movement = new ArmorStandMovement();
            movement.convert(decoded);
            movements.add(movement);
        });
    }
    public ConvertedArmorStand(ServerPlayerEntity player, LinkedList<String> list) {
        //check if serverworld works
        World world = player.getEntityWorld();
        BlockPos pos = player.getBlockPos().offset(player.getHorizontalFacing(), 2);
        playerYaw = player.getYaw();
        armorStand = EntityType.ARMOR_STAND.create(world);
        armorStand.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        armorStand.setNoGravity(true);
        ServerWorld serverWorld = (ServerWorld) world;


        try {
            Method[] methods = {ArmorStandEntity.class.getDeclaredMethod("setHideBasePlate", boolean.class),
                    ArmorStandEntity.class.getDeclaredMethod("setShowArms", boolean.class)};
            for(Method method : methods) {
                method.setAccessible(true);
                method.invoke(armorStand, true);
            }
        } catch(NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        world.spawnEntity(armorStand);

        list.forEach(s -> {
            String[] decoded = s.split(":");
            ArmorStandMovement movement = new ArmorStandMovement();
            movement.convert(decoded);
            movements.add(movement);
        });
    }

    public void nextMovement(short tick) {
        ArmorStandMovement move = movements.get(tick);

        armorStand.setHeadRotation(toEulerAngle(new Vec3f(move.headPitch,0,0)));

        armorStand.setRightArmRotation(toEulerAngle(new Vec3f(-move.right_armX,move.right_armY,0)));
        armorStand.setLeftArmRotation(toEulerAngle(new Vec3f(-move.left_armX,move.left_armY,0)));

        armorStand.setRightLegRotation(toEulerAngle(new Vec3f(move.right_legX,move.right_legY,0)));
        armorStand.setLeftLegRotation(toEulerAngle(new Vec3f(move.left_legX,move.left_legY,0)));

        //check if is refresh or update - check if armorstand.getyaw is ok
        armorStand.updatePositionAndAngles(armorStand.getX() - move.vecZ, armorStand.getY() - move.vecY , armorStand.getZ() - move.vecX, (float) (playerYaw - (move.yaw * 0.8)),  move.pitch + 10);

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
