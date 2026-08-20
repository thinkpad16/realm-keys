package net.realm_keys.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.realm_keys.config.RealmKeysConfig;

import java.util.Set;

/**
 * Builds player-facing messages. Falls back to built-in, per-client-translated
 * lang keys unless a server admin has configured an explicit override for that dimension.
 */
public final class RealmKeysText {
    private static final Set<String> BUILTIN_DIMENSIONS = Set.of("minecraft:the_nether", "minecraft:the_end");

    private RealmKeysText() {}

    private static String dimensionKey(String dimensionId, String suffix) {
        int colon = dimensionId.indexOf(':');
        String namespace = colon >= 0 ? dimensionId.substring(0, colon) : "minecraft";
        String path = colon >= 0 ? dimensionId.substring(colon + 1) : dimensionId;
        return "dimension.realm_keys." + namespace + "." + path + "." + suffix;
    }

    public static MutableComponent welcomeTitle(String dimensionId) {
        RealmKeysConfig config = RealmKeysConfig.getInstance();
        String override = config.welcomeTitles.get(dimensionId);
        if (override != null && !override.isEmpty()) {
            return Component.literal(override.replace("&", "§"));
        }
        if (BUILTIN_DIMENSIONS.contains(dimensionId)) {
            return Component.translatableWithFallback(dimensionKey(dimensionId, "welcome"), dimensionId);
        }
        if (config.defaultWelcomeTitle != null && !config.defaultWelcomeTitle.isEmpty()) {
            return Component.literal(formatTemplate(config.defaultWelcomeTitle, dimensionId));
        }
        return null;
    }

    public static MutableComponent lockedMessage(String dimensionId) {
        RealmKeysConfig config = RealmKeysConfig.getInstance();
        String override = config.lockedMessages.get(dimensionId);
        if (override != null && !override.isEmpty()) {
            return Component.literal(override.replace("&", "§"));
        }
        if (BUILTIN_DIMENSIONS.contains(dimensionId)) {
            return Component.translatableWithFallback(dimensionKey(dimensionId, "locked"), "Access to %s is locked!", dimensionId);
        }
        if (config.defaultLockedMessage != null && !config.defaultLockedMessage.isEmpty()) {
            return Component.literal(formatTemplate(config.defaultLockedMessage, dimensionId));
        }
        return Component.translatableWithFallback("message.realm_keys.locked.generic", "Access to %s is locked!", dimensionId);
    }

    private static String formatTemplate(String template, String dimensionId) {
        return template.replace("%dimension%", dimensionId).replace("&", "§");
    }

    public static Component firstDiscoverer(String playerName, String dimensionId) {
        RealmKeysConfig config = RealmKeysConfig.getInstance();
        Component dimensionDisplay = displayName(dimensionId);

        if (config.firstDiscovererMessage != null && !config.firstDiscovererMessage.isEmpty()) {
            String text = config.firstDiscovererMessage
                    .replace("%player%", playerName)
                    .replace("%dimension%", dimensionDisplay.getString())
                    .replace("&", "§");
            return Component.literal(text);
        }

        return Component.translatableWithFallback("message.realm_keys.first_discoverer",
                "[!] %s was the first to unseal the gate to %s!", playerName, dimensionDisplay);
    }

    /** The best available text for a dimension: admin override, built-in welcome title, or the raw id. */
    public static Component displayName(String dimensionId) {
        MutableComponent title = welcomeTitle(dimensionId);
        return title != null ? title : Component.literal(dimensionId);
    }
}
