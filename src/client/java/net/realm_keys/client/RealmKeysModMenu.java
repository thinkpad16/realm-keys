package net.realm_keys.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.realm_keys.config.RealmKeysConfig;

import java.util.List;

public class RealmKeysModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            RealmKeysConfig config = RealmKeysConfig.getInstance();

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.translatable("config.realm_keys.title"))
                    .setSavingRunnable(RealmKeysConfig::save);

            ConfigEntryBuilder entry = builder.entryBuilder();
            ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.realm_keys.category.general"));

            general.addEntry(entry.startBooleanToggle(Component.translatable("config.realm_keys.per_player_progression"), config.perPlayerProgression)
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.realm_keys.per_player_progression.tooltip"))
                    .setSaveConsumer(value -> config.perPlayerProgression = value)
                    .build());

            general.addEntry(entry.startBooleanToggle(Component.translatable("config.realm_keys.enable_unlock_effects"), config.enableUnlockEffects)
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.realm_keys.enable_unlock_effects.tooltip"))
                    .setSaveConsumer(value -> config.enableUnlockEffects = value)
                    .build());

            general.addEntry(entry.startBooleanToggle(Component.translatable("config.realm_keys.enable_block_effects"), config.enableBlockEffects)
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.realm_keys.enable_block_effects.tooltip"))
                    .setSaveConsumer(value -> config.enableBlockEffects = value)
                    .build());

            general.addEntry(entry.startBooleanToggle(Component.translatable("config.realm_keys.enable_block_message"), config.enableBlockMessage)
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> config.enableBlockMessage = value)
                    .build());

            general.addEntry(entry.startBooleanToggle(Component.translatable("config.realm_keys.enable_welcome_titles"), config.enableWelcomeTitles)
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> config.enableWelcomeTitles = value)
                    .build());

            general.addEntry(entry.startBooleanToggle(Component.translatable("config.realm_keys.enable_first_discoverer_broadcast"), config.enableFirstDiscovererBroadcast)
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> config.enableFirstDiscovererBroadcast = value)
                    .build());

            general.addEntry(entry.startBooleanToggle(Component.translatable("config.realm_keys.enable_custom_spawns"), config.enableCustomSpawns)
                    .setDefaultValue(true)
                    .setSaveConsumer(value -> config.enableCustomSpawns = value)
                    .build());

            general.addEntry(entry.startStrField(Component.translatable("config.realm_keys.first_discoverer_message"), config.firstDiscovererMessage)
                    .setDefaultValue("")
                    .setTooltip(Component.translatable("config.realm_keys.first_discoverer_message.tooltip"))
                    .setSaveConsumer(value -> config.firstDiscovererMessage = value)
                    .build());

            general.addEntry(entry.startStrField(Component.translatable("config.realm_keys.default_welcome_title"), config.defaultWelcomeTitle)
                    .setDefaultValue("")
                    .setTooltip(Component.translatable("config.realm_keys.default_welcome_title.tooltip"))
                    .setSaveConsumer(value -> config.defaultWelcomeTitle = value)
                    .build());

            general.addEntry(entry.startStrField(Component.translatable("config.realm_keys.default_locked_message"), config.defaultLockedMessage)
                    .setDefaultValue("")
                    .setTooltip(Component.translatable("config.realm_keys.default_locked_message.tooltip"))
                    .setSaveConsumer(value -> config.defaultLockedMessage = value)
                    .build());

            general.addEntry(entry.startStrList(Component.translatable("config.realm_keys.exempt_dimensions"), config.exemptDimensions)
                    .setDefaultValue(List.of("minecraft:overworld"))
                    .setTooltip(Component.translatable("config.realm_keys.exempt_dimensions.tooltip"))
                    .setSaveConsumer(value -> config.exemptDimensions = value)
                    .build());

            general.addEntry(entry.startIntField(Component.translatable("config.realm_keys.command_permission_level"), config.commandPermissionLevel)
                    .setDefaultValue(2)
                    .setMin(0).setMax(4)
                    .setTooltip(Component.translatable("config.realm_keys.command_permission_level.tooltip"))
                    .setSaveConsumer(value -> config.commandPermissionLevel = value)
                    .build());

            general.addEntry(entry.startDoubleField(Component.translatable("config.realm_keys.block_knockback_strength"), config.blockKnockbackStrength)
                    .setDefaultValue(1.0)
                    .setMin(0.0)
                    .setTooltip(Component.translatable("config.realm_keys.block_knockback_strength.tooltip"))
                    .setSaveConsumer(value -> config.blockKnockbackStrength = value)
                    .build());

            ConfigCategory effects = builder.getOrCreateCategory(Component.translatable("config.realm_keys.category.effects"));
            effects.addEntry(entry.startTextDescription(Component.translatable("config.realm_keys.effects.editor_hint")).build());

            return builder.build();
        };
    }
}
