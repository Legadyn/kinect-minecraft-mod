package me.legadyn.kinectminecraft.fabric;

import me.legadyn.kinectminecraft.ArmorStandMovement;
import me.legadyn.kinectminecraft.utils.FileUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class MultiArmorStand implements Converted{

    private ArmorStandEntity centerArmor;
    private HashMap<String, ArmorStandEntity> armorStands;

    private LinkedList<ArmorStandMovement> movements = new LinkedList<>();

    float playerYaw, centerYaw;

    //Constructor for resuming
    public MultiArmorStand(ArmorStandEntity armorStand, LinkedList<String> list) {
        this.centerArmor = armorStand;
        armorStands = FileUtils.getMultiState(centerArmor.getUuidAsString(), centerArmor.getEntityWorld());
        addMovements(list);
    }

    //Constructor for new MultiArmorStand
    public MultiArmorStand(ServerPlayerEntity player, LinkedList<String> list, double x, double y, double z) {
        ServerWorld world = player.getWorld();
        BlockPos pos = new BlockPos(new Vec3d(x, y, z));

        playerYaw = player.getBodyYaw();

        centerArmor = EntityType.ARMOR_STAND.create(world);
        centerArmor.refreshPositionAndAngles(x, y, z, player.getBodyYaw(), 0.0F);
        centerArmor.getScoreboardTags().add("center");
        centerArmor.setNoGravity(true);
        centerArmor.setInvisible(true);
        centerArmor.setInvulnerable(true);

        world.spawnEntity(centerArmor);

        armorStands = SplitArmorStand.createArmorMap(pos, world, player);
        addMovements(list);
    }

    @Override
    public void addMovements(LinkedList<String> list) {
        list.forEach(s -> {
            String[] decoded = s.split(":");
            ArmorStandMovement movement = new ArmorStandMovement();
            movement.convert(decoded);
            this.movements.add(movement);
        });
    }

    @Override
    public void nextMovement(short tick) {

        ArmorStandMovement move = movements.get(tick);
        centerYaw = centerArmor.getYaw();
        float centerPitch = centerArmor.getPitch();
        Vec3d centerPos = centerArmor.getPos();
        //Updating position and angles for all armorstands
        for(Map.Entry<String, ArmorStandEntity> entry : armorStands.entrySet()) {
            String key = entry.getKey();
            ArmorStandEntity armorStandEntity = entry.getValue();
            NbtCompound nbt = armorStandEntity.getMainHandStack().getOrCreateNbt();
            //Stuff for rotate armorstands around center
            double distancia = Math.abs(armorStandEntity.getX() - centerArmor.getX()); // distancia entre los dos ArmorStands
            double angulo = Math.toRadians(centerArmor.getYaw());
            double offsetX = Math.cos(angulo) * distancia;
            double offsetZ = Math.sin(angulo) * distancia;
            Vec3d posSecundario;

            //TEMPORAL CHANGE**************
            centerPos = armorStandEntity.getPos();

            switch (key) {
                case "head":
                    armorStandEntity.setHeadRotation(toEulerAngle(new Vec3f(move.headPitch, 0, 0)));
                    Vec3d headOffset = new Vec3d(0,0,0);//new Vec3d(0, nbt.getFloat("Long"), 0);
                    //centerArmor.refreshPositionAfterTeleport(centerPos.getX(), centerPos.getY(), centerPos.getZ());
                    armorStandEntity.updatePositionAndAngles(centerPos.getX(), centerPos.getY(), centerPos.getZ(), centerYaw, centerPitch);

                    //armorStandEntity.updatePositionAndAngles(centerPos.getX() + headOffset.getX(), centerPos.getY() + headOffset.getY(), centerPos.getZ() + headOffset.getZ(), (float) (centerYaw - ((move.yaw * 0.8))), move.pitch + 10);
                    break;

                case "leftForeArm":
                    armorStandEntity.setLeftArmRotation(toEulerAngle(new Vec3f(-move.left_lower_armX, move.left_lower_armY, 0)));
                    Vec3d leftForeArmOffset = new Vec3d(0,0,0);//new Vec3d(nbt.getFloat("Long"), 0, 0);
                    armorStandEntity.updatePositionAndAngles(centerPos.getX(), centerPos.getY(), centerPos.getZ(), (float) (centerYaw - ((move.yaw * 0.8))), move.pitch + 10);
                    //armorStandEntity.refreshPositionAndAngles(new BlockPos(centerMarker.getPos().add(leftForeArmOffset)), (float) (centerYaw - ((move.yaw * 0.8)/120)), move.pitch + 10);
                    break;

                case "rightForeArm":
                    armorStandEntity.setRightArmRotation(toEulerAngle(new Vec3f(-move.right_lower_armX, move.right_lower_armY, 0)));
                    Vec3d rightForeArmOffset = new Vec3d(0,0,0);//new Vec3d(-nbt.getFloat("Long"), 0, 0);
                    armorStandEntity.updatePositionAndAngles(centerPos.getX(), centerPos.getY(), centerPos.getZ(), (float) (centerYaw - ((move.yaw * 0.8))), move.pitch + 10);
                    //armorStandEntity.refreshPositionAndAngles(new BlockPos(centerMarker.getPos().add(rightForeArmOffset)), (float) (centerYaw - ((move.yaw * 0.8)/120)), move.pitch + 10);
                    break;

                case "leftShoulder":
                    armorStandEntity.setLeftArmRotation(toEulerAngle(new Vec3f(-move.left_upper_armX, move.left_upper_armY, 0)));
                    Vec3d leftShoulderOffset = new Vec3d(0,0,0);//new Vec3d(nbt.getFloat("Long")-1, 0, 0);
                    double diffX = armorStandEntity.getX() - centerPos.getX();

                    posSecundario = centerArmor.getPos().add(offsetX, 0, offsetZ);
                    armorStandEntity.updatePositionAndAngles(posSecundario.getX(), centerPos.getY(), posSecundario.getZ(), centerYaw, centerPitch);
                    //armorStandEntity.refreshPositionAndAngles(new BlockPos(centerMarker.getPos().add(leftShoulderOffset)), (float) (centerYaw - ((move.yaw * 0.8)/120)), move.pitch + 10);
                    break;

                case "rightShoulder":
                    armorStandEntity.setRightArmRotation(toEulerAngle(new Vec3f(-move.right_upper_armX, move.right_upper_armY, 0)));
                    Vec3d rightShoulderOffset = new Vec3d(0,0,0); //new Vec3d(-nbt.getFloat("Long")+1, 0, 0);
                    double diffX2 = armorStandEntity.getX() - centerPos.getX();
                    posSecundario = centerArmor.getPos().subtract(offsetX, 0, offsetZ);
                    armorStandEntity.updatePositionAndAngles(posSecundario.getX(), centerPos.getY(), posSecundario.getZ(), centerYaw, centerPitch);
                    //armorStandEntity.refreshPositionAndAngles(new BlockPos(centerMarker.getPos().add(rightShoulderOffset)), (float) (centerYaw - ((move.yaw * 0.8)/120)), move.pitch + 10);
                    break;

                default:
                    break;
            }

        }
        //check if is refresh or update - check if armorstand.getyaw is ok
        centerArmor.updatePosition(centerArmor.getX(), centerArmor.getY(), centerArmor.getZ());
        //centerArmor.updatePositionAndAngles(centerArmor.getX(), centerArmor.getY(), centerArmor.getZ(), centerArmor.getBodyRotation().getYaw(), centerArmor.getPitch());
        //centerArmor.refreshPositionAfterTeleport(centerArmor.getX(), centerArmor.getY(), centerArmor.getZ());

    }

    @Override
    public void remove() {
        armorStands.forEach((s, armorStandEntity) -> {
            armorStandEntity.remove(Entity.RemovalReason.DISCARDED);
        });
    }

    @Override
    public void setCenterPos(double x, double y, double z) {
        centerArmor.setPos(x, y, z);
    }

    @Override
    public void setYaw(float yaw) {
        centerArmor.setYaw(yaw);
    }

    @Override
    public void setPitch(float pitch) {
        centerArmor.setPitch(pitch);
    }

    private EulerAngle toEulerAngle(Vec3f rotation) {
        float pitch = rotation.getX();
        float yaw = rotation.getY();
        float roll = rotation.getZ();
        return new EulerAngle(pitch, yaw, roll);
    }

    @Override
    public ArmorStandEntity getArmorStand() {
        return centerArmor;
    }

    public HashMap<String, ArmorStandEntity> getArmorStands() {
        return armorStands;
    }
}
