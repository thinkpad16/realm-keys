package net.realm_keys.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.realm_keys.config.RealmKeysConfig;
import net.realm_keys.data.RealmKeysData;
import net.realm_keys.util.RealmKeysEffects;
import net.realm_keys.util.RealmKeysText;

import java.util.Collection;
import java.util.List;

public class RealmKeysCommand {

    private static boolean hasAdminPermission(CommandSourceStack source) {
        return source.hasPermission(RealmKeysConfig.getInstance().commandPermissionLevel);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("realmkeys")

                .then(Commands.literal("global")
                        .then(Commands.literal("unlock")
                                .requires(RealmKeysCommand::hasAdminPermission)
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(context -> {
                                            ResourceLocation dimId = DimensionArgument.getDimension(context, "dimension").dimension().location();
                                            MinecraftServer server = context.getSource().getServer();
                                            RealmKeysData data = RealmKeysData.getServerState(server);

                                            if (data.unlockGlobal(dimId.toString())) {
                                                RealmKeysData.flush(server);
                                                context.getSource().sendSuccess(() -> Component.translatable("command.realm_keys.global.unlock.success", dimId).withStyle(ChatFormatting.GREEN), true);
                                                server.getPlayerList().getPlayers().forEach(p -> playUnlockEffects(p, RealmKeysText.displayName(dimId.toString())));
                                            } else {
                                                context.getSource().sendFailure(Component.translatable("command.realm_keys.global.unlock.fail", dimId));
                                            }
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("lock")
                                .requires(RealmKeysCommand::hasAdminPermission)
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(context -> {
                                            ResourceLocation dimId = DimensionArgument.getDimension(context, "dimension").dimension().location();
                                            MinecraftServer server = context.getSource().getServer();
                                            RealmKeysData data = RealmKeysData.getServerState(server);

                                            if (data.lockGlobal(dimId.toString())) {
                                                RealmKeysData.flush(server);
                                                context.getSource().sendSuccess(() -> Component.translatable("command.realm_keys.global.lock.success", dimId).withStyle(ChatFormatting.RED), true);
                                            } else {
                                                context.getSource().sendFailure(Component.translatable("command.realm_keys.global.lock.fail", dimId));
                                            }
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("unlockall")
                                .requires(RealmKeysCommand::hasAdminPermission)
                                .executes(context -> {
                                    MinecraftServer server = context.getSource().getServer();
                                    RealmKeysData data = RealmKeysData.getServerState(server);
                                    List<String> allDimensions = server.levelKeys().stream().map(key -> key.location().toString()).toList();

                                    int unlocked = data.unlockAllGlobal(allDimensions);
                                    if (unlocked > 0) {
                                        RealmKeysData.flush(server);
                                        context.getSource().sendSuccess(() -> Component.translatable("command.realm_keys.global.unlockall.success", unlocked).withStyle(ChatFormatting.GREEN), true);
                                        server.getPlayerList().getPlayers().forEach(RealmKeysCommand::playUnlockAllEffects);
                                    } else {
                                        context.getSource().sendFailure(Component.translatable("command.realm_keys.global.unlockall.fail"));
                                    }
                                    return unlocked;
                                })
                        )
                        .then(Commands.literal("lockall")
                                .requires(RealmKeysCommand::hasAdminPermission)
                                .executes(context -> {
                                    MinecraftServer server = context.getSource().getServer();
                                    RealmKeysData data = RealmKeysData.getServerState(server);
                                    List<String> allDimensions = server.levelKeys().stream().map(key -> key.location().toString()).toList();

                                    int locked = data.lockAllGlobal(allDimensions);
                                    if (locked > 0) {
                                        RealmKeysData.flush(server);
                                        context.getSource().sendSuccess(() -> Component.translatable("command.realm_keys.global.lockall.success", locked).withStyle(ChatFormatting.RED), true);
                                    } else {
                                        context.getSource().sendFailure(Component.translatable("command.realm_keys.global.lockall.fail"));
                                    }
                                    return locked;
                                })
                        )
                )

                .then(Commands.literal("player")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.literal("unlock")
                                        .requires(RealmKeysCommand::hasAdminPermission)
                                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                .executes(context -> {
                                                    Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
                                                    ResourceLocation dimId = DimensionArgument.getDimension(context, "dimension").dimension().location();
                                                    MinecraftServer server = context.getSource().getServer();
                                                    RealmKeysData data = RealmKeysData.getServerState(server);

                                                    boolean changed = false;
                                                    for (Entity entity : targets) {
                                                        if (data.unlockForPlayer(entity.getUUID(), dimId.toString())) {
                                                            changed = true;
                                                            context.getSource().sendSuccess(() -> Component.translatable("command.realm_keys.player.unlock.success", dimId, entity.getDisplayName()).withStyle(ChatFormatting.GREEN), true);
                                                            if (entity instanceof ServerPlayer onlinePlayer) playUnlockEffects(onlinePlayer, RealmKeysText.displayName(dimId.toString()));
                                                        }
                                                    }
                                                    if (changed) RealmKeysData.flush(server);
                                                    return targets.size();
                                                })
                                        )
                                )
                                .then(Commands.literal("lock")
                                        .requires(RealmKeysCommand::hasAdminPermission)
                                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                .executes(context -> {
                                                    Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
                                                    ResourceLocation dimId = DimensionArgument.getDimension(context, "dimension").dimension().location();
                                                    MinecraftServer server = context.getSource().getServer();
                                                    RealmKeysData data = RealmKeysData.getServerState(server);

                                                    boolean changed = false;
                                                    for (Entity entity : targets) {
                                                        if (data.lockForPlayer(entity.getUUID(), dimId.toString())) {
                                                            changed = true;
                                                            context.getSource().sendSuccess(() -> Component.translatable("command.realm_keys.player.lock.success", dimId, entity.getDisplayName()).withStyle(ChatFormatting.RED), true);
                                                        }
                                                    }
                                                    if (changed) RealmKeysData.flush(server);
                                                    return targets.size();
                                                })
                                        )
                                )
                                .then(Commands.literal("unlockall")
                                        .requires(RealmKeysCommand::hasAdminPermission)
                                        .executes(context -> {
                                            Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
                                            MinecraftServer server = context.getSource().getServer();
                                            RealmKeysData data = RealmKeysData.getServerState(server);
                                            List<String> allDimensions = server.levelKeys().stream().map(key -> key.location().toString()).toList();

                                            boolean changed = false;
                                            for (Entity entity : targets) {
                                                int unlocked = data.unlockAllForPlayer(entity.getUUID(), allDimensions);
                                                if (unlocked > 0) {
                                                    changed = true;
                                                    context.getSource().sendSuccess(() -> Component.translatable("command.realm_keys.player.unlockall.success", unlocked, entity.getDisplayName()).withStyle(ChatFormatting.GREEN), true);
                                                    if (entity instanceof ServerPlayer onlinePlayer) playUnlockAllEffects(onlinePlayer);
                                                } else {
                                                    context.getSource().sendFailure(Component.translatable("command.realm_keys.player.unlockall.fail", entity.getDisplayName()));
                                                }
                                            }
                                            if (changed) RealmKeysData.flush(server);
                                            return targets.size();
                                        })
                                )
                                .then(Commands.literal("lockall")
                                        .requires(RealmKeysCommand::hasAdminPermission)
                                        .executes(context -> {
                                            Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");
                                            MinecraftServer server = context.getSource().getServer();
                                            RealmKeysData data = RealmKeysData.getServerState(server);
                                            List<String> allDimensions = server.levelKeys().stream().map(key -> key.location().toString()).toList();

                                            boolean changed = false;
                                            for (Entity entity : targets) {
                                                int locked = data.lockAllForPlayer(entity.getUUID(), allDimensions);
                                                if (locked > 0) {
                                                    changed = true;
                                                    context.getSource().sendSuccess(() -> Component.translatable("command.realm_keys.player.lockall.success", locked, entity.getDisplayName()).withStyle(ChatFormatting.RED), true);
                                                } else {
                                                    context.getSource().sendFailure(Component.translatable("command.realm_keys.player.lockall.fail", entity.getDisplayName()));
                                                }
                                            }
                                            if (changed) RealmKeysData.flush(server);
                                            return targets.size();
                                        })
                                )
                        )
                )

                .then(Commands.literal("chunk")
                        .then(Commands.literal("unlock")
                                .requires(RealmKeysCommand::hasAdminPermission)
                                .executes(context -> {
                                    Entity entity = context.getSource().getEntityOrException();
                                    String dimId = entity.level().dimension().location().toString();
                                    int cX = entity.chunkPosition().x;
                                    int cZ = entity.chunkPosition().z;
                                    MinecraftServer server = context.getSource().getServer();
                                    RealmKeysData data = RealmKeysData.getServerState(server);

                                    if (data.unlockChunk(dimId, cX, cZ)) {
                                        RealmKeysData.flush(server);
                                        context.getSource().sendSuccess(() -> Component.translatable("command.realm_keys.chunk.unlock.success", cX, cZ, dimId).withStyle(ChatFormatting.GREEN), true);
                                    } else {
                                        context.getSource().sendFailure(Component.translatable("command.realm_keys.chunk.unlock.fail"));
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("lock")
                                .requires(RealmKeysCommand::hasAdminPermission)
                                .executes(context -> {
                                    Entity entity = context.getSource().getEntityOrException();
                                    String dimId = entity.level().dimension().location().toString();
                                    int cX = entity.chunkPosition().x;
                                    int cZ = entity.chunkPosition().z;
                                    MinecraftServer server = context.getSource().getServer();
                                    RealmKeysData data = RealmKeysData.getServerState(server);

                                    if (data.lockChunk(dimId, cX, cZ)) {
                                        RealmKeysData.flush(server);
                                        context.getSource().sendSuccess(() -> Component.translatable("command.realm_keys.chunk.lock.success", cX, cZ, dimId).withStyle(ChatFormatting.RED), true);
                                    } else {
                                        context.getSource().sendFailure(Component.translatable("command.realm_keys.chunk.lock.fail"));
                                    }
                                    return 1;
                                })
                        )
                )

                .then(Commands.literal("spawn")
                        .then(Commands.literal("set")
                                .requires(RealmKeysCommand::hasAdminPermission)
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(context -> {
                                            Entity entity = context.getSource().getEntityOrException();
                                            ResourceLocation dimId = DimensionArgument.getDimension(context, "dimension").dimension().location();
                                            String coords = entity.getX() + "," + entity.getY() + "," + entity.getZ();
                                            MinecraftServer server = context.getSource().getServer();
                                            RealmKeysData data = RealmKeysData.getServerState(server);

                                            data.customSpawns.put(dimId.toString(), coords);
                                            data.setDirty();
                                            RealmKeysData.flush(server);

                                            context.getSource().sendSuccess(() -> Component.translatable("command.realm_keys.spawn.set.success", coords, dimId).withStyle(ChatFormatting.GREEN), true);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("clear")
                                .requires(RealmKeysCommand::hasAdminPermission)
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(context -> {
                                            ResourceLocation dimId = DimensionArgument.getDimension(context, "dimension").dimension().location();
                                            MinecraftServer server = context.getSource().getServer();
                                            RealmKeysData data = RealmKeysData.getServerState(server);

                                            data.customSpawns.remove(dimId.toString());
                                            data.setDirty();
                                            RealmKeysData.flush(server);

                                            context.getSource().sendSuccess(() -> Component.translatable("command.realm_keys.spawn.clear.success", dimId).withStyle(ChatFormatting.GREEN), true);
                                            return 1;
                                        })
                                )
                        )
                )

                .then(Commands.literal("mode")
                        .requires(RealmKeysCommand::hasAdminPermission)
                        .then(Commands.literal("global")
                                .executes(context -> {
                                    RealmKeysConfig.getInstance().perPlayerProgression = false;
                                    RealmKeysConfig.save();
                                    context.getSource().sendSuccess(() -> Component.translatable("command.realm_keys.mode.global").withStyle(ChatFormatting.YELLOW), true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("per_player")
                                .executes(context -> {
                                    RealmKeysConfig.getInstance().perPlayerProgression = true;
                                    RealmKeysConfig.save();
                                    context.getSource().sendSuccess(() -> Component.translatable("command.realm_keys.mode.per_player").withStyle(ChatFormatting.YELLOW), true);
                                    return 1;
                                })
                        )
                )

                .then(Commands.literal("reload")
                        .requires(RealmKeysCommand::hasAdminPermission)
                        .executes(context -> {
                            RealmKeysConfig.load();
                            context.getSource().sendSuccess(() -> Component.translatable("command.realm_keys.reload.success").withStyle(ChatFormatting.AQUA), true);
                            return 1;
                        })
                )
        );
    }

    private static void playUnlockEffects(ServerPlayer player, Component subtitle) {
        RealmKeysConfig config = RealmKeysConfig.getInstance();
        if (!config.enableUnlockEffects) return;

        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("message.realm_keys.path_opened").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        ));

        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));

        RealmKeysEffects.playNotifySounds(player, config.unlockSounds);
    }

    private static void playUnlockAllEffects(ServerPlayer player) {
        playUnlockEffects(player, Component.translatable("message.realm_keys.all_realms_opened").withStyle(ChatFormatting.YELLOW));
    }
}
