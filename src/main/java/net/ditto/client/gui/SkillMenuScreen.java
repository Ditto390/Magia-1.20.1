package net.ditto.client.gui;

import com.google.common.collect.ImmutableList;
import net.ditto.networking.MagiaPackets;
import net.ditto.skill.Skill;
import net.ditto.util.IPlayerCombat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class SkillMenuScreen extends Screen {

    private SkillListWidget skillList;
    private Skill selectedSkill = null;
    private ButtonWidget[] slotButtons = new ButtonWidget[5];

    public SkillMenuScreen() {
        super(Text.literal("Skill Menu"));
    }

    @Override
    protected void init() {
        // 1. Calculate Layout
        int listTop = 32;
        int listBottom = this.height - 80; // Leave space at bottom for binding area
        int itemHeight = 24;

        // 2. Initialize Scrollable List
        this.skillList = new SkillListWidget(this.client, this.width, this.height, listTop, listBottom, itemHeight);
        this.addDrawableChild(this.skillList);

        // 3. Initialize Bottom Binding Buttons
        int buttonY = this.height - 50;
        int buttonWidth = 60;
        int gap = 5;
        // Center the group of buttons
        int startX = (this.width - ((buttonWidth * 5) + (gap * 4))) / 2;

        for (int i = 0; i < 5; i++) {
            final int slotIndex = i;
            slotButtons[i] = ButtonWidget.builder(Text.literal("Slot " + (i + 1)), (button) -> {
                        if (selectedSkill != null) {
                            equipSkill(slotIndex, selectedSkill);
                            updateButtonLabels();
                        }
                    })
                    .dimensions(startX + (i * (buttonWidth + gap)), buttonY, buttonWidth, 20)
                    .build();

            this.addDrawableChild(slotButtons[i]);
        }

        updateButtonLabels();
    }

    private void updateButtonLabels() {
        IPlayerCombat player = (IPlayerCombat) MinecraftClient.getInstance().player;
        if (player == null) return;

        for (int i = 0; i < 5; i++) {
            Skill s = player.getEquippedSkill(i);
            String name = (s == Skill.NONE) ? "Empty" : s.getName();
            slotButtons[i].setMessage(Text.literal("[" + (i + 1) + "] " + name));
        }
    }

    private void equipSkill(int slot, Skill skill) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(slot);
        buf.writeString(skill.name());
        ClientPlayNetworking.send(MagiaPackets.EQUIP_SKILL, buf);

        IPlayerCombat player = (IPlayerCombat) MinecraftClient.getInstance().player;
        if (player != null) {
            player.setEquippedSkill(slot, skill);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        this.skillList.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);

        if (selectedSkill != null) {
            int infoY = this.height - 75;
            Text nameText = Text.literal("Selected: " + selectedSkill.getName()).formatted(Formatting.GOLD, Formatting.BOLD);
            Text descText = Text.literal(selectedSkill.getDescription()).formatted(Formatting.GRAY);

            context.drawCenteredTextWithShadow(this.textRenderer, nameText, this.width / 2, infoY, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, descText, this.width / 2, infoY + 12, 0xFFFFFF);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Select a skill above to bind it").formatted(Formatting.DARK_GRAY), this.width / 2, this.height - 70, 0xFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    public void setSelected(Skill skill) {
        this.selectedSkill = skill;
    }

    // =================================================================================================
    // INNER CLASS: The Scrollable List
    // =================================================================================================

    class SkillListWidget extends ElementListWidget<SkillListWidget.SkillEntry> {

        public SkillListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
            super(client, width, height, top, bottom, itemHeight);

            // Populate the list
            IPlayerCombat pc = (IPlayerCombat) client.player;
            if (pc == null) return;

            for (Skill skill : Skill.values()) {
                // --- MODIFIED: Only show unlocked skills ---
                if (skill != Skill.NONE && pc.isSkillUnlocked(skill)) {
                    this.addEntry(new SkillEntry(skill));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return 220;
        }

        @Override
        protected int getScrollbarPositionX() {
            return this.width / 2 + getRowWidth() / 2 + 10;
        }

        // =============================================================================================
        // INNER CLASS: The List Entry
        // =============================================================================================

        class SkillEntry extends ElementListWidget.Entry<SkillEntry> {
            private final Skill skill;

            public SkillEntry(Skill skill) {
                this.skill = skill;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                if (selectedSkill == this.skill) {
                    context.fill(x, y, x + entryWidth, y + entryHeight, 0x44FFFFFF);
                    context.drawBorder(x, y, entryWidth, entryHeight, 0xFFFFFFFF);
                } else if (hovered) {
                    context.fill(x, y, x + entryWidth, y + entryHeight, 0x22FFFFFF);
                }

                int iconSize = 16;
                int iconX = x + 5;
                int iconY = y + (entryHeight - iconSize) / 2;
                context.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, skill.getColor());
                context.drawBorder(iconX, iconY, iconSize, iconSize, 0xFF000000);

                context.drawTextWithShadow(client.textRenderer, skill.getName(), iconX + iconSize + 10, y + 4, 0xFFFFFF);

                String cd = skill.getCooldownSeconds() + "s";
                int cdWidth = client.textRenderer.getWidth(cd);
                context.drawTextWithShadow(client.textRenderer, cd, x + entryWidth - cdWidth - 5, y + 8, 0xAAAAAA);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    SkillMenuScreen.this.setSelected(this.skill);
                    return true;
                }
                return false;
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return ImmutableList.of();
            }

            @Override
            public List<? extends Element> children() {
                return ImmutableList.of();
            }
        }
    }
}