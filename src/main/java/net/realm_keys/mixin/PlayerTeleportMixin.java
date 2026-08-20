package net.realm_keys.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.realm_keys.config.RealmKeysConfig;
import net.realm_keys.data.RealmKeysData;
import net.realm_keys.util.PortalTeleportContext;
import net.realm_keys.util.RealmKeysEffects;
import net.realm_keys.util.RealmKeysText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class PlayerTeleportMixin {

    @ModifyVariable(method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;", at = @At("HEAD"), argsOnly = true)
    private DimensionTransition modifyDimensionTransition(DimensionTransition transition) {
        if (transition == null || transition.newLevel() == null) return transition;

        RealmKeysConfig config = RealmKeysConfig.getInstance();
        if (!config.enableCustomSpawns || !PortalTeleportContext.isPortalTeleport()) return transition;

        ServerPlayer player = (ServerPlayer) (Object) this;
        RealmKeysData data = RealmKeysData.getServerState(player.serverLevel().getServer());
        String targetDimId = transition.newLevel().dimension().location().toString();

        if (data.customSpawns.containsKey(targetDimId)) {
            String[] coords = data.customSpawns.get(targetDimId).split(",");
            if (coords.length >= 3) {
                try {
                    double x = Double.parseDouble(coords[0].trim());
                    double y = Double.parseDouble(coords[1].trim());
                    double z = Double.parseDouble(coords[2].trim());

                    return new DimensionTransition(
                            transition.newLevel(),
                            new Vec3(x, y, z),
                            Vec3.ZERO,
                            transition.yRot(),
                            transition.xRot(),
                            transition.postDimensionTransition()
                    );
                } catch (NumberFormatException ignored) {}
            }
        }
        return transition;
    }

    @Inject(method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;", at = @At("HEAD"), cancellable = true)
    private void onBeforeChangeDimension(DimensionTransition transition, CallbackInfoReturnable<Entity> cir) {
        if (transition == null || transition.newLevel() == null) return;

        ServerPlayer player = (ServerPlayer) (Object) this;
        String targetDimId = transition.newLevel().dimension().location().toString();
        String currentDimId = player.serverLevel().dimension().location().toString();
        int currentChunkX = player.chunkPosition().x;
        int currentChunkZ = player.chunkPosition().z;

        RealmKeysData data = RealmKeysData.getServerState(player.serverLevel().getServer());

        if (!currentDimId.equals(targetDimId)) {
            if (data.isChunkUnlocked(currentDimId, currentChunkX, currentChunkZ)) return;

            if (!data.hasAccess(player.getUUID(), targetDimId)) {
                RealmKeysConfig config = RealmKeysConfig.getInstance();

                if (config.enableBlockMessage) {
                    player.displayClientMessage(RealmKeysText.lockedMessage(targetDimId).withStyle(ChatFormatting.BOLD), true);
                }

                if (config.enableBlockEffects) {
                    RealmKeysEffects.playWorldSounds(player, config.blockSounds);
                    player.knockback(config.blockKnockbackStrength, player.getLookAngle().x, player.getLookAngle().z);
                    player.hurtMarked = true;
                    RealmKeysEffects.spawnParticles(player, config.blockParticles);
                }
                cir.setReturnValue(null);
            }
        }
    }

    @Inject(method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;", at = @At("RETURN"))
    private void onAfterChangeDimension(DimensionTransition transition, CallbackInfoReturnable<Entity> cir) {
        Entity newEntity = cir.getReturnValue();

        if (newEntity instanceof ServerPlayer newPlayer) {
            RealmKeysConfig config = RealmKeysConfig.getInstance();
            RealmKeysData data = RealmKeysData.getServerState(newPlayer.serverLevel().getServer());
            String targetDimId = newPlayer.serverLevel().dimension().location().toString();
            String playerName = newPlayer.getName().getString();

            if (config.enableFirstDiscovererBroadcast) {
                if (!data.firstDiscoverers.containsKey(targetDimId)) {
                    data.firstDiscoverers.put(targetDimId, playerName);
                    data.setDirty();
                    RealmKeysData.flush(newPlayer.serverLevel().getServer()); // Зберігаємо нове відкриття одразу

                    Component broadcastMsg = RealmKeysText.firstDiscoverer(playerName, targetDimId);
                    newPlayer.serverLevel().getServer().getPlayerList().broadcastSystemMessage(broadcastMsg, false);
                    RealmKeysEffects.broadcastNotifySounds(newPlayer.serverLevel().getServer(), config.discovererSounds);
                }
            }

            if (config.enableWelcomeTitles) {
                Component title = RealmKeysText.welcomeTitle(targetDimId);
                if (title != null) {
                    newPlayer.connection.send(new ClientboundSetTitleTextPacket(title));
                }
            }
        }
    }
}