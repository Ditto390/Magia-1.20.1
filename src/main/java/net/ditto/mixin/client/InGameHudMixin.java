package net.ditto.mixin.client;

import net.ditto.util.IPlayerCombat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void cancelHotbar(float tickDelta, DrawContext context, CallbackInfo ci) {
        // If the player is in Combat Mode, do NOT render the vanilla hotbar
        if (client.player instanceof IPlayerCombat && ((IPlayerCombat) client.player).isInCombatMode()) {
            ci.cancel();
        }
    }
}
