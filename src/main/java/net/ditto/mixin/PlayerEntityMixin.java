package net.ditto.mixin;

import net.ditto.race.Race;
import net.ditto.skill.Skill;
import net.ditto.util.IPlayerRace;
import net.ditto.util.IPlayerMagia;
import net.ditto.util.IPlayerCombat;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements IPlayerRace, IPlayerMagia, IPlayerCombat {

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    // --- Race Data ---
    @Unique private Race magia$race = null;
    @Override public Race magia$getRace() { return this.magia$race; }
    @Override public void magia$setRace(Race race) { this.magia$race = race; }

    // --- Magia Data ---
    @Unique private int magia = 10;
    @Unique private int maxMagia = 10;
    @Override public int getMagia() { return this.magia; }
    @Override public void setMagia(int amount) { this.magia = amount; }
    @Override public int getMaxMagia() { return this.maxMagia; }

    // --- Combat Data ---
    @Unique private boolean isCombatMode = false;
    @Unique private Skill[] equippedSkills = new Skill[]{Skill.NONE, Skill.NONE, Skill.NONE, Skill.NONE, Skill.NONE};

    @Override
    public boolean isInCombatMode() {
        return this.isCombatMode;
    }

    @Override
    public void setCombatMode(boolean active) {
        this.isCombatMode = active;
    }

    @Override
    public Skill getEquippedSkill(int slot) {
        if (slot < 0 || slot >= equippedSkills.length) return Skill.NONE;
        return equippedSkills[slot];
    }

    @Override
    public void setEquippedSkill(int slot, Skill skill) {
        if (slot >= 0 && slot < equippedSkills.length) {
            this.equippedSkills[slot] = skill;
        }
    }

    // --- Persistence ---
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void writeCustomData(NbtCompound nbt, CallbackInfo ci) {
        // Race
        if (magia$race != null) nbt.putString("magia_race", magia$race.name());

        // Magia
        nbt.putInt("magia_amount", this.magia);
        nbt.putInt("magia_max", this.maxMagia);

        // Combat
        nbt.putBoolean("magia_combat_mode", this.isCombatMode);
        for(int i = 0; i < equippedSkills.length; i++) {
            nbt.putString("magia_skill_" + i, equippedSkills[i].name());
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void readCustomData(NbtCompound nbt, CallbackInfo ci) {
        // Race
        if (nbt.contains("magia_race")) {
            try { this.magia$race = Race.valueOf(nbt.getString("magia_race")); } catch (Exception ignored) {}
        }

        // Magia
        if (nbt.contains("magia_amount")) this.magia = nbt.getInt("magia_amount");
        if (nbt.contains("magia_max")) this.maxMagia = nbt.getInt("magia_max");

        // Combat
        if (nbt.contains("magia_combat_mode")) this.isCombatMode = nbt.getBoolean("magia_combat_mode");
        for(int i = 0; i < equippedSkills.length; i++) {
            if(nbt.contains("magia_skill_" + i)) {
                try {
                    this.equippedSkills[i] = Skill.valueOf(nbt.getString("magia_skill_" + i));
                } catch (Exception e) {
                    this.equippedSkills[i] = Skill.NONE;
                }
            }
        }
    }
}