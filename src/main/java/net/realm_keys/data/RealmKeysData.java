package net.realm_keys.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.realm_keys.config.RealmKeysConfig;

import java.util.*;

public class RealmKeysData extends SavedData {
    public Set<String> globalUnlockedDimensions = new HashSet<>();
    public Map<String, Set<String>> playerUnlockedDimensions = new HashMap<>();
    public Map<String, Set<String>> unlockedChunks = new HashMap<>();
    public Map<String, String> firstDiscoverers = new HashMap<>();
    public Map<String, String> customSpawns = new HashMap<>();

    // Отримання даних, прив'язаних до збереження світу
    public static RealmKeysData getServerState(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(RealmKeysData::new, RealmKeysData::load, null),
                "realm_keys_data"
        );
    }

    public static RealmKeysData load(CompoundTag tag, HolderLookup.Provider registries) {
        RealmKeysData data = new RealmKeysData();

        // Global Dimensions
        ListTag globalList = tag.getList("GlobalDimensions", 8);
        for (int i = 0; i < globalList.size(); i++) data.globalUnlockedDimensions.add(globalList.getString(i));

        // Player Dimensions
        CompoundTag playersTag = tag.getCompound("PlayerDimensions");
        for (String uuid : playersTag.getAllKeys()) {
            ListTag pList = playersTag.getList(uuid, 8);
            Set<String> dims = new HashSet<>();
            for (int i = 0; i < pList.size(); i++) dims.add(pList.getString(i));
            data.playerUnlockedDimensions.put(uuid, dims);
        }

        // Chunks
        CompoundTag chunksTag = tag.getCompound("UnlockedChunks");
        for (String dim : chunksTag.getAllKeys()) {
            ListTag cList = chunksTag.getList(dim, 8);
            Set<String> chunks = new HashSet<>();
            for (int i = 0; i < cList.size(); i++) chunks.add(cList.getString(i));
            data.unlockedChunks.put(dim, chunks);
        }

        // Discoverers
        CompoundTag discTag = tag.getCompound("FirstDiscoverers");
        for (String dim : discTag.getAllKeys()) data.firstDiscoverers.put(dim, discTag.getString(dim));

        // Spawns
        CompoundTag spawnTag = tag.getCompound("CustomSpawns");
        for (String dim : spawnTag.getAllKeys()) data.customSpawns.put(dim, spawnTag.getString(dim));

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        // Global
        ListTag globalList = new ListTag();
        for (String dim : globalUnlockedDimensions) globalList.add(StringTag.valueOf(dim));
        tag.put("GlobalDimensions", globalList);

        // Players
        CompoundTag playersTag = new CompoundTag();
        for (Map.Entry<String, Set<String>> entry : playerUnlockedDimensions.entrySet()) {
            ListTag pList = new ListTag();
            for (String dim : entry.getValue()) pList.add(StringTag.valueOf(dim));
            playersTag.put(entry.getKey(), pList);
        }
        tag.put("PlayerDimensions", playersTag);

        // Chunks
        CompoundTag chunksTag = new CompoundTag();
        for (Map.Entry<String, Set<String>> entry : unlockedChunks.entrySet()) {
            ListTag cList = new ListTag();
            for (String chunk : entry.getValue()) cList.add(StringTag.valueOf(chunk));
            chunksTag.put(entry.getKey(), cList);
        }
        tag.put("UnlockedChunks", chunksTag);

        // Discoverers
        CompoundTag discTag = new CompoundTag();
        for (Map.Entry<String, String> entry : firstDiscoverers.entrySet()) discTag.putString(entry.getKey(), entry.getValue());
        tag.put("FirstDiscoverers", discTag);

        // Spawns
        CompoundTag spawnTag = new CompoundTag();
        for (Map.Entry<String, String> entry : customSpawns.entrySet()) spawnTag.putString(entry.getKey(), entry.getValue());
        tag.put("CustomSpawns", spawnTag);

        return tag;
    }

    // --- Логіка доступу (перенесена з Config) ---
    public boolean hasAccess(UUID playerUuid, String dimensionId) {
        if (RealmKeysConfig.getInstance().exemptDimensions.contains(dimensionId)) return true;
        if (globalUnlockedDimensions.contains(dimensionId)) return true;
        if (RealmKeysConfig.getInstance().perPlayerProgression) {
            Set<String> playerDims = playerUnlockedDimensions.get(playerUuid.toString());
            return playerDims != null && playerDims.contains(dimensionId);
        }
        return false;
    }

    public boolean unlockGlobal(String dimensionId) {
        if (globalUnlockedDimensions.add(dimensionId)) { setDirty(); return true; } return false;
    }

    public boolean lockGlobal(String dimensionId) {
        if (globalUnlockedDimensions.remove(dimensionId)) { setDirty(); return true; } return false;
    }

    public boolean unlockForPlayer(UUID playerUuid, String dimensionId) {
        Set<String> dims = playerUnlockedDimensions.computeIfAbsent(playerUuid.toString(), k -> new HashSet<>());
        if (dims.add(dimensionId)) { setDirty(); return true; } return false;
    }

    public boolean lockForPlayer(UUID playerUuid, String dimensionId) {
        Set<String> dims = playerUnlockedDimensions.get(playerUuid.toString());
        if (dims != null && dims.remove(dimensionId)) {
            if (dims.isEmpty()) playerUnlockedDimensions.remove(playerUuid.toString());
            setDirty(); return true;
        } return false;
    }

    public boolean isChunkUnlocked(String dimId, int cX, int cZ) {
        return unlockedChunks.containsKey(dimId) && unlockedChunks.get(dimId).contains(cX + "," + cZ);
    }

    public boolean unlockChunk(String dimId, int cX, int cZ) {
        Set<String> chunks = unlockedChunks.computeIfAbsent(dimId, k -> new HashSet<>());
        if (chunks.add(cX + "," + cZ)) { setDirty(); return true; } return false;
    }

    public boolean lockChunk(String dimId, int cX, int cZ) {
        Set<String> chunks = unlockedChunks.get(dimId);
        if (chunks != null && chunks.remove(cX + "," + cZ)) {
            if (chunks.isEmpty()) unlockedChunks.remove(dimId);
            setDirty(); return true;
        } return false;
    }

    public int unlockAllForPlayer(UUID playerUuid, Collection<String> dimensionIds) {
        int count = 0;
        for (String dim : dimensionIds) if (unlockForPlayer(playerUuid, dim)) count++;
        return count;
    }

    public int lockAllForPlayer(UUID playerUuid, Collection<String> dimensionIds) {
        int count = 0;
        for (String dim : dimensionIds) if (lockForPlayer(playerUuid, dim)) count++;
        return count;
    }

    public int unlockAllGlobal(Collection<String> dimensionIds) {
        int count = 0;
        for (String dim : dimensionIds) if (unlockGlobal(dim)) count++;
        return count;
    }

    public int lockAllGlobal(Collection<String> dimensionIds) {
        int count = 0;
        for (String dim : dimensionIds) if (lockGlobal(dim)) count++;
        return count;
    }

    // Дані стають "dirty" одразу після зміни, але вейнільний рушій пише SavedData на диск лише
    // під час автозбереження або коректного /stop. Якщо сервер перезапустити (чи вбити процес)
    // між цими моментами, свіжі розблокування губляться. Тому після кожної зміни примусово
    // скидаємо стан сховища даних на диск, а не чекаємо на наступне автозбереження.
    public static void flush(MinecraftServer server) {
        server.overworld().getDataStorage().save();
    }
}