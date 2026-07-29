package net.realm_keys.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.realm_keys.RealmKeys;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RealmKeysConfig {
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "realm_keys.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean perPlayerProgression = true;

    public Set<String> globalUnlockedDimensions = new HashSet<>();

    public Map<String, Set<String>> playerUnlockedDimensions = new HashMap<>();

    private static RealmKeysConfig instance;

    public static void load() {
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                instance = GSON.fromJson(reader, RealmKeysConfig.class);
            } catch (Exception e) {
                RealmKeys.LOGGER.error("Failed to load Realm Keys config", e);
                instance = new RealmKeysConfig();
            }
        } else {
            instance = new RealmKeysConfig();
            instance.globalUnlockedDimensions.add("minecraft:overworld");
            save();
        }

        if (instance.playerUnlockedDimensions == null) instance.playerUnlockedDimensions = new HashMap<>();
        if (instance.globalUnlockedDimensions == null) instance.globalUnlockedDimensions = new HashSet<>();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(instance, writer);
        } catch (Exception e) {
            RealmKeys.LOGGER.error("Failed to save Realm Keys config", e);
        }
    }

    public static RealmKeysConfig getInstance() {
        return instance;
    }

    public static boolean hasAccess(UUID playerUuid, String dimensionId) {
        if (instance.globalUnlockedDimensions.contains(dimensionId)) return true;

        if (instance.perPlayerProgression) {
            Set<String> playerDims = instance.playerUnlockedDimensions.get(playerUuid.toString());
            return playerDims != null && playerDims.contains(dimensionId);
        }
        return false;
    }

    public static boolean unlockGlobal(String dimensionId) {
        if (instance.globalUnlockedDimensions.add(dimensionId)) {
            save();
            return true;
        }
        return false;
    }

    public static boolean lockGlobal(String dimensionId) {
        if (instance.globalUnlockedDimensions.remove(dimensionId)) {
            save();
            return true;
        }
        return false;
    }

    public static boolean unlockForPlayer(UUID playerUuid, String dimensionId) {
        Set<String> dims = instance.playerUnlockedDimensions.computeIfAbsent(playerUuid.toString(), k -> new HashSet<>());
        if (dims.add(dimensionId)) {
            save();
            return true;
        }
        return false;
    }

    public static boolean lockForPlayer(UUID playerUuid, String dimensionId) {
        Set<String> dims = instance.playerUnlockedDimensions.get(playerUuid.toString());
        if (dims != null && dims.remove(dimensionId)) {
            if (dims.isEmpty()) instance.playerUnlockedDimensions.remove(playerUuid.toString());
            save();
            return true;
        }
        return false;
    }
}