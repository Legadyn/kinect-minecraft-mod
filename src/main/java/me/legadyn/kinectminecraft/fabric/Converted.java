package me.legadyn.kinectminecraft.fabric;

import java.util.LinkedList;

public interface Converted {
    void addMovements(LinkedList<String> movements);
    void nextMovement(short tick);
    void remove();
    void setCenterPos(double x, double y, double z);
    void setYaw(double yaw);
    void setPitch(double pitch);

}
