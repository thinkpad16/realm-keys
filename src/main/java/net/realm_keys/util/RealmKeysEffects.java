package net.realm_keys.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.realm_keys.RealmKeys;
import net.realm_keys.config.RealmKeysConfig;

import java.util.List;

/**
 * Plays the sounds/particles listed in {@link RealmKeysConfig}. Ids that don't resolve to a
 * known registry entry are logged once and skipped, so a typo in the config can't crash anything.
 */
public final class RealmKeysEffects {
    private RealmKeysEffects() {}

    public static void playNotifySounds(ServerPlayer player, List<RealmKeysConfig.SoundConfig> sounds) {
        for (RealmKeysConfig.SoundConfig cfg : sounds) {
            SoundEvent event = resolveSound(cfg.sound);
            if (event != null) player.playNotifySound(event, SoundSource.MASTER, cfg.volume, cfg.pitch);
        }
    }

    public static void broadcastNotifySounds(MinecraftServer server, List<RealmKeysConfig.SoundConfig> sounds) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            playNotifySounds(player, sounds);
        }
    }

    public static void playWorldSounds(ServerPlayer player, List<RealmKeysConfig.SoundConfig> sounds) {
        for (RealmKeysConfig.SoundConfig cfg : sounds) {
            SoundEvent event = resolveSound(cfg.sound);
            if (event != null) {
                player.serverLevel().playSound(null, player.blockPosition(), event, SoundSource.PLAYERS, cfg.volume, cfg.pitch);
            }
        }
    }

    public static void spawnParticles(ServerPlayer player, List<RealmKeysConfig.ParticleConfig> particles) {
        for (RealmKeysConfig.ParticleConfig cfg : particles) {
            ParticleOptions options = resolveParticle(cfg.particle);
            if (options != null) {
                player.serverLevel().sendParticles(options, player.getX(), player.getY() + 1.0, player.getZ(),
                        cfg.count, cfg.spreadX, cfg.spreadY, cfg.spreadZ, cfg.speed);
            }
        }
    }

    private static SoundEvent resolveSound(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        SoundEvent event = loc == null ? null : BuiltInRegistries.SOUND_EVENT.getOptional(loc).orElse(null);
        if (event == null) RealmKeys.LOGGER.warn("Unknown sound id in realm_keys_settings.json: {}", id);
        return event;
    }

    private static ParticleOptions resolveParticle(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        ParticleType<?> type = loc == null ? null : BuiltInRegistries.PARTICLE_TYPE.getOptional(loc).orElse(null);
        if (type instanceof ParticleOptions options) return options;
        RealmKeys.LOGGER.warn("Unknown or unsupported particle id in realm_keys_settings.json: {}", id);
        return null;
    }
}
