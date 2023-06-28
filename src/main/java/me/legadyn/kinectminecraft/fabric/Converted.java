package me.legadyn.kinectminecraft.fabric;

import net.minecraft.entity.decoration.ArmorStandEntity;

import java.util.HashMap;
import java.util.LinkedList;

public interface Converted {
    void addMovements(LinkedList<String> movements);
    void nextMovement(short tick);
    void remove();
    void setCenterPos(double x, double y, double z);
    void setYaw(float yaw);
    void setPitch(float pitch);
    ArmorStandEntity getArmorStand();
    HashMap<String, ArmorStandEntity> getArmorStands();
}
