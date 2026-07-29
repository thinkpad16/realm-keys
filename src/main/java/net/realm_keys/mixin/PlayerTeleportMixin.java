package net.realm_keys.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
    private void onChangeDimension(DimensionTransition transition, CallbackInfoReturnable<Entity> cir) {
        if (transition == null || transition.newLevel() == null) return;

        ServerPlayer player = (ServerPlayer) (Object) this;
        String targetDimId = transition.newLevel().dimension().location().toString();
        String currentDimId = player.serverLevel().dimension().location().toString();

        if (!currentDimId.equals(targetDimId)) {
            if (!RealmKeysConfig.hasAccess(player.getUUID(), targetDimId)) {
                player.displayClientMessage(Component.literal("Шлях до " + targetDimId + " заблоковано!").withStyle(ChatFormatting.RED), true);
                cir.setReturnValue(null);
            }
        }
    }
}