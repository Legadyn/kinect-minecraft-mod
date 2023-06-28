package me.legadyn.kinectminecraft.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChangeRotationEvent {

    private static ArrayList<ArmorStandEntity> armorStands = new ArrayList<ArmorStandEntity>();

    private static HashMap<ArmorStandEntity,HashMap<String, ArmorStandEntity>> armorParts = new HashMap<>();

    private static final Map<ArmorStandEntity, Vec2f> prevState = new HashMap<>();

    public static void registerArmorStand(ArmorStandEntity armor, HashMap<String, ArmorStandEntity> armorStandMap) {
        armorStands.add(armor);
        armorParts.put(armor, armorStandMap);
    }

    static double distancia = 0;

    public static void startListening() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {

            if(armorStands.isEmpty()) return;

            armorStands.forEach(armor -> {
                if(prevState.get(armor) == null) {
                    prevState.put(armor, new Vec2f(armor.getYaw(), armor.getPitch()));
                }
                Vec2f prevRot = prevState.get(armor);
                Vec2f currRot = new Vec2f(armor.getYaw(), armor.getPitch());
                //Comparing to check if a rotation is made

                if(!prevRot.equals(currRot)) {

                    HashMap<String, ArmorStandEntity> armorsMap= armorParts.get(armor);
                    armorsMap.forEach((name, armorstand) -> {
                        //update rotation of all armorstands except the center
                        if(!name.equals("center") && armor.getEntityWorld().getEntityById(armorstand.getId()) != null) {
                            armorstand.setYaw(armor.getYaw());
                            armorstand.setPitch(armor.getPitch());
                            double angulo = Math.toRadians(armor.getYaw());
                            Vec3d newPos = new Vec3d(0,0,0);

                            if(name.contains("right")) {
                                distancia = Math.abs(armorstand.getX() - armor.getX());
                                System.out.println(distancia);
                                newPos=armor.getPos().subtract(Math.cos(angulo) * distancia, 0, Math.sin(angulo) * distancia);
                                armorstand.updatePosition(newPos.getX(), armor.getY(), newPos.getZ());
                            } else if(name.contains("left")) {
                                newPos=armor.getPos().add(Math.cos(angulo) * distancia, 0, Math.sin(angulo) * distancia);
                                armorstand.updatePosition(newPos.getX(), armor.getY(), newPos.getZ());
                            }

                        }
                    });
                }
                prevState.put(armor, currRot);
            });
        });
    }



}
