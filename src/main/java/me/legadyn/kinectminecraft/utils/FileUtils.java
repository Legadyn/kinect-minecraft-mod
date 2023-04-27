package me.legadyn.kinectminecraft.utils;

import com.google.gson.JsonObject;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class FileUtils {
    public static JSONObject jsonObject;
    static Path filePath;

    public FileUtils(ServerWorld world) {
        Path configPath = world.getServer().getSavePath(WorldSavePath.ROOT).toFile().toPath();
        filePath = configPath.resolve("armorstands.json");
        // Leer el json una sola vez
        try {
            FileReader fileReader = new FileReader(filePath.toString());
            jsonObject = new JSONObject(new JSONTokener(fileReader));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    public static void createConfigFile(ServerWorld world) {
        // Obtener la carpeta de configuración de tu mod
        Path configPath = world.getServer().getSavePath(WorldSavePath.ROOT).toFile().toPath();
        Path filePath = configPath.resolve("armorstands.json");

        try {
            // Crear la carpeta de configuración si no existe
            Files.createDirectories(configPath);

            // Crear el archivo de texto si no existe
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeState(String UUID, float value) {

        JSONObject uuidObject = new JSONObject();
        uuidObject.put("tick", value);
        uuidObject.put("animation", "armorstand");
        jsonObject.put(UUID, uuidObject);

        try (FileWriter file = new FileWriter(filePath.toString())) {
            file.write(jsonObject.toString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static float getState(String UUID) {
        return jsonObject.getFloat(UUID);
    }


}