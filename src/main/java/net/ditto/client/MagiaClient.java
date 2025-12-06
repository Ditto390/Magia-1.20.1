package net.ditto.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ditto.client.gui.SkillMenuScreen;
import net.ditto.networking.MagiaPackets;
import net.ditto.skill.Skill;
import net.ditto.util.IPlayerCombat;
import net.ditto.util.IPlayerMagia;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class MagiaClient implements ClientModInitializer {

    private static KeyBinding keySkillMenu;
    private static KeyBinding keyCombatMode;

    // Vanilla widgets texture (contains hotbar, slots, etc.)
    private static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/widgets.png");

    @Override
    public void onInitializeClient() {

        // --- Keybindings ---
        keySkillMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.magia.skill_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.magia.general"
        ));

        keyCombatMode = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.magia.combat_mode",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "category.magia.general"
        ));

        // --- Client Tick (Input Handling) ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (keySkillMenu.wasPressed()) {
                client.setScreen(new SkillMenuScreen());
            }

            while (keyCombatMode.wasPressed()) {
                // Send toggle packet to server
                ClientPlayNetworking.send(MagiaPackets.TOGGLE_COMBAT, PacketByteBufs.create());
            }
        });

        // --- Networking Receivers ---

        // 1. Magia Sync
        ClientPlayNetworking.registerGlobalReceiver(MagiaPackets.SYNC_MAGIA, (client, handler, buf, responseSender) -> {
            int magia = buf.readInt();
            int maxMagia = buf.readInt();
            client.execute(() -> {
                if (client.player != null) ((IPlayerMagia) client.player).setMagia(magia);
            });
        });

        // 2. Combat Sync
        ClientPlayNetworking.registerGlobalReceiver(MagiaPackets.SYNC_COMBAT, (client, handler, buf, responseSender) -> {
            boolean isCombat = buf.readBoolean();
            Skill[] skills = new Skill[5];
            for(int i=0; i<5; i++) {
                try { skills[i] = Skill.valueOf(buf.readString()); }
                catch(Exception e) { skills[i] = Skill.NONE; }
            }

            client.execute(() -> {
                if (client.player != null) {
                    IPlayerCombat pc = (IPlayerCombat) client.player;
                    pc.setCombatMode(isCombat);
                    for(int i=0; i<5; i++) {
                        pc.setEquippedSkill(i, skills[i]);
                    }
                }
            });
        });

        // 3. Unlocked Skills Sync
        ClientPlayNetworking.registerGlobalReceiver(MagiaPackets.SYNC_UNLOCKED, (client, handler, buf, responseSender) -> {
            int count = buf.readInt();
            Skill[] unlocked = new Skill[count];
            for(int i=0; i<count; i++) {
                try { unlocked[i] = Skill.valueOf(buf.readString()); }
                catch(Exception ignored) {}
            }

            client.execute(() -> {
                if (client.player != null) {
                    IPlayerCombat pc = (IPlayerCombat) client.player;
                    // Add unlocked skills to the player's local data
                    for (Skill s : unlocked) {
                        if (s != null) pc.unlockSkill(s);
                    }
                }
            });
        });

        // --- HUD Rendering ---
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.player.isSpectator()) return;

            IPlayerCombat pc = (IPlayerCombat) client.player;

            // 1. Render Magia Bar (Top Left)
            renderMagiaBar(drawContext, client);

            // 2. Render Combat Bar (Bottom Center - replaces Hotbar if active)
            if (pc.isInCombatMode()) {
                renderCombatBar(drawContext, client, pc);
            }
        });
    }

    private void renderMagiaBar(DrawContext context, MinecraftClient client) {
        IPlayerMagia playerMagia = (IPlayerMagia) client.player;
        int current = playerMagia.getMagia();
        int max = playerMagia.getMaxMagia();
        int x = 20, y = 20, width = 100, height = 10;

        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF000000); // Border
        context.fill(x, y, x + width, y + height, 0xFF1A1A1A); // BG

        float ratio = (float) current / Math.max(1, max);
        int filledWidth = (int) (width * ratio);

        context.fill(x, y, x + width, y + height, 0xFF002244); // Empty
        context.fill(x, y, x + filledWidth, y + height, 0xFF00A2FF); // Filled

        // Text
        String text = "Magia: " + current + "/" + max;
        float scale = 0.7f;
        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, 1.0f);
        float textX = (x + (width - (client.textRenderer.getWidth(text) * scale)) / 2.0f) / scale;
        float textY = (y + (height - (client.textRenderer.fontHeight * scale)) / 2.0f) / scale + 0.5f;
        context.drawTextWithShadow(client.textRenderer, text, (int)textX, (int)textY, 0xFFFFFF);
        context.getMatrices().pop();
    }

    private void renderCombatBar(DrawContext context, MinecraftClient client, IPlayerCombat pc) {
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // Use the vanilla "offhand slot" texture size (22x22) which looks like a standard slot
        int slotSize = 22;
        int totalWidth = 5 * slotSize;
        int startX = (width - totalWidth) / 2;
        int startY = height - 22; // Just above bottom edge

        // Draw "Magia" indicator above the bar
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal("◆ MAGIA ◆").formatted(Formatting.GOLD), width / 2, startY - 12, 0xFFFFFF);

        RenderSystem.enableBlend();

        for (int i = 0; i < 5; i++) {
            int slotX = startX + (i * slotSize);
            Skill skill = pc.getEquippedSkill(i);

            // 1. Draw Slot Background (Vanilla Texture)
            // U: 24, V: 22 is the standard square slot texture in widgets.png
            context.drawTexture(WIDGETS_TEXTURE, slotX, startY, 24, 22, 22, 22);

            // 2. Skill Icon (Colored Box)
            if (skill != Skill.NONE) {
                int padding = 3;
                int size = 16;

                // Ensure color is opaque (or add 0xFF000000 alpha)
                int color = skill.getColor();
                if ((color & 0xFF000000) == 0) {
                    color |= 0xFF000000;
                }

                context.fill(slotX + padding, startY + padding, slotX + padding + size, startY + padding + size, color);
            }

            // 3. Key Number (Small text in corner)
            String keyNum = String.valueOf(i + 1);
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 200); // Bring to front

            float scale = 0.6f;
            context.getMatrices().scale(scale, scale, 1.0f);

            // Adjust coordinates for scaling
            int textX = (int) ((slotX + 2) / scale);
            int textY = (int) ((startY + 2) / scale);

            context.drawTextWithShadow(client.textRenderer, keyNum, textX, textY, 0xFFFFFF);
            context.getMatrices().pop();
        }

        RenderSystem.disableBlend();
    }
}