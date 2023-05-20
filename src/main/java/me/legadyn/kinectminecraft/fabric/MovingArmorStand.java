package me.legadyn.kinectminecraft.fabric;

import me.legadyn.kinectminecraft.ArmorStandMovement;
import me.legadyn.kinectminecraft.KinectArmorStand;
import net.minecraft.client.render.entity.model.ArmorStandArmorEntityModel;
import net.minecraft.client.render.entity.model.ArmorStandEntityModel;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class MovingArmorStand {

    Vec3d position;
    private ArmorStandEntity armorStand;
    float playerYaw;

    float armorYaw;
    double armorY;
    double armorX;
    double armorZ;

    ServerPlayerEntity player;
    public MovingArmorStand(Vec3d pos) {
        position = pos;
        position.add(position.multiply(3));
    }

    public void spawn(ServerPlayerEntity player) {
        ServerWorld world = player.getWorld();
        BlockPos pos = player.getBlockPos().offset(player.getHorizontalFacing(), 2);
        this.player = player;

        armorStand = EntityType.ARMOR_STAND.create(world);
        armorStand.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        armorStand.setNoGravity(true);

        armorYaw = armorStand.getBodyRotation().getYaw();
        armorY = pos.getY();
        armorX = pos.getX() + 0.5D;
        armorZ = pos.getZ() + 0.5D;

        world.spawnEntity(armorStand);
        playerYaw = armorStand.getYaw();
        player.getServer().getCommandManager().execute(player.getCommandSource().withSilent(),"/data merge entity "+ armorStand.getUuidAsString() + " {NoBasePlate:1b,ShowArms:1b}");

    }

    public void update(String packet) {

        String[] decoded = packet.split(":");
        ArmorStandMovement move;
        // try catch for unknown error (packet has 33-19 on pitch and it should be 33:19)
        try {
             move = new ArmorStandMovement(decoded);
        } catch(Exception e) {
            e.printStackTrace();
            return;
        }

        //adds every movement to the main cache in KinectArmorStand
        if(KinectArmorStand.isStartSaving()) {
            KinectArmorStand.getInstance().getCache().add(move);
        }

        armorStand.setHeadRotation(toEulerAngle(new Vec3f(move.headPitch,0,0)));

        armorStand.setRightArmRotation(toEulerAngle(new Vec3f(-move.right_armX,move.right_armY,0)));
        armorStand.setLeftArmRotation(toEulerAngle(new Vec3f(-move.left_armX,move.left_armY,0)));

        armorStand.setRightLegRotation(toEulerAngle(new Vec3f(move.right_legX,move.right_legY,0)));
        armorStand.setLeftLegRotation(toEulerAngle(new Vec3f(move.left_legX,move.left_legY,0)));

        //check if is refresh or update - check if armorstand.getyaw is ok
        //armorStand.updatePositionAndAngles(armorX - move.vecZ, armorY - move.vecY , armorZ - move.vecX, (float) (armorYaw - (move.yaw * 0.8)),  move.pitch + 10);
        double scaleFactor = 300.0;
        armorStand.updatePositionAndAngles(armorX - (move.vecZ / scaleFactor), armorY - (move.vecY / scaleFactor), armorZ - (move.vecX / scaleFactor), (float) (armorYaw - ((move.yaw * 0.8)/120)),  move.pitch + 10);
        player.sendMessage(new LiteralText("X: " + armorStand.getX() + " Y: " + armorStand.getY() + " Z: " + armorStand.getZ()), false);
    }

    private EulerAngle toEulerAngle(Vec3f rotation) {
        float pitch = rotation.getX();
        float yaw = rotation.getY();
        float roll = rotation.getZ();
        return new EulerAngle(pitch, yaw, roll);
    }

    public ArmorStandEntity getArmorStandEntity() {
        return armorStand;
    }

}
