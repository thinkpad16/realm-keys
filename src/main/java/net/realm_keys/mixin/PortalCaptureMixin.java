package net.realm_keys.mixin;

import net.minecraft.world.entity.Entity;
import net.realm_keys.util.PortalTeleportContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class PortalCaptureMixin {

    // Відловлюємо телепортацію через стандартну систему порталів 1.21.1 (Незер, Енд та модові)
    @Inject(method = "handlePortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;"))
    private void onHandlePortalStart(CallbackInfo ci) {
        PortalTeleportContext.setPortalTeleport(true);
    }

    @Inject(method = "handlePortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;", shift = At.Shift.AFTER))
    private void onHandlePortalEnd(CallbackInfo ci) {
        PortalTeleportContext.setPortalTeleport(false);
    }
}