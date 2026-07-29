package net.realm_keys;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.ResourceLocation;
import net.realm_keys.command.RealmKeysCommand;
import net.realm_keys.config.RealmKeysConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmKeys implements ModInitializer {
    public static final String MOD_ID = "realm-keys";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Realm Keys!");

        RealmKeysConfig.load();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RealmKeysCommand.register(dispatcher);
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}