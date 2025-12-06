package net.ditto.mixin.client;

import net.ditto.race.Race;
import net.ditto.util.IPlayerRace;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ElytraFeatureRenderer.class)
public class ElytraFeatureRendererMixin {

    /**
     * Redirects the call to getEquippedStack.
     * If the entity is a Player and their race is ANGEL, we fake return an Elytra ItemStack.
     * This forces the renderer to draw the wings, even if the chest slot is empty or has armor.
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getEquippedStack(Lnet/minecraft/entity/EquipmentSlot;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack forceAngelWings(LivingEntity instance, EquipmentSlot slot) {
        // Get the actual item in the slot
        ItemStack actualStack = instance.getEquippedStack(slot);

        // If it's already an elytra, just return it (let vanilla handle it)
        if (actualStack.isOf(Items.ELYTRA)) {
            return actualStack;
        }

        // Check for Angel Race
        if (instance instanceof IPlayerRace) {
            Race race = ((IPlayerRace) instance).magia$getRace();
            if (race == Race.ANGEL) {
                // Return a fake Elytra stack to satisfy the renderer check
                return new ItemStack(Items.ELYTRA);
            }
        }

        // Default behavior
        return actualStack;
    }
}
