package net.ditto.mixin.client;

import net.ditto.networking.MagiaPackets;
import net.ditto.util.IPlayerCombat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        // Ensure player exists and is in Combat Mode
        if (client.player != null && ((IPlayerCombat)client.player).isInCombatMode()) {

            // Action 1 is "Press" (we don't want to trigger on release)
            if (action == 1) {
                // Loop through the 9 hotbar keys (standard Minecraft options)
                for (int i = 0; i < 9; i++) {
                    if (client.options.hotbarKeys[i].matchesKey(key, scancode)) {

                        // If it's one of the first 5 slots (0-4), trigger the skill
                        if (i < 5) {
                            PacketByteBuf buf = PacketByteBufs.create();
                            buf.writeInt(i); // Send the slot index
                            ClientPlayNetworking.send(MagiaPackets.USE_SKILL, buf);
                        }

                        // Cancel the event so the hotbar selection doesn't move
                        ci.cancel();
                        return;
                    }
                }
            }
        }
    }
}
