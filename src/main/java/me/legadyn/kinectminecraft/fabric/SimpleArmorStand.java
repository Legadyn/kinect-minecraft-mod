package me.legadyn.kinectminecraft.fabric;

import me.legadyn.kinectminecraft.ArmorStandMovement;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.LinkedList;

public class SimpleArmorStand implements Converted {

    ArmorStandEntity armorStand;

    private LinkedList<ArmorStandMovement> movements = new LinkedList<>();

    float playerYaw;
    float armorYaw;

    //Constructor for resuming
    public SimpleArmorStand(ArmorStandEntity armorStand, LinkedList<String> list) {
        this.armorStand = armorStand;
        addMovements(list);
    }

    //Constructor for a new armorstand
    public SimpleArmorStand(ServerPlayerEntity player, LinkedList<String> list, double x, double y, double z) {
        World world = player.getEntityWorld();
        BlockPos pos = player.getBlockPos();

        armorStand = EntityType.ARMOR_STAND.create(world);

        armorStand.refreshPositionAndAngles(x, y, z, player.getBodyYaw(), 0.0F);
        armorStand.setNoGravity(true);

        playerYaw = player.getYaw();
        armorYaw = armorStand.getBodyRotation().getYaw();

        world.spawnEntity(armorStand);
        player.getServer().getCommandManager().executeWithPrefix(player.getCommandSource().withSilent(),"/data merge entity "+ armorStand.getUuidAsString() + " {NoBasePlate:1b,ShowArms:1b}");

        addMovements(list);
    }

    @Override
    public void addMovements(LinkedList<String> list) {
        list.forEach(s -> {
            String[] decoded = s.split(":");
            ArmorStandMovement movement = new ArmorStandMovement();
            movement.convert(decoded);
            movements.add(movement);
        });
    }

    @Override
    public void nextMovement(short tick) {
        ArmorStandMovement move = movements.get(tick);
        armorYaw = armorStand.getBodyRotation().getYaw();

        armorStand.setHeadRotation(toEulerAngle(new Vec3f(move.headPitch,0,0)));

        armorStand.setRightArmRotation(toEulerAngle(new Vec3f(-move.right_armX,move.right_armY,0)));
        armorStand.setLeftArmRotation(toEulerAngle(new Vec3f(-move.left_armX,move.left_armY,0)));

        armorStand.setRightLegRotation(toEulerAngle(new Vec3f(move.right_legX,move.right_legY,0)));
        armorStand.setLeftLegRotation(toEulerAngle(new Vec3f(move.left_legX,move.left_legY,0)));

        //check if is refresh or update - check if armorstand.getyaw is ok
        armorStand.updatePosition(armorStand.getX() - move.vecZ, armorStand.getY() - move.vecY, armorStand.getZ() - move.vecX); //(float) (armorYaw - (move.yaw * 0.8)),  move.pitch + 10

    }

    @Override
    public void remove() {
        armorStand.kill();
    }

    @Override
    public void setCenterPos(double x, double y, double z) {
        armorStand.updatePosition(x, y, z);
    }

    @Override
    public void setYaw(float yaw) {
        armorStand.setBodyYaw(yaw);
    }

    @Override
    public void setPitch(float pitch) {
        armorStand.setPitch(pitch);
    }

    private EulerAngle toEulerAngle(Vec3f rotation) {
        float pitch = rotation.getX();
        float yaw = rotation.getY();
        float roll = rotation.getZ();
        return new EulerAngle(pitch, yaw, roll);
    }

    @Override
    public ArmorStandEntity getArmorStand() {
        return armorStand;
    }

    @Override
    public HashMap<String, ArmorStandEntity> getArmorStands() {
        return null;
    }
}
