package net.realm_keys.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.realm_keys.config.RealmKeysConfig;

import java.util.Collection;

public class RealmKeysCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("realmkeys")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("global")
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("dimension", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(context.getSource().getServer().levelKeys().stream().map(key -> key.location()), builder))
                                        .executes(context -> {
                                            ResourceLocation dimId = ResourceLocationArgument.getId(context, "dimension");
                                            if (RealmKeysConfig.unlockGlobal(dimId.toString())) {
                                                context.getSource().sendSuccess(() -> Component.literal("Глобально відкрито: " + dimId).withStyle(ChatFormatting.GREEN), true);
                                                context.getSource().getServer().getPlayerList().getPlayers().forEach(p -> playUnlockEffects(p, dimId));
                                            } else {
                                                context.getSource().sendFailure(Component.literal("Вимір " + dimId + " вже був глобально відкритий."));
                                            }
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("lock")
                                .then(Commands.argument("dimension", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(context.getSource().getServer().levelKeys().stream().map(key -> key.location()), builder))
                                        .executes(context -> {
                                            ResourceLocation dimId = ResourceLocationArgument.getId(context, "dimension");
                                            if (RealmKeysConfig.lockGlobal(dimId.toString())) {
                                                context.getSource().sendSuccess(() -> Component.literal("Глобально закрито: " + dimId).withStyle(ChatFormatting.RED), true);
                                            } else {
                                                context.getSource().sendFailure(Component.literal("Вимір " + dimId + " вже був глобально закритий."));
                                            }
                                            return 1;
                                        })
                                )
                        )
                )

                .then(Commands.literal("player")
                        .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                .then(Commands.literal("unlock")
                                        .then(Commands.argument("dimension", ResourceLocationArgument.id())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(context.getSource().getServer().levelKeys().stream().map(key -> key.location()), builder))
                                                .executes(context -> {
                                                    Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "targets");
                                                    ResourceLocation dimId = ResourceLocationArgument.getId(context, "dimension");

                                                    for (GameProfile profile : profiles) {
                                                        if (RealmKeysConfig.unlockForPlayer(profile.getId(), dimId.toString())) {
                                                            context.getSource().sendSuccess(() -> Component.literal("Відкрито " + dimId + " для " + profile.getName()).withStyle(ChatFormatting.GREEN), true);

                                                            ServerPlayer onlinePlayer = context.getSource().getServer().getPlayerList().getPlayer(profile.getId());
                                                            if (onlinePlayer != null) {
                                                                playUnlockEffects(onlinePlayer, dimId);
                                                            }
                                                        }
                                                    }
                                                    return profiles.size();
                                                })
                                        )
                                )
                                .then(Commands.literal("lock")
                                        .then(Commands.argument("dimension", ResourceLocationArgument.id())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(context.getSource().getServer().levelKeys().stream().map(key -> key.location()), builder))
                                                .executes(context -> {
                                                    Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "targets");
                                                    ResourceLocation dimId = ResourceLocationArgument.getId(context, "dimension");

                                                    for (GameProfile profile : profiles) {
                                                        if (RealmKeysConfig.lockForPlayer(profile.getId(), dimId.toString())) {
                                                            context.getSource().sendSuccess(() -> Component.literal("Закрито " + dimId + " для " + profile.getName()).withStyle(ChatFormatting.RED), true);
                                                        }
                                                    }
                                                    return profiles.size();
                                                })
                                        )
                                )
                        )
                )

                .then(Commands.literal("chunk")
                        .then(Commands.literal("unlock")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String dimId = player.serverLevel().dimension().location().toString();
                                    int cX = player.chunkPosition().x;
                                    int cZ = player.chunkPosition().z;

                                    if (RealmKeysConfig.unlockChunk(dimId, cX, cZ)) {
                                        context.getSource().sendSuccess(() -> Component.literal(String.format("Чанк [%d, %d] у %s відкрито як публічний портал!", cX, cZ, dimId)).withStyle(ChatFormatting.GREEN), true);
                                    } else {
                                        context.getSource().sendFailure(Component.literal("Цей чанк вже є публічною зоною."));
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("lock")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String dimId = player.serverLevel().dimension().location().toString();
                                    int cX = player.chunkPosition().x;
                                    int cZ = player.chunkPosition().z;

                                    if (RealmKeysConfig.lockChunk(dimId, cX, cZ)) {
                                        context.getSource().sendSuccess(() -> Component.literal(String.format("Чанк [%d, %d] у %s закрито. Тепер діють звичайні правила.", cX, cZ, dimId)).withStyle(ChatFormatting.RED), true);
                                    } else {
                                        context.getSource().sendFailure(Component.literal("Цей чанк не був публічною зоною."));
                                    }
                                    return 1;
                                })
                        )
                )

                .then(Commands.literal("spawn")
                        .then(Commands.literal("set")
                                .then(Commands.argument("dimension", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(context.getSource().getServer().levelKeys().stream().map(key -> key.location()), builder))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            ResourceLocation dimId = ResourceLocationArgument.getId(context, "dimension");
                                            String coords = player.getX() + ", " + player.getY() + ", " + player.getZ();
                                            RealmKeysConfig.getInstance().customSpawns.put(dimId.toString(), coords);
                                            RealmKeysConfig.save();
                                            context.getSource().sendSuccess(() -> Component.literal("Safe spawn set to " + coords + " for " + dimId).withStyle(ChatFormatting.GREEN), true);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("clear")
                                .then(Commands.argument("dimension", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(context.getSource().getServer().levelKeys().stream().map(key -> key.location()), builder))
                                        .executes(context -> {
                                            ResourceLocation dimId = ResourceLocationArgument.getId(context, "dimension");
                                            RealmKeysConfig.getInstance().customSpawns.remove(dimId.toString());
                                            RealmKeysConfig.save();
                                            context.getSource().sendSuccess(() -> Component.literal("Safe spawn cleared for " + dimId).withStyle(ChatFormatting.GREEN), true);
                                            return 1;
                                        })
                                )
                        )
                )

                .then(Commands.literal("mode")
                        .then(Commands.literal("global")
                                .executes(context -> {
                                    RealmKeysConfig.getInstance().perPlayerProgression = false;
                                    RealmKeysConfig.save();
                                    context.getSource().sendSuccess(() -> Component.literal("Режим змінено: Глобальна прогресія").withStyle(ChatFormatting.YELLOW), true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("per_player")
                                .executes(context -> {
                                    RealmKeysConfig.getInstance().perPlayerProgression = true;
                                    RealmKeysConfig.save();
                                    context.getSource().sendSuccess(() -> Component.literal("Режим змінено: Індивідуальна прогресія").withStyle(ChatFormatting.YELLOW), true);
                                    return 1;
                                })
                        )
                )

                .then(Commands.literal("reload")
                        .executes(context -> {
                            RealmKeysConfig.load();
                            context.getSource().sendSuccess(() -> Component.literal("Конфіг Realm Keys перезавантажено!").withStyle(ChatFormatting.AQUA), true);
                            return 1;
                        })
                )
        );
    }

    private static void playUnlockEffects(ServerPlayer player, ResourceLocation dimId) {
        RealmKeysConfig config = RealmKeysConfig.getInstance();
        if (!config.enableUnlockEffects) return;

        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("Шлях Відкрито!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        ));

        String subtitleText = config.welcomeTitles.getOrDefault(dimId.toString(), dimId.toString()).replace("&", "§");

        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal(subtitleText)
        ));

        player.playNotifySound(SoundEvents.END_PORTAL_SPAWN, SoundSource.MASTER, 1.0f, 1.0f);
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
    }
}