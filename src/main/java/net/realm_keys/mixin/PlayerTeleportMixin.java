package net.realm_keys.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.realm_keys.config.RealmKeysConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class PlayerTeleportMixin {

    @Inject(method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;", at = @At("HEAD"), cancellable = true)
    private void onBeforeChangeDimension(DimensionTransition transition, CallbackInfoReturnable<Entity> cir) {
        if (transition == null || transition.newLevel() == null) return;

        ServerPlayer player = (ServerPlayer) (Object) this;
        String targetDimId = transition.newLevel().dimension().location().toString();
        String currentDimId = player.serverLevel().dimension().location().toString();
        int currentChunkX = player.chunkPosition().x;
        int currentChunkZ = player.chunkPosition().z;

        if (!currentDimId.equals(targetDimId)) {
            if (RealmKeysConfig.isChunkUnlocked(currentDimId, currentChunkX, currentChunkZ)) return;

            if (!RealmKeysConfig.hasAccess(player.getUUID(), targetDimId)) {
                RealmKeysConfig config = RealmKeysConfig.getInstance();

                if (config.enableBlockMessage) {
                    String msg = config.lockedMessages.getOrDefault(targetDimId, "&cШлях до &e" + targetDimId + " &cзаблоковано!");
                    msg = msg.replace("&", "§");
                    player.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.BOLD), true);
                }

                if (config.enableBlockEffects) {
                    player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 0.5f);
                    player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 0.5f);
                    player.knockback(1.0, player.getLookAngle().x, player.getLookAngle().z);
                    player.hurtMarked = true;
                    player.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
                    player.serverLevel().sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
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
            String targetDimId = newPlayer.serverLevel().dimension().location().toString();
            String playerName = newPlayer.getName().getString();

            if (config.enableFirstDiscovererBroadcast) {
                if (!config.firstDiscoverers.containsKey(targetDimId)) {

                    config.firstDiscoverers.put(targetDimId, playerName);
                    RealmKeysConfig.save();

                    String dimName = config.welcomeTitles.getOrDefault(targetDimId, targetDimId);

                    String broadcastMsg = config.firstDiscovererMessage
                            .replace("%player%", playerName)
                            .replace("%dimension%", dimName)
                            .replace("&", "§");

                    newPlayer.serverLevel().getServer().getPlayerList().broadcastSystemMessage(
                            Component.literal(broadcastMsg), false
                    );

                    newPlayer.serverLevel().getServer().getPlayerList().getPlayers().forEach(p -> {
                        p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0f, 1.0f);
                    });
                }
            }

            if (config.enableWelcomeTitles) {
                if (config.welcomeTitles.containsKey(targetDimId)) {
                    String titleText = config.welcomeTitles.get(targetDimId).replace("&", "§");
                    newPlayer.connection.send(new ClientboundSetTitleTextPacket(
                            Component.literal(titleText)
                    ));
                }
            }
        }
    }
}