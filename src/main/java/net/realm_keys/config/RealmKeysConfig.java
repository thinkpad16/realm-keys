package net.realm_keys.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RealmKeysConfig {
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "realm_keys_settings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // --- Progression ---
    public boolean perPlayerProgression = true;

    // Dimensions listed here never require an unlock, regardless of lock state.
    // Empty this list to require a key for the overworld too.
    public List<String> exemptDimensions = new ArrayList<>(List.of("minecraft:overworld"));

    // --- Commands ---
    // Permission level (op level) required to run /realmkeys admin subcommands.
    public int commandPermissionLevel = 2;

    // --- Feature toggles ---
    public boolean enableUnlockEffects = true;
    public boolean enableBlockEffects = true;
    public boolean enableBlockMessage = true;
    public boolean enableWelcomeTitles = true;
    public boolean enableFirstDiscovererBroadcast = true;
    public boolean enableCustomSpawns = true;

    // --- Messages ---
    // Empty by default: built-in messages come from the lang files (net.realm_keys.util.RealmKeysText).
    // Set a value here only to override the translated default for that dimension.
    public String firstDiscovererMessage = "";

    // Fallback title/message applied to any dimension that has no per-dimension entry below
    // and isn't one of the built-in flavored dimensions (Nether/End). Empty disables the fallback.
    // Supports %dimension% and & color codes.
    public String defaultWelcomeTitle = "";
    public String defaultLockedMessage = "";

    public Map<String, String> welcomeTitles = new HashMap<>();
    public Map<String, String> lockedMessages = new HashMap<>();

    // --- Effects ---
    // Sound/particle ids use "namespace:path" registry names (e.g. "minecraft:entity.enderman.teleport").
    // Unknown ids are logged and skipped, so a typo won't crash the server.
    public List<SoundConfig> unlockSounds = defaultUnlockSounds();
    public List<SoundConfig> discovererSounds = defaultDiscovererSounds();
    public List<SoundConfig> blockSounds = defaultBlockSounds();
    public List<ParticleConfig> blockParticles = defaultBlockParticles();
    public double blockKnockbackStrength = 1.0;

    private static List<SoundConfig> defaultUnlockSounds() {
        return new ArrayList<>(List.of(
                new SoundConfig("minecraft:block.end_portal.spawn", 1.0f, 1.0f),
                new SoundConfig("minecraft:ui.toast.challenge_complete", 1.0f, 1.0f)
        ));
    }

    private static List<SoundConfig> defaultDiscovererSounds() {
        return new ArrayList<>(List.of(
                new SoundConfig("minecraft:ui.toast.challenge_complete", 1.0f, 1.0f)
        ));
    }

    private static List<SoundConfig> defaultBlockSounds() {
        return new ArrayList<>(List.of(
                new SoundConfig("minecraft:item.shield.block", 1.0f, 0.5f),
                new SoundConfig("minecraft:entity.enderman.teleport", 0.5f, 0.5f)
        ));
    }

    private static List<ParticleConfig> defaultBlockParticles() {
        return new ArrayList<>(List.of(
                new ParticleConfig("minecraft:large_smoke", 20, 0.5, 0.5, 0.5, 0.05),
                new ParticleConfig("minecraft:witch", 10, 0.5, 0.5, 0.5, 0.1)
        ));
    }

    public static class SoundConfig {
        public String sound;
        public float volume;
        public float pitch;

        public SoundConfig() {}

        public SoundConfig(String sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }
    }

    public static class ParticleConfig {
        public String particle;
        public int count;
        public double spreadX;
        public double spreadY;
        public double spreadZ;
        public double speed;

        public ParticleConfig() {}

        public ParticleConfig(String particle, int count, double spreadX, double spreadY, double spreadZ, double speed) {
            this.particle = particle;
            this.count = count;
            this.spreadX = spreadX;
            this.spreadY = spreadY;
            this.spreadZ = spreadZ;
            this.speed = speed;
        }
    }

    private static RealmKeysConfig instance;

    public static void load() {
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                instance = GSON.fromJson(reader, RealmKeysConfig.class);
            } catch (Exception e) {
                instance = new RealmKeysConfig();
                save();
            }
        } else {
            instance = new RealmKeysConfig();
            save();
        }

        if (instance.exemptDimensions == null) instance.exemptDimensions = new ArrayList<>();
        if (instance.welcomeTitles == null) instance.welcomeTitles = new HashMap<>();
        if (instance.lockedMessages == null) instance.lockedMessages = new HashMap<>();
        if (instance.unlockSounds == null) instance.unlockSounds = new ArrayList<>();
        if (instance.discovererSounds == null) instance.discovererSounds = new ArrayList<>();
        if (instance.blockSounds == null) instance.blockSounds = new ArrayList<>();
        if (instance.blockParticles == null) instance.blockParticles = new ArrayList<>();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(instance, writer);
        } catch (Exception ignored) {}
    }

    public static RealmKeysConfig getInstance() {
        if (instance == null) load();
        return instance;
    }
}
